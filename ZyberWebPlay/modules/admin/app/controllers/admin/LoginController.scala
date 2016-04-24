package controllers.admin

import play.api.mvc._
import play.api.i18n.I18nSupport
import adminservices.admin.LoginService
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.Messages
import play.api.i18n.Lang
import zyber.server.dao.admin.TenantAdmin
import java.util.UUID
import java.util.Date
import scala.util.Failure
import scala.util.Success

class LoginController @Inject() (
    val loginService: LoginService,
    val messagesApi: MessagesApi) extends Secured with Controller with I18nSupport with Utils {

  val loginForm = Form {
    tuple(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "rememberme" -> boolean)
  }

  val createUserForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "username" -> email.verifying(nonEmpty),
      "password" -> nonEmptyText) {
        (name, username, password) =>
          new TenantAdmin(UUID.randomUUID(), username, password, new Date, name, "en")

      } { (u: TenantAdmin) =>
        Some((u.getName, u.getUsername, ""))
      }
  }

  val updateUserForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "username" -> email.verifying(nonEmpty),
      "password" -> optional(text),
      "reset_pass" -> optional(boolean)) {
        (name, username, password, mres) =>
          val u = new TenantAdmin(UUID.randomUUID(), username, "", new Date, name, "en")
          if (mres.isDefined && mres.get) {
            u.setReset(mres.get)
            u.setPassword(password.getOrElse(""))
          }
          u
      } { (u: TenantAdmin) =>
        Some((u.getName, u.getUsername, None, Some(false)))
      }
  }
  def login = Action { implicit rs =>
    Ok(views.html.admin.login(loginForm))
  }

  def authenticate = Action { implicit rs =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.admin.login(formWithErrors)),
      {
        case (user, pass, remember) =>
          if (loginService.checkCredentials(user, pass)) {
            val langDb = loginService.getUser(user).flatMap(u => Option(u.getLanguage))
            val toPath = rs.session.get(REDIRECT_FROM).getOrElse(routes.TenantsController.index.path)
            val result =
              if (remember)
                updateSecCookie(Redirect(toPath), user)
              else
                Redirect(toPath).withSession(Security.username -> user)
            result.withLang(Lang(langDb.getOrElse("en")))
          } else
            Unauthorized(views.html.admin.login(loginForm.fill((user, pass, remember)).withGlobalError(Messages("login.error"))))
      })
  }

  def logout = Action {
    Redirect(routes.LoginController.login).withNewSession
      .discardingCookies(
        DiscardingCookie(Security.username),
        DiscardingCookie(messagesApi.langCookieName))
      .flashing(
        "success" -> Messages("logout"))
  }

  def viewUsers = Authenticated { implicit rs =>
    Ok(views.html.admin.viewUsers(loginService.getUsers))
  }

  def createUser = Authenticated { implicit rs =>
    Ok(views.html.admin.createUser(createUserForm))
  }

  def addUser = Authenticated { implicit rs =>
    createUserForm.bindFromRequest.fold(
      fwe => BadRequest(views.html.admin.createUser(fwe)),
      user => loginService.createUser(user) match {
        case Left(msg) =>
          Redirect(routes.LoginController.viewUsers()).flashing("error" -> msg)
        case Right(_) =>
          Redirect(routes.LoginController.viewUsers()).flashing("success" -> Messages("user_created"))
      })
  }

  def editUser(id: String) = Authenticated { implicit rs =>
    loginService.getUserById(UUID.fromString(id)) map { user =>
      Ok(views.html.admin.editUser(id, createUserForm.fill(user)))
    } getOrElse {
      Redirect(routes.LoginController.viewUsers()).flashing("error" -> Messages("invalid_user"))
    }
  }

  def updateDeleteUser(id: String) = Authenticated { implicit rs =>
    getAction match {
      case Some("delete") => deleteUser(id)(rs)
      case _ => updateUser(id)(rs)
    }
  }

  def deleteUser(id: String)(implicit rs: UserRequest[_]): Result = {
    loginService.getUserById(UUID.fromString(id)) map { user =>
      loginService.deleteUser(user) match {
        case Failure(e) =>
          Logger.debug("Error deleting tenant: ", e)
          Redirect(routes.LoginController.viewUsers()).flashing("error" -> e.getMessage)
        case Success(_) =>
          Redirect(routes.LoginController.viewUsers()).flashing("success" -> Messages("user_deleted"))
      }
    } getOrElse {
      Redirect(routes.TenantsController.tenants()).flashing("error" -> Messages("invalid_user"))
    }
  }

  def updateUser(id: String)(implicit rs: UserRequest[_]): Result = {
    val uuid = UUID.fromString(id)
    loginService.getUserById(uuid) map { existentUser =>
      updateUserForm.bindFromRequest.fold(
        fwe => BadRequest(views.html.admin.editUser(id, fwe)),
        user => {
          if (user.getReset && "".equals(user.getPassword))
            BadRequest(views.html.admin.editUser(id, updateUserForm.fill(user)
                .withError("password", Messages("password_required"))))
          else {
            user.setUserId(uuid)
            loginService.updateUser(existentUser, user) match {
              case Left(msg) =>
                Redirect(routes.LoginController.viewUsers()).flashing("error" -> msg)
              case Right(_) =>
                Redirect(routes.LoginController.viewUsers()).flashing("success" -> Messages("user_updated"))
            }
          }
        })
    } getOrElse {
      Redirect(routes.TenantsController.tenants()).flashing("error" -> Messages("invalid_user"))
    }
  }
}