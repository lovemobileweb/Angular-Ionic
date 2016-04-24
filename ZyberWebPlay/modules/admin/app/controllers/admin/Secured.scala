package controllers.admin

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.Crypto
import play.api.libs.concurrent.Execution.Implicits._
import zyber.server.dao.admin.TenantAdmin
import adminservices.admin.LoginService
import com.google.inject.ImplementedBy
import adminservices.admin.LoginServiceImp



trait Secured { this: Controller =>

  val REDIRECT_FROM = "Redirect_from"

  val rememberTime = 4 * 60 * 60

  val defaultTenant = "default"

  def loginService: LoginService

  def username(request: RequestHeader): Option[TenantAdmin] = usernameSession(request) orElse usernameCookie(request)

  def usernameSession(request: RequestHeader): Option[TenantAdmin] =
    request.session.get(Security.username).
      flatMap(loginService.getUser(_).filter(_.getActive))

  def usernameCookie(request: RequestHeader): Option[TenantAdmin] =
    request.cookies.get(Security.username).
      map(_.value).
      flatMap(v => Crypto.extractSignedToken(v)).
      flatMap(loginService.getUser(_).filter(_.getActive))

  def updateSecCookie(result: Result, user: String): Result =
    result.withCookies(Cookie(Security.username, Crypto.signToken(user), Some(rememberTime)))

  def onUnauthorized(request: RequestHeader): Result = {
    Redirect(routes.LoginController.login()).withSession(REDIRECT_FROM -> request.path)
  }

  object Authenticated extends ActionBuilder[UserRequest] {

    def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]) =
      authenticate(request, block)

    def authenticate[A](request: Request[A], block: (UserRequest[A]) => Future[Result]) = {
      usernameSession(request) map { user =>
        block(new UserRequest(user, request))
      } orElse (usernameCookie(request) map { user =>
        block(new UserRequest(user, request)).
          map(updateSecCookie(_, user.getUsername))
      }) getOrElse {
        Future.successful(onUnauthorized(request))
      }
    }
  }
}