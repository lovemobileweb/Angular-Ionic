package controllers.zyberapp

import java.util.UUID
import javax.inject.Inject

import bridge.SharingBridge
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.{FileService, LoginService}
import util.{MultitenancyHelper, Secured, Util}

class Application @Inject() (
    val loginService: LoginService,
    val fileService: FileService,
    val messagesApi: MessagesApi,
    val multitenancyHelper: MultitenancyHelper) extends Secured with Controller with I18nSupport {

  val sharingBridge = new SharingBridge(fileService, loginService)

  val folderForm = Form(
    single(
      "folderName" -> nonEmptyText.verifying(Messages("invalid.foldername"), f => f.matches("[^\\/:?*\"|]+"))))
   
  val restoreForm = Form(
    single(
      "restore" -> nonEmptyText.verifying(Messages("missing.version"), f => f.matches("\\d+"))))

  def index = Authenticated { implicit rs =>
    Redirect(routes.HomeController.home(""))
  }

  def downloadFile(uuid: UUID) = Authenticated { implicit rs =>
    fileService.downloadPath(uuid, rs.user).map(Util.pathToResult).getOrElse(NotFound)
  }

  def downloadRestricted(uuid: UUID) = Authenticated { implicit rs =>
    sharingBridge.downloadRestricted(uuid,rs.user)
  }

  def downloadFileVersion(uuid: UUID, version: Long) = Authenticated { implicit rs =>
    (fileService.downloadPath(uuid, rs.user), fileService.getFileVersion(uuid, version, rs.user)) match {
      case (Some(path), Some(revision)) =>
        Util.pathToResult(path)
      case _ => NotFound
    }
  }
}
