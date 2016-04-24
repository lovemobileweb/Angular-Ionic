package controllers.zyberapp

import java.util.UUID
import javax.inject.Inject
import util._
import controllers.admin.Utils
import core.{ ApiErrors, ZyberResponse }
import models.JsonFormats._
import models.{ JGroup, JGroupMember }
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.{ I18nSupport, Lang, Messages, MessagesApi }
import play.api.libs.functional.syntax.{ functionalCanBuildApplicative, toFunctionalBuilderOps }
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.{ JsPath, Reads }
import play.api.mvc._
import services._
import zyber.server.Abilities
import zyber.server.dao.User
import zyber.server.dao.admin.Tenant
import scala.util.{ Try, Failure, Success }
import models.JPrincipal
import models.JUser

class LoginController @Inject() (
    val loginService: LoginService,
    val activityService: ActivityService,
    implicit val metadataService: MetadataService,
    val messagesApi: MessagesApi,
    val multitenancyHelper: MultitenancyHelper,
    val securityService: SecurityService) extends Secured with Controller with I18nSupport {

  import Util._

  val loginForm = Form {
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "rememberme" -> boolean //     , "twoFactor" -> boolean
      )
  }

  val loginJsonReads: Reads[(String, String)] = (
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read[String])((_, _))

  val newAccountForm = Form(
    tuple(
      "countryCode" -> text,
      "phoneNumber" -> text,
      "email" -> email.verifying(nonEmpty),
      "password" -> nonEmptyText(minLength = 6).
        verifying(Messages("account.create.password"), pass => pass.matches("^\\S+$")),
      "name" -> nonEmptyText(minLength = 6)))

  def login = TenantAction { implicit rs =>
    implicit val fakeUser = new User()
    fakeUser.setTenantId(rs.tenant.getTenantId)
    Ok(views.html.login(loginForm, twoFactorEnabled))
  }

  def getFakeUser(tenant: Tenant): User = {
    implicit val fakeUser = new User()
    fakeUser.setTenantId(tenant.getTenantId)
    fakeUser
  }

  def authenticate = TenantAction { implicit rs =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, twoFactorEnabled(getFakeUser(rs.tenant)))),
      {
        case (user, pass, remember) =>
          if (loginService.checkCredentials(user, pass)) {
            implicit val user1: User = loginService.getUser(user).get
            activityService.saveLoginActivity(user1, false, requestNote)
            val langDb = Option(user1.getLanguage)
            val toPath = rs.session.get(REDIRECT_FROM).getOrElse(routes.HomeController.home("").path)

            //TODO potential issue where the password has expired and two auth is active
            if (user1.expiredPassword()) {
              Redirect(routes.LoginController.changePassword()).withSession(Security.username -> user, REDIRECT_FROM -> toPath)
            } else {
              if (!twoFactorMandatory) {
                val result =
                  if (remember)
                    updateSecCookie(Redirect(toPath), user)
                  else
                    Redirect(toPath).withSession(Security.username -> user, Utils.fullAuth -> "true")
                result.withLang(Lang(langDb.getOrElse("en")))
              } else {
                //                Redirect("/fakePhoneConfirm").withSession(Security.username -> user)
                Redirect(routes.LoginController.phoneConfirmation).withSession(Security.username -> user)
              }
            }
          } else
            Unauthorized(views.html.login(loginForm.fill((user, pass, remember)).withGlobalError(Messages("login.error")), twoFactorEnabled(getFakeUser(rs.tenant))))
      })
  }

  case class PasswordForm(password: String)

  def changePassword = TenantAction { implicit rs =>
    rs.session.get(Security.username).map(s => {
      Ok(views.html.changePassword(None, None))
    }).getOrElse(Unauthorized)
  }

  def doChangePassword() = TenantAction { implicit rs =>
    rs.session.get(Security.username).map(s => {
      implicit val user = loginService.getUser(s).get
      val form: Form[PasswordForm] = Form(
        mapping(
          "password" -> nonEmptyText)(PasswordForm.apply)(PasswordForm.unapply))
      val request: Form[PasswordForm] = form.bindFromRequest
      val password = request.get.password

      loginService.changePassword(user, password) match {
        case Success(_) => {
          loginService.expire(user, false)
          val toPath = rs.session.get(REDIRECT_FROM).getOrElse(routes.HomeController.home("").absoluteURL())
          Redirect(toPath).withSession(rs.session + (Utils.fullAuth -> "true"))
        }
        case Failure(e) =>
          e match {
            //TODO: support other languages
            case t: PasswordReuseException => BadRequest(views.html.changePassword(Some("You cannot reuse an existing password"), Some(password)))
            case _ => {
              Logger.error("Error changing password", e)
              InternalServerError(e.getMessage)
            }
          }

      }
    }).getOrElse(Unauthorized)
  }

  def authenticateApi = TenantAction(parse.json) { implicit rs =>
    rs.body.validate[(String, String)](loginJsonReads).map {
      case (user, pass) =>
        if (loginService.checkCredentials(user, pass)) {
          implicit val user1: User = loginService.getUser(user).get
          activityService.saveLoginActivity(user1, false, requestNote)
          val langDb = Option(user1.getLanguage)
          val result = tokenResult(user)
          result.withLang(Lang(langDb.getOrElse("en"))) //TODO save language in client ?
        } else {
          Unauthorized
        }
    } getOrElse {
      BadRequest
    }
  }

  def logout = TenantAction {
    Redirect(routes.LoginController.login()).withNewSession
      .discardingCookies(
        DiscardingCookie(Security.username),
        DiscardingCookie(messagesApi.langCookieName))
      .flashing(
        "success" -> Messages("login.logout"))
  }

  private def twoFactorEnabled(implicit user: User): Boolean = Utils.getPasswordPolicy.twoFactorEnabled.getOrElse(false)
  private def twoFactorMandatory(implicit user: User): Boolean = Utils.getPasswordPolicy.twoFactorMandatory.getOrElse(false)

  def phoneConfirmation = TenantAction { implicit rs =>
    rs.session.get(Security.username).map(s => {
      implicit val user = loginService.getUser(s).get
      implicit val ur: UserRequest[_] = UserRequest(user, rs.tenant, rs.request)
      Ok(views.html.phoneConfirmation(user, user.getNonce, true, false))
    }).getOrElse(Unauthorized)
  }

  def getOneFactoredUser(implicit rs: TenantRequest[_]) = {
    rs.session.get(Security.username).flatMap(loginService.getUser).
      getOrElse(throw new RuntimeException("No user found in session"))
  }

  /*def fakePhoneConfirmation() = TenantAction { implicit rs =>
    //Just hack in a null for now
    implicit val ur:UserRequest[_] = UserRequest(null,rs.tenant,rs.request)
    implicit val user = loginService.getUser("a@b.com").get
    //    Ok(views.html.phoneConfirmation(user,user.getNonce))
    Ok(views.html.phoneConfirmation(user, user.getNonce, true,false)).withSession(Security.username->"a@b.com")
  }*/

  def updateNumber(countryCode: String, phoneNumber: String, nonce: UUID) = TenantAction { implicit rs =>
    implicit val user = getOneFactoredUser
    if (!user.isNumberConfirmed && nonce.equals(user.getNonce)) {
      user.setCountryCode(countryCode)
      user.setPhoneNumber(phoneNumber)
      val user1: Either[ApiErrors, Unit] = loginService.updateUser(user.getUserId)(user.getName, user.getEmail, user.getLanguage, None, null,
        countryCode, phoneNumber)
      if (user1.isLeft && user1.left.get.mainError.message != null) {
        InternalServerError(user1.left.get.mainError.message)
      } else {
        Ok("")
      }
    } else {
      Ok("")
    }
  }

  def resendConfirmation() = TenantAction { implicit rs =>
    implicit val user = getOneFactoredUser
    val sms: Try[Unit] = loginService.sendSMS(user)
    //    val sms: Try[Unit] = Try()
    if (sms.isSuccess) {
      Ok("")
    } else {
      Logger.warn(s"failed to send SMS to ${user.getPhoneNumber}/${user.getCountryCode}", sms.failed.get)
      InternalServerError(sms.failed.get.getMessage)
    }
  }

  def doPhoneConfirmation(token: String) = TenantAction { implicit rs =>
    implicit val user = getOneFactoredUser
    val sms: Try[Boolean] = loginService.checkSMS(user, token)
    //    val sms: Try[Boolean] = Try(true)
    if (sms.isSuccess) {
      if (sms.get) {
        Ok("").withSession(Security.username -> user.getEmail, Utils.fullAuth -> "true")
      } else {
        Unauthorized
      }
    } else {
      Logger.warn(s"failed to check SMS for ${user.getPhoneNumber}/${user.getCountryCode}", sms.failed.get)
      InternalServerError(sms.failed.get.getMessage)
    }
  }

  def newAccount = TenantAction { implicit rs =>
    implicit val fakeUser = getFakeUser(rs.tenant)
    Ok(views.html.createAccount(twoFactorEnabled, newAccountForm))
  }

  def createAccount = TenantAction { implicit rs =>
    implicit val fakeUser = getFakeUser(rs.tenant)
    newAccountForm.bindFromRequest.fold(
      fwe => BadRequest(views.html.createAccount(twoFactorEnabled, fwe)),
      {
        case (countryCode, phoneNumber, email1, password, name) =>
          loginService.getUser(email1) match {
            case Some(user) =>
              val formWithError = newAccountForm.fill(countryCode, phoneNumber, email1, password, name).withError("username", Messages("account.create.user.exists"))
              BadRequest(views.html.createAccount(twoFactorEnabled, formWithError))
            case None =>
              val prefLang = messagesApi.preferred(rs).lang.code
              val roleToSet = if (rs.tenant.getSubdomain.equals("localhost")) Abilities.DefaultUserRoles.powerUser else Abilities.DefaultUserRoles.user
              loginService.createUser(email1, password, name, prefLang, roleToSet.getRoleId) match {
                case Success(nUser) => {
                  activityService.saveLoginActivity(nUser, true, requestNote)(nUser)
                  Redirect(routes.Application.index).
                    withSession(Security.username -> nUser.getEmail, Utils.fullAuth -> "true").
                    flashing("success" -> Messages("account.create.success"))
                }
                case Failure(e) =>
                  e match {
                    case p: BadPasswordException => {
                      val formWithError = newAccountForm.fill((countryCode, phoneNumber, email1, password, name)).withError("password", e.getMessage)
                      BadRequest(views.html.createAccount(twoFactorEnabled, formWithError))
                    }
                    case _ => {
                      Logger.error("Error creating account", e)
                      Redirect(routes.LoginController.login())
                        .flashing("error" -> e.getMessage)
                    }
                  }

              }
          }
      })
  }

  //  def getUserGroups = Authenticated { implicit rs =>
  //    Ok(Json.toJson(loginService.getGroups(rs.user).map(JGroup.from)))
  //  }

  def requestNote(implicit rs: RequestHeader): String = {
    s" IP - ${rs.remoteAddress} : UA - ${rs.headers.get("User-Agent").getOrElse("")}"
  }

  //Returns ZyberResponse

  def getGroupMembers(groupId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right {
        loginService.getGroupMembers(
          UUID.fromString(groupId)).map(JGroupMember.from)
      }
    }
  }

  def getGroups = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right { loginService.getGroups.map(JGroup.from) }
    }
  }

  def getGroup(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        g <- loginService.getGroup(UUID.fromString(uuid)).toRight(ApiErrors.single("Invalid group", Messages("api.invalid.group"), BAD_REQUEST))
      } yield {
        JGroup.from(g)
      }
    }
  }

  def createGroup = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateGroups
        name <- ZyberResponse.jsonToResponse((rs.body \ "name").validate[String])
        _ <- loginService.createGroup(name)
      } yield {
        Messages("api.group_created")
      }
    }
  }

  def updateGroup(groupId: String) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      Logger.debug("Body: " + rs.body)
      for {
        _ <- loginService.canCreateGroups
        newName <- ZyberResponse.jsonToResponse((rs.body \ "name").validate[String])
        _ <- loginService.updateGroup(UUID.fromString(groupId), newName)
      } yield {
        Messages("api.group_updated")
      }
    }
  }
  def addMembers(groupId: String) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateGroups
        members <- ZyberResponse.jsonToResponse((rs.body \ "members").validate[Seq[String]])
        membersAdded <- loginService.addMembers(UUID.fromString(groupId), members)
      } yield {
        Messages("members.added", membersAdded)
      }
    }
  }

  def removeMember(groupId: String, memberId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateGroups
        _ <- loginService.removeMember(UUID.fromString(groupId), UUID.fromString(memberId))
      } yield {
        Messages("member.removed")
      }
    }
  }

  def removeGroup(groupId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateGroups
        _ <- loginService.deleteGroup(UUID.fromString(groupId))
      } yield {
        Messages("group.removed")
      }
    }
  }

  def findPrincipalByName(name: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right {
        loginService.getPrincipalByNameLike(name).map { p =>
          JPrincipal(
            p.getPrincipalId.toString(),
            p.getType.name(),
            p.getCreatedDate,
            p.getDisplayName)
        }
      }
    }
  }

  def findUsersByName(name: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right {
        loginService.getUserByNameLike(name).map(JUser.fromUser)
      }
    }
  }

  def preflight() = Action {
    Ok("").withHeaders("Access-Control-Allow-Origin" -> "*",
      "Allow" -> "*",
      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
  }
}