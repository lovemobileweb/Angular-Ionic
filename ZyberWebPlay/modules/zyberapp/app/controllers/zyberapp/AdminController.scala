package controllers.zyberapp

import java.util.UUID
import javax.inject.Inject

import _root_.util.{MultitenancyHelper, Secured, Util}
import controllers.admin.Utils
import core.{ApiErrors, OnsuccessResult, ZyberResponse}
import models._
import models.extra.{JAdminActivity, JAdminActivityByTime, JFileActivity}
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import services.{ActivityService, FileService, LoginService, MetadataService, SecurityService}
import zyber.server.Abilities
import zyber.server.dao.{ActivityTimeline, Path, User}

class AdminController @Inject() (
    val loginService: LoginService,
    val activityService: ActivityService,
    val fileService: FileService,
    implicit val metadataService: MetadataService,
    val messagesApi: MessagesApi,
    val multitenancyHelper: MultitenancyHelper,
    val securityService: SecurityService) extends Secured with Controller with JsonFormats with I18nSupport {

  import Util._

  val createUserReads: Reads[(String, String, String, String, String)] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "language").read[String] and
    (JsPath \ "userRole").read[String])((_, _, _, _, _))

  val updateUserReads: Reads[(String, String, String, String, Option[String], Option[String])] = (
    (JsPath \ "uuid").read[String] and
    (JsPath \ "email").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "language").read[String] and
    (JsPath \ "userRole").readNullable[String] and
    (JsPath \ "resetPassword").readNullable[String])((_, _, _, _, _, _))

  def admin = AuthenticatedToModule(Abilities.DefaultUserRoles.powerUser) { implicit rs =>
    Ok(views.html.modules.administration())
  }

  def pathToJson(p: Path, user: User)(implicit u: User): JPath = {
    JPath.fromPathWithShares(p, fileService, loginService, List(user))
  }

  //noinspection MutatorLikeMethodIsParameterless
  def invalidPath(spath: String) = ApiErrors.single("invalid path",
    Messages("api.invalid.path", spath), NOT_FOUND)

  def getTermStore = Authenticated { implicit rs =>
    Ok(Json.toJson(metadataService.getTermStore.map(JTermStore.from(_))))
  }

  def getTerms(termStoreName: String) = Authenticated { implicit rs =>
    Ok(Json.toJson(metadataService.getTerms(termStoreName)))
  }

  def getPathMetadata(uuid: String) = Authenticated { implicit rs =>
    Ok {
      toJsonObj(metadataService.getPathMetadata(UUID.fromString(uuid))
        .map(JMetadata.from(_)))
    }
  }

  //#########################################################################

  /*Making use for ZyberResponse*/

  def getLoginAdminActivityView = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canViewActivy
      } yield {
        val activity: Seq[ActivityTimeline] = activityService.listLoginActivity
        val mapped = activity.map(a => (a, loginService.findById(a.getUserId)))
        mapped.map(a => JActivityTimeline.from(a._1, a._2))
      }
    }
  }

  implicit val passwordF = Json.format[JPasswordPolicy]

  def getPasswordPolicy = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(Utils.getPasswordPolicy)
    }
  }

  def savePasswordPolicy = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      val result: JsResult[JPasswordPolicy] = rs.body.validate[JPasswordPolicy]
      result.map(p => {
        metadataService.putApplicationSetting(Utils.policyKey, Json.toJson(p).toString())
        Right(p)
      }).get
    }
  }

  def getAdminActivityView(path: String, byTime: Option[Boolean] = Some(true),
                           sinceTime: Option[Long] = None) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canViewActivy
        files <- fileService.adminListFiles(path).toRight(ApiErrors.single("", Messages("api.invalid.path", path), BAD_REQUEST))
      } yield {
        val map = files.map(p =>
          pathToJson(p, rs.user)).toList

        val toInspect: List[Path] = if (path == "" || path == "/") files.toList else files.toList ++ List(fileService.adminLocationToPath(path).get)
        val activityView = activityService.listActivityRecursively(toInspect)

        if (byTime.getOrElse(true)) {
          //TODO : this should use some kind of paging using the sinceTime
          val flatten: List[(String, ActivityTimeline)] = activityView.flatMap(a => a._2.map(b => (a._1.getName, b))).sortBy(_._2.getActivityTimestamp).reverse
          val mapped = flatten.map(f => JActivityTimeline.fromActivity(f._2, loginService, Option(f._1)))
          val line = JAdminActivityByTime(files.map(a => JPath.fromPath(a, Nil)).toList, mapped)
          Json.toJson(line)
        } else {
          val mapped = activityView.map(a => JFileActivity(JPath.fromPath(a._1, Nil), a._2.map(b => JActivityTimeline.fromActivity(b, loginService))))
          Json.toJson(JAdminActivity(map, mapped))
        }
      }
    }
  }

  def updatePathMetadata(uuid: String) = AuthenticatedApi(parse.json) { implicit rs =>
    def validValue(jmd: JMetadata): Either[ApiErrors, Unit] = {
      if (jmd.value.isEmpty) Left { ApiErrors.single("Invalid metadata", Messages("api.empty.metadata"), BAD_REQUEST) }
      else Right { () }
    }
    ZyberResponse {
      for {
        _ <- loginService.canManageTermstore
        jmd <- ZyberResponse.jsonToResponse(rs.body.validate[JMetadata])
        _ <- validValue(jmd)
        _ <- metadataService.updateMetadata(jmd.to(UUID.fromString(uuid)))
      } yield {
        Messages("api.metadata.updated")
      }
    }
  }

  def deletePathMetadata(pathId: String, name: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canManageTermstore
        _ <- metadataService.deletePathMetadata(UUID.fromString(pathId), name)
      } yield {
        Messages("api.metadata_deleted")
      }
    }
  }

  def addTermStore = AuthenticatedApi(parse.json) { implicit rs =>
    val termStore = (rs.body \ "termStore")
    val terms = (rs.body \ "terms")

    ZyberResponse {
      for {
        _ <- loginService.canManageTermstore
        jtermStore <- ZyberResponse.jsonToResponse(termStore.validate[JTermStore])
        terms <- ZyberResponse.jsonToResponse(terms.validate[Seq[String]])
        _ <- metadataService.addTermstore(jtermStore.toTermStore, terms)
      } yield {
        Messages("api.termStore.saved")
      }
    }
  }

  def updateTermStore(uuid: String) = AuthenticatedApi(parse.json) { implicit rs =>
    val termStore = (rs.body \ "termStore")
    val terms = (rs.body \ "terms")

    ZyberResponse {
      for {
        _ <- loginService.canManageTermstore
        jtermStore <- ZyberResponse.jsonToResponse(termStore.validate[JTermStore])
        terms <- ZyberResponse.jsonToResponse(terms.validate[Seq[String]])
        _ <- metadataService.updateTermstore(jtermStore.toTermStore(UUID.fromString(uuid)), terms)
      } yield {
        Messages("api.termStore.saved")
      }
    }
  }

  def deleteTermStore(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canManageTermstore
        _ <- metadataService.deleteTermstore(UUID.fromString(uuid))
      } yield {
        Messages("api.termStore.deleted")
      }
    }
  }

  def getUserRoles = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(loginService.getUserRoles.map(JsUserRole.fromUserRole(_)))
    }
  }

  def users = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateUsers
      } yield {
        loginService.getActiveUsers.map { u => JUser.fromUser(u) }
      }
    }
  }

  def createUser = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      def userDoesNotExist(email: String): Either[ApiErrors, Unit] = {
        loginService.getUser(email)(rs.tenant.getTenantId) match {
          case Some(user) =>
            Left(ApiErrors.single("user already exists", Messages("account.create.user.exists"), BAD_REQUEST))
          case None => Right(())
        }
      }
      for {
        _ <- loginService.canCreateUsers
        jsValue <- ZyberResponse.jsonToResponse(rs.body.validate[(String, String, String, String, String)](createUserReads))
        (name, email, password, lang, userRoleId) = jsValue
        _ <- userDoesNotExist(email)
        createdUser <- loginService.createUserApi(email, password, name, lang, UUID.fromString(userRoleId))(rs.tenant.getTenantId)
      } yield {
        Messages("account.create.success")
      }
    }
  }

  def updateUser = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse.afterResult {
      for {
        _ <- loginService.canCreateUsers
        jsValue <- ZyberResponse.jsonToResponse(
          rs.body.validate[(String, String, String, String, Option[String], Option[String])](updateUserReads))
        (uuid, email, name, lang, maybeUserRoleId, maybePass) = jsValue
        user <- loginService.getUserById(UUID.fromString(uuid)).toRight(invalidPath(uuid))
        _ <- loginService.checkEmail(user, email, rs.tenant.getTenantId)
        _ <- loginService.updateUser(UUID.fromString(uuid))(name, email, lang, maybeUserRoleId.map(UUID.fromString), maybePass)
      } yield {
        OnsuccessResult(Messages("account.update.success"), (_.withLang(Lang(lang))))
      }
    }
  }

  def deleteUser(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateUsers
        u <- loginService.getUserById(UUID.fromString(uuid)).filter(_.getActive).toRight(
          ApiErrors.single("User does not exist", Messages("user_not_found"), NOT_FOUND))
        _ <- loginService.deleteUser(u.getUserId)
      } yield {
        Messages("user.deleted")
      }
    }
  }

  def getUser(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- loginService.canCreateUsers
        user <- loginService.getUserById(UUID.fromString(uuid)).toRight(
          ApiErrors.single("invalid user", Messages("invalid_user"), BAD_REQUEST))
      } yield {
        JUser.fromUser(user)
      }
    }
  }
}