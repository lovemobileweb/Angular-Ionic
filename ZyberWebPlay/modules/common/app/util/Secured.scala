package util

import java.util.UUID

import controllers.admin.Utils
import core.{ApiErrors, ZyberResponse}
import play.api.Logger
import play.api.http.Status
import play.api.libs.Crypto
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import services.LoginService
import zyber.server.Abilities
import zyber.server.dao.{User, UserRole}
import zyber.server.dao.admin.Tenant

import scala.concurrent.Future

case class UserRequest[A](
  val user: User,
  val tenant: Tenant,
  request: Request[A],
  abilities: Option[Abilities] = None) extends WrappedRequest[A](request)

case class TenantRequest[A](
  val tenant: Tenant,
  request: Request[A]) extends WrappedRequest[A](request)

//TODO clean up a bit
trait Secured {

  val REDIRECT_FROM = "Redirect_from"

  val rememberTime = 4 * 60 * 60
  //  val rememberTime = 60

  val defaultTenant = "default"

  val tokenKey = "bearer"

  def loginService: LoginService

  def multitenancyHelper: MultitenancyHelper

  def username(request: RequestHeader): Option[User] = {
    multitenancyHelper.getTenant(request)
      .flatMap(multitenancyHelper.validTenant)
      .flatMap { tenant =>
        implicit val tenantId = tenant.getTenantId
        usernameToken(request) orElse usernameSession(request) orElse usernameCookie(request)
      }
  }

  private def checkFullAuth(requestHeader: RequestHeader, user:Option[User]) = {
    user.flatMap(u => if(requestHeader.session.get(Utils.fullAuth).exists(_.toBoolean)) user else None)
  }

  protected def usernameSession(request: RequestHeader)(implicit tenantId: UUID): Option[User] =
    checkFullAuth(request, request.session.get(Security.username).
      flatMap(loginService.getUser(_).filter(_.getActive)))

  protected def usernameCookie(request: RequestHeader)(implicit tenantId: UUID): Option[User] =
    request.cookies.get(Security.username).
      map(_.value).
      flatMap(v => Crypto.extractSignedToken(v)).
      flatMap(loginService.getUser(_).filter(_.getActive))

  protected def usernameToken(request: RequestHeader)(implicit tenantId: UUID): Option[User] = {
    request.headers.get(tokenKey).
      flatMap(v => Crypto.extractSignedToken(v)).
      flatMap(loginService.getUser(_).filter(_.getActive))
  }

  protected def usernameParam(request: RequestHeader)(implicit tenantId: UUID): Option[User] = {
    val query = request.queryString.map { case (k, v) => k -> v.mkString }
    query.get(tokenKey).
      flatMap(v => Crypto.extractSignedToken(v)).
      flatMap(loginService.getUser(_).filter(_.getActive))
  }

  def tokenResult(result: Result, user: String): Result =
    result.withHeaders(tokenKey -> Crypto.signToken(user))

  def tokenResult(user: String): Result =
    Ok { JsObject(Seq(tokenKey -> JsString(Crypto.signToken(user)))) }

  def updateSecCookie(result: Result, user: String): Result =
    result.withCookies(Cookie(Security.username, Crypto.signToken(user), Some(rememberTime)))

  def onUnauthorized(request: RequestHeader): Result = {
    Results.Redirect("/login").withSession(REDIRECT_FROM -> request.path)
  }

  def onUnauthorizedApi(request: RequestHeader) = ZyberResponse.errorResult(
    ApiErrors.single("Unauthorized request", "Login required",
      Status.UNAUTHORIZED, Some("authentication")))

  object TenantAction extends ActionBuilder[TenantRequest] {
    def invokeBlock[A](request: Request[A],
                       block: (TenantRequest[A]) => Future[Result]): Future[Result] = {
      multitenancyHelper.getTenant(request)
        .flatMap(multitenancyHelper.validTenant)
        .map { implicit t =>
          block(TenantRequest(t, request))
        } getOrElse {
          Future.successful(multitenancyHelper.onInvalidTenant(request))
        }
    }
  }

  //Does not update security cookie
  object AuthenticatedTenantAction extends ActionRefiner[TenantRequest, UserRequest] {
    def refine[A](input: TenantRequest[A]) = Future.successful {
      usernameSession(input)(input.tenant.getTenantId)
        .orElse(usernameCookie(input)(input.tenant.getTenantId))
        .map { user =>
          new UserRequest(user, input.tenant, input)
        } toRight onUnauthorized(input)
    }
  }

