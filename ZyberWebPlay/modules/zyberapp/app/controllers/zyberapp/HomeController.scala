package controllers.zyberapp

import java.util.Date
import java.util.UUID

import com.datastax.driver.mapping.Mapper.{ Option => CassandraOption }
import com.groupdocs.viewer.config.ServiceConfiguration
import com.groupdocs.viewer.handlers.ViewerHandler

import bridge.SharingBridge
import core.ApiErrors
import core.StreamingBodyParser
import core.StreamingChunksBodyParser
import core.WrappedResult
import core.ZyberResponse
import document.viewer.config.Configuration
import javax.inject._
import models.JActivityTimeline
import models.JPath
import models.JPermissions
import models.JPrincipal
import models.JVersion
import models.JsonFormats
import models.extra.FlowChunkInfo
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Cookie
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import services.FileService
import services.LoginService
import services.SearchService
import services.SecurityService
import util.MultitenancyHelper
import util.Secured
import util.UserRequest
import util.Util
import zyber.server.dao.ActivityTimeline
import zyber.server.dao.FileVersion
import zyber.server.dao.HasPathOutputStream
import zyber.server.dao.Path
import zyber.server.dao.User
import zyber.server.dao.ViewerInfo

class HomeController @Inject() (
  val loginService: LoginService,
  val fileService: FileService,
  val searchService: SearchService,
  val messagesApi: MessagesApi,
  val multitenancyHelper: MultitenancyHelper,
  val securityService: SecurityService) extends Secured with Controller
    with I18nSupport with JsonFormats {

  import Util._

  val config = new ServiceConfiguration(new Configuration())
  val viewerHandler = new ViewerHandler(config)
  val viewerId = "groupDocsViewer"

  val sharingBridge = new SharingBridge(fileService, loginService)

  def shares(path: String) = home(path)

  val createFolderRead: Reads[(String, String)] = (
    (JsPath \ "path").read[String] and
    (JsPath \ "folderName").read[String])((_, _))

  val changeNameRead: Reads[(String, String)] = (
    (JsPath \ "uuid").read[String] and
    (JsPath \ "name").read[String])((_, _))

  val flowInfoRead: Reads[(String, String, Option[Boolean], String)] = (
    (JsPath \ "flowIdentifier").read[String] and
    (JsPath \ "flowFilename").read[String] and
    (JsPath \ "add_version").readNullable[Boolean] and
    (JsPath \ "relativePath").read[String])((_, _, _, _))

  val unshareRead: Reads[(String, Option[String])] = (
    (JsPath \ "src_path").read[String] and
    (JsPath \ "parent_path").readNullable[String])((_, _))

  val movePathRead: Reads[(Seq[UUID], UUID)] = (
    (JsPath \ "paths").read[Seq[UUID]] and
    (JsPath \ "dstPath").read[UUID])((_, _))

  def home(path: String) = (Authenticated andThen WithAbilities) { implicit rs =>
    Ok(views.html.modules.home(""))
  }

  private val tokenTtl = 30 * 60 // Token for 30 min preview
  /* The groupDocs library seems to do an on-demand rendering for previews, so
   * the token will probably be required to live more than 5 minutes (in cassandraInputDataHandler)
   * when the user previews the file for more time. A possible solution to this could be
   * to create an endpoint for generating the token and another one for updating its ttl,
   * then from client we would call update ttl endpoint every N minutes (checking authentication
   * and permissions each time) to update the token as long as the user is doing the preview. This
   * would be necessary because we don't authenticate on the servlet endpoints for groupDocs.
   * TODO Implement this if we stick to GroupDocs library, for now we just increase ttl to 30 min.
   * */
  def viewer(path: String) = Authenticated { implicit rs =>
    Logger.debug("Creating viewer...")
    fileService.getPathByUUID(UUID.fromString(path), false) match {
      case None => BadRequest(views.html.notFound())
      case Some(p) =>
        val tokenId = UUID.randomUUID
        val user = rs.user
        val viewerInfo = new ViewerInfo(tokenId, user.getTenantId, user.getUserId, p.getPathId, new Date)
        val viewerInfoMapper = fileService.userSession.session.getMappingManager.mapper(classOf[ViewerInfo])
        viewerInfoMapper.save(viewerInfo, CassandraOption.ttl(tokenTtl))

        val locale = viewerHandler.getLocale()
        Ok(views.html.shared.helper.docViewer(viewerHandler.getHeader, viewerHandler.getViewerScript(
          viewerId, tokenId.toString, locale)))
    }
  }

  def pathToJson(p: Path, user: User)(implicit u: User): JPath = {
    JPath.fromPathWithShares(p, fileService, loginService, List(user))
  }

  def fileSharing(pathId: String) = Authenticated(parse.json) { implicit rs =>
    rs.body.validate(shareSubmitFormat).map(s => {
      fileService.getPathByUUID(UUID.fromString(pathId)).map(p => {
        Ok(Json.toJson(sharingBridge.setSharesFor(p, s, rs.user)))
      }).getOrElse(NotFound)

    }).getOrElse(BadRequest)
  }

  def folderSharing(pathId: String) = Authenticated(parse.json) { implicit rs =>
    rs.body.validate(shareSubmitFormat).map(s => {
      fileService.getPathByUUID(UUID.fromString(pathId)).map(p => {
        Ok(Json.toJson(sharingBridge.setSharesForFolder(p, s, rs.user)))
      }).getOrElse(NotFound)
    }).getOrElse(BadRequest)
  }

  def undelete(pathId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse.withMessage {
      Right {
        WrappedResult(
          JPath.fromPathWithShares(
            fileService.restore(UUID.fromString(pathId), rs.user),
            fileService, loginService), Messages("file_restored"))
      }
    }
  }

  //##############################################
  //Making use of ZyberResponse for API

  def search(name: String, spath: Option[String], view: Option[String],
             hiddenOnly: Option[Boolean], showHidden: Option[Boolean],
             limit: Option[Boolean]) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Logger.debug(s"Searching Files in $view for $name")
      for {
        pathId <- fileService.getUUID(spath.getOrElse(""), view)
        path <- fileService.getPathByUUID(pathId).toRight(invalidPath(pathId.toString))
        _ <- securityService.canViewOrAccessFolder(path)
      } yield {
        val searchResult = searchService.search(
          rs.user, name, hiddenOnly.getOrElse(false),
          showHidden.getOrElse(false), path, limit.getOrElse(true))
        searchResult.map { p =>
          val d: Option[Path] = fileService.getPathByUUID(p.getParentPathId)
          JPath.fromPath(p).copy(directoryPath = d.map(x =>
            fileService.getPathHierarchy(x).map(_.getName).mkString("/")), parentUuid = d.map(_.getPathId.toString))
        }
      }
    }
    //    val search1: List[Path] = searchService.search(rs.user, name, hiddenOnly.getOrElse(false), showHidden.getOrElse(false))
    //    val rootPath: Path = loginService.getRootPath
    //    Ok {
    //      //      Json.toJson(search1.map(JPath.fromPath(_)).map(m => m.copy(directoryPath = Some(fileService.fullContainingPath(fileService.existingPath(UUID.fromString(m.uuid), true), rs.user)))))}
    //      Json.toJson(search1.map { p =>
    //        //        val parent = fileService.existingPath(p.getParentPathId, true)
    //        val d: Option[Path] = fileService.getPathByUUID(p.getParentPathId)
    //        JPath.fromPath(p).copy(directoryPath = d.map(x =>
    //          fileService.getPathHierarchy(x).map(_.getName).mkString("/")), parentUuid = d.map(_.getPathId.toString))
    //      })
    //    }
  }

  //Security part in home
  def getPrincipalsPermissions(pathId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        path <- fileService.getPathByUUID(UUID.fromString(pathId)).toRight(invalidPath(pathId))
        _ <- securityService.canViewPermissions(path)
      } yield {
        securityService.getPrincipalsForPath(path.getPathId).map { p =>
          val permissions = securityService.getPermissionSetForPrincipal(path.getPathId, p.getPrincipalId)
          JPrincipal.withPermissions(p, permissions)
        }
      }
    }
  }

  def addPrincipalToPath(pathId: String) = Authenticated(parse.json) { implicit rs =>
    ZyberResponse.withMessage {
      for {
        princName <- ZyberResponse.jsonToResponse((rs.body \ "principal_name").validate[String])
        setRecursively <- ZyberResponse.jsonToResponseWithDefault(
          (rs.body \ "set_recursively").validate[Boolean], true)
        path <- fileService.getPathByUUID(UUID.fromString(pathId)).toRight(invalidPath(pathId))
        _ <- securityService.canAllowAccessToFolder(path)
        principal <- securityService.addPrincipalToPath(path.getPathId, princName, setRecursively)
      } yield {
        WrappedResult(JPrincipal(
          principal.getPrincipalId.toString,
          principal.getType.name,
          principal.getCreatedDate,
          principal.getDisplayName),
          Messages("principal_added_to_path"))
      }
    }
  }

  def removePricipalFromPath(pathId: String, princId: String, setRecursively: Option[Boolean]) = Authenticated { implicit rs =>
    ZyberResponse {
      for {
        path <- fileService.getPathByUUID(UUID.fromString(pathId)).toRight(invalidPath(pathId))
        _ <- securityService.canRemoveAccessToFolder(path)
        _ <- securityService.removePrincipalFromPath(
          UUID.fromString(pathId), UUID.fromString(princId), setRecursively.getOrElse(true))
      } yield {
        Messages("principal_removed_from_path")
      }
    }
  }

  def updatePermissionSetsForPrincipal(pathId: String, princId: String) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        permissions <- ZyberResponse.jsonToResponse(rs.body.validate[JPermissions])
        setRecursively <- ZyberResponse.jsonToResponseWithDefault(
          (rs.body \ "set_recursively").validate[Boolean], true)
        path <- fileService.getPathByUUID(UUID.fromString(pathId)).toRight(invalidPath(pathId))
        _ <- securityService.canAllowAccessToFolder(path)
        _ <- securityService.setPermissionSetForPrincipal(
          path.getPathId, UUID.fromString(princId), permissions, setRecursively)
      } yield {
        Messages("permissions_updated")
      }
    }
  }

  def getPermissionSets() = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(securityService.getPermisionSets)
    }
  }

  //TODO remove method ??
  def getPermisionSetForPrincipal(pathId: String, princId: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(securityService.getPermissionSetForPrincipal(UUID.fromString(pathId), UUID.fromString(princId)))
    }
  }

  def invalidPath(spath: String) = ApiErrors.single("invalid path",
    Messages("api.invalid.path", spath), NOT_FOUND)

  def getFiles(spath: String, showHidden: Boolean,
               ord: Option[String], t: Option[String], view: Option[String]) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Logger.debug(s"Getting Files for " + spath)
      //      Thread.sleep(10000) //REMOVE
      for {
        pathId <- fileService.getUUID(spath, view)
        path <- fileService.getPathByUUID(pathId).toRight(invalidPath(pathId.toString))
        _ <- securityService.canViewOrAccessFolder(path)
        files <- fileService.listFiles(rs.user, pathId.toString(), showHidden, ord, t).toRight(invalidPath(spath))
      } yield {
        files.map(p => pathToJson(p, rs.user))
      }
    }
  }
  
  
  def getPath(pathId: UUID) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Logger.debug(s"Getting File id " + pathId)
      //      Thread.sleep(10000) //REMOVE
      for {
        path <- fileService.getPathByUUID(pathId).toRight(invalidPath(pathId.toString))
        _ <- orElseEither(securityService.canViewOrAccessFolder(path), securityService.canViewFile(path))
      } yield {
        JPath.fromPath(path)
      }
    }
  }

  def rename = Authenticated(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        p <- ZyberResponse.jsonToResponse(rs.body.validate[(String, String)](changeNameRead))
        path <- fileService.getPathByUUID(UUID.fromString(p._1)).toRight(invalidPath(p._1))
        _ <- securityService.canRenameFile(path)
        path2 <- fileService.rename(path, p._2)
      } yield {
        Messages("file_renamed")
      }
    }
  }

  def createFolder(view: Option[String]) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        pf <- ZyberResponse.jsonToResponse(rs.body.validate[(String, String)](createFolderRead))
        pathId <- fileService.getUUID(pf._1, view).right
        parentPath <- fileService.getPathByUUID(pathId).toRight(invalidPath(pathId.toString))
        _ <- securityService.canAddToFolder(parentPath)
        createdFolder <- fileService.createFolder(parentPath, pf._2)
      } yield {
        Messages("api.folder.created", createdFolder.getName)
      }
    }
  }

  private def checkUpload(spath: String, user: User, view: Option[String]): Either[ApiErrors, Path] = {
    for {
      //      path <- fileService.locationToPath(user, spath, false)(user).toRight(invalidPath(spath)).right
      pathId <- fileService.getUUID(spath, view)(user)
      path <- fileService.getPathByUUID(pathId)(user).toRight(invalidPath(pathId.toString()))
      _ <- securityService.canAddToFolder(path)(user)
    } yield {
      path
    }
  }

  private def streamConstructor(spath: String, view: Option[String]): RequestHeader => String => Option[HasPathOutputStream] = {
    rh =>
      fn =>
        username(rh).flatMap { implicit user =>
          val res = for {
            path <- checkUpload(spath, user, view)
          } yield {
            fileService.getOSPath(fn, user, path.getPathId)
          }
          res match {
            case Right(mos) => mos
            case Left(_)    => None
          }
        }
  }

  //Reference: https://github.com/heiflo/play21-file-upload-streaming. For non-chunked uploads
  private def doStreamUpload(spath: String, view: Option[String]) = AuthenticatedApi(StreamingBodyParser.streamingBodyParser(streamConstructor(spath, view))) { implicit rs =>
    ZyberResponse {
      for {
        _ <- checkUpload(spath, rs.user, view)
        streamSuccess <- rs.body.files.head.ref.left.map { se =>
          ApiErrors.single("Stream upload error: ", se.errorMessage, INTERNAL_SERVER_ERROR)
        }.right
      } yield {
        //Update mimeType
        streamSuccess.path.foreach { p =>
          p.setZus(fileService.userSession)
          fileService.updateMimetypeFor(p)
        }
        Messages("api.successfull.upload", streamSuccess.filename)
      }
    }
  }

  //  def streamUpload(pathId: String, view: Option[String]) = doStreamChunkedUpload(pathId, view)
  def streamUpload() = doStreamChunkedUpload()

  def doStreamChunkedUpload() =
    AuthenticatedApi(StreamingChunksBodyParser.chunksBodyParser) { implicit rs =>
      rs.body.files.head.ref match {
        case Left(error) => InternalServerError
        case Right(streamSuccess) =>
          val chunkInfo = flowChunkInfo(rs.body.dataParts)
          Logger.debug("Received chunk info: " + chunkInfo)
          fileService.saveChunk(chunkInfo, streamSuccess.bytes)
          Ok
      }
    }

  def flowChunkInfo(m: Map[String, Seq[String]]): FlowChunkInfo = {
    FlowChunkInfo(
      m.get("flowChunkNumber").get.head.toInt,
      m.get("flowChunkSize").get.head.toInt,
      m.get("flowCurrentChunkSize").get.head.toInt,
      m.get("flowTotalSize").get.head.toLong,
      m.get("flowTotalChunks").get.head.toInt,
      m.get("flowIdentifier").get.head,
      m.get("flowFilename").get.head)
  }

  def completeUpload(spath: String, view: Option[String]) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        parentPath <- checkUpload(spath, rs.user, view)
        flowInfo <- ZyberResponse.jsonToResponse(
          rs.body.validate[(String, String, Option[Boolean], String)](flowInfoRead))
        (flowIdent, filename, addVersion, relativePath) = flowInfo
        localParentPath <- fileService.getDestinationPath(parentPath, filename, relativePath, true)
        hpos <- fileService.completeUpload(localParentPath, flowIdent, filename, addVersion.getOrElse(true))
      } yield {
        val p = hpos.getPath
        p.setZus(fileService.userSession)
        fileService.updateMimetypeFor(p)
        Messages("api.successfull.upload", filename)
      }
    }
  }

  def checkUploadName(spath: String, view: Option[String]) = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        name <- ZyberResponse.jsonToResponse((rs.body \ "name").validate[String])
        relativePath <- ZyberResponse.jsonToResponse((rs.body \ "relativePath").validate[String])
        pathId <- fileService.getUUID(spath, view)
        path <- fileService.getPathByUUID(pathId).toRight(invalidPath(pathId.toString()))
      } yield {
        val maybeParent =
          fileService.getDestinationPath(path, name, relativePath, false).fold(_ => None, x => Some(x))

        val maybePath = maybeParent.flatMap { pp =>
          fileService.getNamedChild(pp.getPathId, name)
        }
        Json.obj(
          "existent" -> maybePath.isDefined,
          "confirm_message" -> Messages("confirm_new_version_message", maybePath.map(_.getName).getOrElse(""),
            fileService.getLocalizedFolderName(maybeParent.getOrElse(path))))
      }
    }
  }

  def checkUploadPermissions(spath: String, view: Option[String]) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        parentPath <- checkUpload(spath, rs.user, view)
      } yield {
        "Upload allowed"
      }
    }
  }

  def testChunks( /*spath: String, view: Option[String],*/ flowChunkNumber: Int, flowIdentifier: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        _ <- fileService.testChunk(flowChunkNumber, flowIdentifier)
      } yield {
        "Upload allowed"
      }
    }
  }

  def delete(uuid: String) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        path <- fileService.getPathByUUID(UUID.fromString(uuid), false).toRight(invalidPath(uuid))
        _ <- securityService.canDeleteFile(path)
      } yield {
        fileService.delete(path)
        Messages("file.deleted")
      }
    }
  }

  def deleteFiles = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        paths <- ZyberResponse.jsonToResponse(rs.body.validate[Seq[String]])
        jsonRes <- fileService.deleteFiles(paths)
      } yield {
        jsonRes
      }
    }
  }

  def downloadFile(uuid: UUID) = AuthenticatedApi { implicit rs =>
    doDownload(uuid)
  }

  //  def downloadFileForIonic(uuid: UUID) = AuthenticatedInParm { implicit rs =>
  //    doDownload(uuid)
  //  }

  private def doDownload[A](uuid: UUID)(implicit rs: UserRequest[A]): Result = {
    val res = for {
      path <- fileService.getPathByUUID(uuid, false).toRight(NotFound)
      _ <- securityService.canViewFile(path).left.map(_ => Unauthorized)
      dp <- fileService.downloadPath(uuid, rs.user).toRight(NotFound)
    } yield {
      Logger.debug("Downloading file")
      Util.pathToResult(dp).withCookies(Cookie("Set-fileDownload",
        "true"), Cookie("path", "/"))
    }
    res.fold(identity, identity)
  }

  def canDownload(uuid: UUID) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        path <- fileService.getPathByUUID(uuid, false).toRight(ApiErrors.single("Invalid path", Messages("invalid_path"), NOT_FOUND))
        _ <- securityService.canViewFile(path)
        dp <- fileService.downloadPath(uuid, rs.user).toRight(ApiErrors.single("Invalid path", Messages("invalid_path"), NOT_FOUND))
      } yield {
        Messages("Ok") //We can return temporal token here in case we cannot use headers for token auth
      }
    }
  }

  def getVersions(pathId: UUID) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        path <- fileService.getPathByUUID(pathId, false).toRight(invalidPath(pathId.toString()))
        _ <- securityService.canViewFileHistory(path)
        versions <- getVersionsInternal(path, rs)
      } yield {
        versions
      }
    }
  }

  def getVersionsInternal(path: Path,
                          rs: UserRequest[AnyContent])(implicit u: User): Either[ApiErrors, Seq[JVersion]] = {
    Logger.debug(s"Getting versions for: " + path.getPathId.toString())

    //    val asPath = fileService.locationToPath(rs.user, thePath, false)
    //    asPath.map(p => pathToResponse(p, rs)).getOrElse(BadRequest(Messages("api.invalid.path", thePath)))
    for {
      versions <- pathToResponse(path, rs).right
    } yield {
      versions
    }
  }

  def pathToResponse(path: Path,
                     rs: UserRequest[AnyContent])(implicit u: User): Either[ApiErrors, Seq[JVersion]] = {
    fileService.listVersionsWithActivity(rs.user, path) match {
      case Nil => Left(ApiErrors.single("Invalid path:" + path.getParentPathId,
        Messages("api.invalid.path_id", path.getPathId), NOT_FOUND))
      case list => Right {
        val zipped: Seq[((FileVersion, ActivityTimeline), Int)] = list.zipWithIndex
        zipped.map(m => JVersion.from(m._1._1, m._1._2, path.getName, zipped.size - m._2))
      }
    }
  }

  def restoreVersion(uuid: UUID, version: Long) = AuthenticatedApi { implicit rs =>
    ZyberResponse.withMessage {
      for {
        pathId <- fileService.getUUID(uuid.toString(), None)
        path <- fileService.getPathByUUID(pathId, false).toRight(invalidPath(uuid.toString()))
        _ <- securityService.canRestoreFile(path)
        restored <- fileService.restoreVersion(uuid, version, rs.user).toRight(invalidPath(uuid.toString()))
        versions <- pathToResponse(restored, rs)
      } yield {
        WrappedResult(versions, Messages("successful_restored"))
      }
    }
  }

  def constructHierarchyFor(spathid: String, view: Option[String]) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        pathId <- fileService.getUUID(spathid, view)
        hierarchy <- fileService.getPathHierarchy(pathId)
      } yield {
        hierarchy.map(JPath.fromPath(_))
      }
    }
  }

  def getActivity(spath: String, showHidden: Boolean, view: Option[String]) = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      for {
        pathId <- fileService.getUUID(spath, view)
        path <- fileService.getPathByUUID(pathId, showHidden).toRight(invalidPath(spath))
        _ <- securityService.canViewFileHistory(path)
      } yield {
        fileService.getActivity(path).map { at =>
          JActivityTimeline.from(at, loginService.findById(at.getUserId))
        }
      }
    }
  }

  def moveToShares = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        pf <- ZyberResponse.jsonToResponse(rs.body.validate[(String, String)](createFolderRead))
        pathId <- fileService.getUUID(pf._1, None)
        destPathId <- fileService.getUUID("", Some(fileService.sharesFolderName))
        path <- fileService.getPathByUUID(pathId, false).toRight(invalidPath(pathId.toString))
        _ <- securityService.canMoveFolder(path)
        _ <- fileService.movePath(pathId, destPathId, Some(pf._2))
      } yield {
        Messages("folder_shared")
      }
    }
  }

  def movePathsTo = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        pf <- ZyberResponse.jsonToResponse(rs.body.validate[(Seq[UUID], UUID)](movePathRead))
        (pathIds, destPathId) = pf
        dstPath <- fileService.getPathByUUID(destPathId, false).toRight(invalidPath(destPathId.toString))
        _ <- securityService.canAddToFolder(dstPath)
        mf <- fileService.moveRenamingPaths(pathIds, dstPath)
      } yield {
        Messages("files_moved", mf)
      }
    }
  }

  def copyPathsTo = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        pf <- ZyberResponse.jsonToResponse(rs.body.validate[(Seq[UUID], UUID)](movePathRead))
        (pathIds, destPathId) = pf
        dstPath <- fileService.getPathByUUID(destPathId, false).toRight(invalidPath(destPathId.toString))
        _ <- securityService.canAddToFolder(dstPath)
        cf <- fileService.copyRenamingPaths(pathIds, dstPath)
      } yield {
        Messages("files_copied", cf)
      }
    }
  }

  def unshareFolder = AuthenticatedApi(parse.json) { implicit rs =>
    ZyberResponse {
      for {
        jsInfo <- ZyberResponse.jsonToResponse(rs.body.validate[(String, Option[String])](unshareRead))
        (folderId, dstId) = jsInfo
        _ <- fileService.unshareFolder(folderId, dstId)
      } yield {
        Messages("folder_unshared")
      }
    }
  }

  def getHomeDirectoryStructure = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(Seq(fileService.directoryTreeForHome))
    }
  }

  def getSharesStructureForUser = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(Seq(fileService.directoryTreeForShares))
    }
  }

  def getSharesAndHomeStructureForUser = AuthenticatedApi { implicit rs =>
    ZyberResponse {
      Right(fileService.directoryTreeForShares.map(s => Seq(fileService.directoryTreeForHome, s)))
    }
  }
}
