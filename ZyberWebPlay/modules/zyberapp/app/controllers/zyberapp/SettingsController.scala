package controllers.zyberapp

import java.util.UUID
import util.{Util => ZyberUtil}
import javax.inject.Inject
import _root_.util.{Secured, MultitenancyHelper}
import core.{ApiErrors, OnsuccessResult, ZyberResponse}
import models._
import play.api._
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import services.{PasswordReuseException, LoginService}
import zyber.server.dao.User
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}


class SettingsController @Inject() (
    val loginService: LoginService,
    val messagesApi: MessagesApi,
    val multitenancyHelper: MultitenancyHelper) extends Secured with Controller with JsonFormats with I18nSupport {
  
  import ZyberUtil._

  val updateUserReads: Reads[(String, String, String, String, Option[String], Option[String])] = (
    (JsPath \ "uuid").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "language").read[String] and
    (JsPath \ "userRole").readNullable[String] and
    (JsPath \ "resetPassword").readNullable[String])((_, _, _, _, _, _))

  def settings = (Authenticated andThen WithAbilities) { implicit rs =>
    Ok(views.html.modules.settings())
  }

  //noinspection MutatorLikeMethodIsParameterless

  def invalidUser(spath: String) = ApiErrors.single("invalid user",
    Messages("api.invalid.user", spath), NOT_FOUND)

  def currentUser = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(JUser.fromUser(rs.user))
    }
  }

  val passwordReads: Reads[(String, String)] = (
    (JsPath \ "password").read[String] and
    (JsPath \ "newpassword").read[String])((_, _))

  def changePassword = Authenticated(parse.json) { implicit rs =>
    rs.body.validate[(String, String)](passwordReads).map {
      case (password, newpassword) =>
        if (loginService.checkPassword(rs.user, password)) {
          loginService.changePassword(rs.user, newpassword) match {
            case Success(_) => Ok(Messages("account.update.success"))
            case Failure(e) =>
              e match {
                //TODO: support other languages
                case t:PasswordReuseException => BadRequest("You cannot reuse an existing password")
                case _ => {
                  Logger.error("Error changing password", e)
                  InternalServerError(e.getMessage)
                }
              }
          }
        } else {
          BadRequest(Messages("password.error"))
        }
    } getOrElse { BadRequest("Invalid json") }
  }

  def getMessages = Action { implicit rs =>
    val pref = rs.acceptLanguages.map(_.code).headOption
    val defaultMap = messagesApi.messages.get("default")
    val prefMap = pref.flatMap { pl => messagesApi.messages.get(pl) } orElse Some(Map())
    val res = defaultMap.flatMap { dm => prefMap.map { pm => dm ++ pm } }
    Ok(Json.toJson(res))
  }

  def userLanguage = Authenticated { implicit rs =>
    Ok(rs.user.getLanguage)
  }

  def getUserMessages = Authenticated { implicit rs =>
    val pref = Option(rs.user.getLanguage)
    val defaultMap = messagesApi.messages.get("default")
    val prefMap = pref.flatMap { pl => messagesApi.messages.get(pl) } orElse Some(Map())
    val res = defaultMap.flatMap { dm => prefMap.map { pm => dm ++ pm } }
    Ok(Json.toJson(res))
  }

  def updateAccount = AuthenticatedApi(parse.json) { implicit rs =>

    def sameAccount(user: User): Either[ApiErrors, Unit] = {
      if (user.getUserId.equals(rs.user.getUserId))
        Right(())
      else {
        Left { ApiErrors.single("User can only update his account", Messages("invalid_account"), FORBIDDEN) }
      }
    }
    ZyberResponse.afterResult {
      for {
        jsValue <- ZyberResponse.jsonToResponse(rs.body.validate[(String, String, String, String, Option[String], Option[String])](updateUserReads))
        (uuid, email, name, lang, maybeUserRoleId, maybePass) = jsValue
        user <- loginService.getUserById(UUID.fromString(uuid)).toRight(invalidUser(uuid))
        _ <- sameAccount(user)
        _ <- loginService.checkEmail(user, email, rs.tenant.getTenantId)
        _ <- loginService.updateUser(UUID.fromString(uuid))(name, email, lang, None, maybePass)
      } yield {
        OnsuccessResult(Messages("account.update.success"), (_.withLang(Lang(lang))))
      }
    }
  }

  def supportedLanguages = Action { implicit rs =>
    ZyberResponse {
      try {
        Right {
          val cod = play.Play.application.configuration.getStringList("play.i18n.langs")
          val lab = play.Play.application.configuration.getStringList("languages.label")
          val jsRes = (cod zip lab).map {
            case (cod, label) => Json.obj("cod" -> cod, "value" -> label)
          }
          Json.toJson(jsRes)
        }
      } catch {
        case e: Exception => Left(ApiErrors.fromException(e))
      }
    }
  }

}