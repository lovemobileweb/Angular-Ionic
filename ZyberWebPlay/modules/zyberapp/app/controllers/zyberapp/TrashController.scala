package controllers.zyberapp

import java.util.UUID
import javax.inject.Inject
import _root_.util.{ Secured, MultitenancyHelper, UserRequest, Util }
import Util._
import models.{ JPath, JVersion, JsonFormats }
import play.api._
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.json._
import play.api.mvc._
import services.{ FileService, LoginService, TrashService }
import zyber.server.dao._
import core.ZyberResponse
import core.WrappedResult

class TrashController @Inject() (
  val loginService: LoginService,
  val fileService: FileService,
  val trashService: TrashService,
  val messagesApi: MessagesApi,
  val multitenancyHelper: MultitenancyHelper) extends Secured with Controller
    with I18nSupport with JsonFormats {

  def getFiles(path: String, ord: Option[String], t: Option[String]) = Authenticated { implicit rs =>
    Logger.debug(s"Getting Files")
    Logger.debug(s"path without decoding $path")
    val spath = decode(path)
    Logger.debug(s"decoding path $spath")
    if (path == "") {
      val map: List[Path] = trashService.listTrash(rs.user.getUserId).flatMap(p => fileService.getPathByUUID(p.getPathId, true))
      Ok(Json.toJson(map.map(p => pathToJson(p, rs.user))))
    } else {
      val last = UUID.fromString(path.split("/").last)
      val containingPath = fileService.existingPath(last, true)
      fileService.sortedFilesUnder(ord, t, Some(containingPath), true) match {
        case None        => BadRequest(Messages("api.invalid.path", spath))
        case Some(files) => Ok(Json.toJson(files.map(p => pathToJson(p, rs.user))))
      }
    }
  }

  def pathToJson(p: Path, user: User)(implicit u: User): JPath = {
    JPath.fromPathWithShares(p, fileService, loginService, List(user))
  }

  def undelete(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse.withMessage {
      Right(WrappedResult(
          JPath.fromPathWithShares(
              fileService.restore(UUID.fromString(uuid), rs.user), 
              fileService, loginService), Messages("file_restored")))
    }
  }

  def pathToResponse(path: Path, rs: UserRequest[AnyContent])(implicit u: User): Result = {
    fileService.listVersionsWithActivity(rs.user, path) match {
      case Nil => BadRequest(Messages("api.invalid.path_id", path.getPathId))
      case list => Ok(Json.toJson({
        val zipped: Seq[((FileVersion, ActivityTimeline), Int)] = list.zipWithIndex
        zipped.map(m => JVersion.from(m._1._1, m._1._2, path.getName, zipped.size - m._2))
      }))
    }
  }

}