  object BasicHttpAction extends ActionRefiner[TenantRequest, UserRequest] {
    def refine[A](input: TenantRequest[A]) = Future.successful {

      input.headers.get("Authorization").flatMap { authorization =>
        authorization.split(" ").drop(1).headOption.flatMap { encoded =>
          new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
            case u :: p :: Nil if loginService.checkCredentials(u, p)(input.tenant.getTenantId) =>
              loginService.getUser(u)(input.tenant.getTenantId)
            case _ => None
          }
        }.map(user => new UserRequest(user, input.tenant, input))
      } toRight Results.Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
    }
  }

  val Authenticated = buildAuthenticated(multitenancyHelper.onInvalidTenant,
    onUnauthorized)

  val AuthenticatedApi = buildAuthenticated(multitenancyHelper.onInvalidTenantApi, onUnauthorizedApi)

  //Check for authentication token in params too
  val AuthenticatedInParm = new ActionBuilder[UserRequest] {
    def invokeBlock[A](request: Request[A],
                       block: (UserRequest[A]) => Future[Result]): Future[Result] = {

      val maybeResult = for {
        tenant <- multitenancyHelper.getTenant(request)
        validTenant <- multitenancyHelper.validTenant(tenant)
        tenantReq = TenantRequest(validTenant, request)
        result <- runAuthenticatedInParam(tenantReq)(block)
      } yield {
        result
      }
      maybeResult.getOrElse(Future.successful(multitenancyHelper.onInvalidTenant(request)))
    }
  }

  type UserResult[A] = UserRequest[A] => Future[Result]

  private def runAuthenticatedAndUpdateCookie[A](tr: TenantRequest[A], onUnauthorized: RequestHeader => Result)(block: UserResult[A]): Option[Future[Result]] = {
    usernameSession(tr)(tr.tenant.getTenantId)
      .map(user => block(new UserRequest(user, tr.tenant, tr)))
      .orElse {
        usernameToken(tr)(tr.tenant.getTenantId)
          .map(user => block(new UserRequest(user, tr.tenant, tr)))
      }.orElse {
        usernameCookie(tr)(tr.tenant.getTenantId) map { user =>
          block(new UserRequest(user, tr.tenant, tr))
            .map(updateSecCookie(_, user.getEmail))
        }

      } orElse {
        Some(Future.successful(onUnauthorized(tr)))
      }
  }

  //Check for authentication token in params too
  private def runAuthenticatedInParam[A](tr: TenantRequest[A])(block: UserResult[A]): Option[Future[Result]] = {
    usernameSession(tr)(tr.tenant.getTenantId)
      .map(user => block(new UserRequest(user, tr.tenant, tr)))
      .orElse {
        usernameToken(tr)(tr.tenant.getTenantId)
          .map(user => block(new UserRequest(user, tr.tenant, tr)))
      } orElse {
        usernameCookie(tr)(tr.tenant.getTenantId) map { user =>
          block(new UserRequest(user, tr.tenant, tr))
            .map(updateSecCookie(_, user.getEmail))
        }
      } orElse {
        usernameParam(tr)(tr.tenant.getTenantId)
          .map(user => block(new UserRequest(user, tr.tenant, tr)))
      } orElse {
        Some(Future.successful(onUnauthorized(tr)))
      }
  }

  private def buildAuthenticated[A](onIvalidTenant: RequestHeader => Result,
                                    onUnauthorized: RequestHeader => Result) = new ActionBuilder[UserRequest] {
    def invokeBlock[C](request: Request[C],
                       block: (UserRequest[C]) => Future[Result]): Future[Result] = {

      val maybeResult = for {
        tenant <- multitenancyHelper.getTenant(request)
        validTenant <- multitenancyHelper.validTenant(tenant)
        tenantReq = TenantRequest(validTenant, request)
        result <- runAuthenticatedAndUpdateCookie(tenantReq, onUnauthorized)(block)
      } yield {
        result
      }
      maybeResult.getOrElse(Future.successful(onIvalidTenant(request)))
    }
  }

  //  lazy val Authenticated = TenantAction andThen AuthenticatedTenantAction

  lazy val TenantWebdavAction = TenantAction andThen BasicHttpAction

  def userWebdav(request: RequestHeader): Either[Result, User] = {
    val res = for {
      tenant <- multitenancyHelper.getTenant(request).flatMap(multitenancyHelper.validTenant).toRight(multitenancyHelper.onInvalidTenant(request)).right
      user <- doHttpAuthentication(request, tenant).right
    } yield {
      user
    }
    res
  }

  def doHttpAuthentication(request: RequestHeader, t: Tenant): Either[Result, User] = {
    if (request.path.endsWith("status.php")) {
      authenticateByUserAgent(request, t)
    } else {
      for (key <- request.headers.keys.toList) {
        key match {
          // case "Cookie"        => return authenticateByCoockie(request, t)
          case "Authorization" => return authenticateByAuthorizationHeader(request, t)
          case _               =>
        }
      }
      Logger.debug("Cookie and Authorization skipped ")
      request.headers.get("User-Agent").map(x => authenticateByUserAgent(request, t)).get
    }
  }

  def authenticateByCookie(request: RequestHeader, t: Tenant): Either[Result, User] = {
    Logger.debug("Cookie found ")
    usernameCookie(request)(t.getTenantId).toRight(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV""""))
  }

  def authenticateByUserAgent(request: RequestHeader, t: Tenant): Either[Result, User] = {

    request.headers.get("User-Agent") match {

      case None =>
        Logger.debug("User Agent not found ")
        Left(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV""""))

      case Some(userAgent) => if (userAgent.toLowerCase().contains("owncloud")) {
        Logger.debug("owncloud User Agent found ")

        val os: String = if (userAgent.toLowerCase().contains("ios")) "ios" else if (userAgent.toLowerCase().contains("android")) "android" else "not found"
        Logger.debug("os = " + os)
        loginService.getUser(os)(t.getTenantId).toRight(
          {
            Logger.debug("Operating system not detected: " + os)
            Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""")
          })
      } else {
        Logger.debug("Not ownCloud User Agent ")
        loginService.getUser("")(t.getTenantId).toRight(
          {

            Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""")
          })

      }
    }

  }

  def authenticateByAuthorizationHeader(request: RequestHeader, t: Tenant): Either[Result, User] = {
    request.headers.get("Authorization") match {
      case None =>
        Logger.debug("Authorization header not found")
        Left(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV""""))

      case Some(meanauthorization: String) => request.headers.get("Authorization").flatMap { authorization =>
        meanauthorization.split(" ").drop(1).headOption.flatMap { encoded =>
          val decodedCredentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes))
          Logger.debug("decodedCredentials " + decodedCredentials)
          val upList = decodedCredentials.split(":").toList
          upList match {
            case u :: p :: Nil if loginService.checkCredentials(u, p)(t.getTenantId) =>
              loginService.getUser(u)(t.getTenantId)
            case _ => upList.size match {
              case 2 => if (loginService.checkCredentials(upList(0), upList(1))(t.getTenantId)) {
                Logger.debug("User: " + upList(0) + " Password: " + upList(1))
                loginService.getUser(upList(0))(t.getTenantId)
              } else {
                None
              }
              case _ =>
                Logger.debug("Invalid credentials")
                None
            }

          }
        }
      }.toRight {
        Results.Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="ZyberWebDAV"""")
      }
    }

  }

  def hasRole(ur: UserRole) = new ActionFilter[UserRequest] {
    override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
      if (request.user.getUserRole.equals(ur.getRoleId))
        Future.successful(None)
      else
        Future.successful(Some(Results.Forbidden("Forbidden"))) //TODO create nice page for unauthorized access
    }
  }

  object WithAbilities extends ActionTransformer[UserRequest, UserRequest] {
    override def transform[A](request: UserRequest[A]) = Future.successful {
      val ab = loginService.getUserRole(request.user)
        .map { ur => new Abilities(ur.getAbilities) }

      UserRequest(request.user, request.tenant, request.request, ab)
    }
  }

  def AuthenticatedToModule(ur: UserRole) = Authenticated andThen hasRole(ur) andThen WithAbilities

  implicit protected def fromTenantReq[A](
    implicit tr: TenantRequest[A]): UUID = tr.tenant.getTenantId

  implicit protected def fromUserReq[A](implicit tr: UserRequest[A]): User = tr.user
}