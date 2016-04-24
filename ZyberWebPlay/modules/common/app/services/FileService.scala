package services

import java.io.OutputStream
import java.net.URLEncoder
import java.util.Date
import java.util.UUID
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.commons.io.IOUtils
import com.datastax.driver.mapping.Result
import com.google.inject.ImplementedBy
import core.ApiErrors
import exceptions.NoSuchUUIDException
import javax.inject._
import models.extra.FlowChunkInfo
import play.api.Logger
import play.api.http.Status
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import zyber.server.CassandraMapperDelegate
import zyber.server.Permission
import zyber.server.ZyberSession
import zyber.server.ZyberUserSession
import zyber.server.dao._
import zyber.server.dao.ActivityTimeline.Action._
import zyber.server.dao.ActivityTimeline.Action
import zyber.server.dao.Principal.PrincipalType
import org.apache.tika.Tika
import core.ApiErrors
import com.datastax.driver.mapping.Mapper.{ Option => CassandraOption }
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString
import models.extra.FolderTree
import util.Util
import Util._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import actors.CreatePathReceptionist._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.util.Success
import scala.util.Failure

@ImplementedBy(classOf[FileServiceImpl])
trait FileService extends MultitenancySupport {

  val sharesFolderName = "shares"
  val homeFolderName = "home"

  val ttlResumableTransfer = 24 * 60 * 60 //24 hours ttl

  //Admin
  def adminListFiles(path: String, ord: Option[String] = None,
                     t: Option[String] = None)(implicit u: User): Option[Seq[Path]]

  def adminLocationToPath(path: String)(implicit u: User): Option[Path]

  def listSharedFolders(implicit u: User): Seq[Path]

  //Sharing
  def getSharesForPath(path: Path)(implicit u: User): Seq[PathShare]
  def resetSharing(path: Path, user: User,
                   shares: Seq[PathShare])(implicit u: User): Path

  def getRootPath(user: User, name: String)(implicit u: User): Option[Path]

  def listLinked(thePath: Path)(implicit u: User): Seq[Path]

  def linkFolder(owner: User, target: User, thePath: Path)(implicit u: User): Path

  def wipeAll()(implicit u: User) = Unit

  def restoreVersion(uuid: UUID, version: Long, user: User)(implicit u: User): Option[Path]

  def listFiles(user: User, path: String, showHidden: Boolean = false,
                ord: Option[String] = None, t: Option[String] = None)(implicit u: User): Option[Seq[Path]]

  def sortedFilesUnder(ord: Option[String], t: Option[String], containingPath: Option[Path],
                       showHidden: Boolean)(implicit u: User): Option[Seq[Path]]

  def locationToPath(user: User, path: String,
                     showHidden: Boolean)(implicit u: User): Option[Path]

  def downloadPath(uuid: UUID, user: User)(implicit u: User): Option[Path]

  def downloadPublicPath(uuid: UUID)(implicit u: User): Option[Path]

  def restrictedPath(uuid: UUID)(implicit u: User): Option[Path]

  def existingPath(uuid: UUID,
                   includeHidden: Boolean)(implicit u: User): Path

  def getPathByUUID(uuid: UUID, showHidden: Boolean = false)(implicit u: User): Option[Path]

  def getPath(parentPath: Path, path: List[String],
              onlyDirectory: Boolean = true, showHidden: Boolean)(implicit u: User): Option[Path]

  def getFileVersion(uuid: UUID, version: Long,
                     user: User)(implicit u: User): Option[FileVersion]

  def createFolder(parentPath: Path,
                   name: String)(implicit u: User): Either[ApiErrors, Path]

  def getOSPath(name: String, user: User,
                path: String)(implicit u: User): Option[HasPathOutputStream]

  def getOSPath(name: String, user: User, parentPathId: UUID)(implicit u: User): Option[HasPathOutputStream]

  def listVersions(user: User, path: Path)(implicit u: User): Seq[FileVersion]

  def listVersionsWithActivity(user: User,
                               path: Path)(implicit u: User): Seq[(FileVersion, ActivityTimeline)]

  def rename(path: Path, name: String)(implicit u: User): Either[ApiErrors, Path]

  def delete(uuid: UUID)(implicit u: User): Option[Boolean]
  def delete(path: Path)(implicit u: User): Boolean
  def restore(uuid: UUID, user: User)(implicit u: User): Path

  def destroy(uuid: UUID, user: User)(implicit u: User)

  def destroy(uuid: UUID, parent: UUID)(implicit u: User)

  def getParentName(path: Path, zus: ZyberUserSession): String

  def pathsFor(path: Path, user: User)(implicit u: User): List[String]

  def fullPath(path: Path, user: User, urlEncode: Boolean = true)(implicit u: User): String = pathsFor(path, user).map(p => if (urlEncode) URLEncoder.encode(p, java.nio.charset.StandardCharsets.UTF_8.toString()) else p).mkString("/")

  //For complete path access, not useful now that we use UUID for folder browsing
  def fullContainingPath(path: Path, user: User, urlEncode: Boolean = true)(implicit u: User): String =
    pathsFor(path, user).map(p => if (urlEncode) URLEncoder.encode(p, java.nio.charset.StandardCharsets.UTF_8.toString()) else p).reverse.tail.reverse.mkString("/")

  def getActivity(path: Path)(implicit u: User): Seq[ActivityTimeline]

  def getPathHierarchy(pathId: UUID)(implicit u: User): Either[ApiErrors, Seq[Path]]
  def getPathHierarchy(path: Path)(implicit u: User): List[Path]

  def getNamedChild(parentId: UUID, name: String)(implicit u: User): Option[Path]

  def getSharesPath()(implicit u: User): Option[Path]

  def movePath(pathId: UUID, dstId: UUID, maybeName: Option[String])(implicit u: User): Either[ApiErrors, Unit]

  def moveRenamingPaths(paths: Seq[UUID], dstPath: Path)(implicit u: User): Either[ApiErrors, Integer]

  def copyRenamingPaths(pathIds: Seq[UUID], dstPath: Path)(implicit u: User): Either[ApiErrors, Integer]

  def copyRenamingPath(path: Path, dstPath: Path)(implicit u: User): Either[ApiErrors, Integer]

  def getUUID(spath: String, view: Option[String])(implicit u: User): Either[ApiErrors, UUID]

  def updateMimetypeFor(path: Path)(implicit u: User): Unit

  def getOrCreateResumableTransfer(chunkInfo: FlowChunkInfo)(implicit u: User): ResumableTransfer

  def saveChunk(chunkInfo: FlowChunkInfo, bytes: Array[Byte])(implicit u: User): Unit

  def completeUpload(parentPath: Path, flowIdentifier: String,
                     fileName: String, addVersion: Boolean)(implicit u: User): Either[ApiErrors, HasPathOutputStream]

  def testChunk(flowChunkNumber: Int, flowIdentifier: String)(implicit u: User): Either[ApiErrors, Unit]

  def deleteFiles(pathStrings: Seq[String])(implicit u: User): Either[ApiErrors, JsObject]

  def unshareFolder(folderId: String, dstId: Option[String])(implicit u: User): Either[ApiErrors, Unit]

  def directoryTreeForHome(implicit u: User): FolderTree

  def directoryTree(path: Path)(implicit u: User): FolderTree

  def directoryTreeForShares(implicit u: User): Option[FolderTree]

  def getLocalizedFolderName(mp: Path)(implicit u: User): String

  def getDestinationPath(parentPath: Path, fileName: String,
                         relativePath: String, createDestination: Boolean = false)(implicit u: User): Either[ApiErrors, Path]
}

class FileServiceImpl @Inject() (
    val session: ZyberSession,
    val activityService: ActivityService,
    val trashService: TrashService,
    val messagesApi: MessagesApi,
    val securityService: SecurityService,
    @Named("create-folder-actor") createPathReceptionist: ActorRef)(implicit ec: ExecutionContext) extends FileService with I18nSupport with PathsCommon {
  import Status._

  def pathMapper(implicit user: User): CassandraMapperDelegate[Path] =
    userSession.mapper(classOf[Path])
  def pathAccessor(implicit user: User): PathAccessor =
    userSession.accessor(classOf[PathAccessor])

  def deletedPathAccessor(implicit user: User): PathIncludingDeletedAccessor =
    userSession.accessor(classOf[PathIncludingDeletedAccessor])

  def shareMapper(implicit user: User): CassandraMapperDelegate[PathShare] =
    userSession.mapper(classOf[PathShare])
  def shareAccessor(implicit user: User): PathShareAccessor =
    userSession.accessor(classOf[PathShareAccessor])

  def versionMapper(implicit user: User): CassandraMapperDelegate[FileVersion] =
    userSession.mapper(classOf[FileVersion])
  def versionAccessor(implicit user: User): FileVersionAccessor =
    userSession.accessor(classOf[FileVersionAccessor])

  def pathSecurityAccessor(implicit user: User) =
    userSession.accessor(classOf[PathSecurityAccessor])

  val tika = new Tika()

  override def listSharedFolders(implicit user: User): Seq[Path] = {
    /*
      This isn't very efficient, and also it assumes a global namespace of shared folders exists which isn't necessarily the case.
      However it's not too far from what we have at the moment, and it will help to figure out how an admin might want
      to view shares / activity.
      We will need to make substantial changes in this area as the permissions and folder structure develops.
     */
    shareAccessor.listAllShares().asScala.filter(_.getShareType == "users").map(_.getPathId).map(pathAccessor.getPath).toList.distinct.filter(_.isDirectory).sortBy(_.getName)
  }

  override def listLinked(thePath: Path)(implicit user: User): Seq[Path] = pathAccessor.getPathsLinked(thePath.getPathId).all().asScala

  def wipeAll(implicit user: User): Unit = {
    val rootPath: UUID = user.getRootPath(userSession).getPathId
    val children: Result[Path] = pathAccessor.getChildren(rootPath)
    children.asScala.foreach(p => {
      pathAccessor.deletePath(p.getPathId, rootPath)
    })
  }

  override def restoreVersion(path: UUID, version: Long, user: User)(implicit u: User): Option[Path] = {
    val existing = Option(pathAccessor.getPath(path))
    val matching = getFileVersion(path, version, user)
    (existing, matching) match {
      case (Some(originalFile), Some(foundVersion)) => {
        val newVersion: FileVersion = withExisting(userSession, user, originalFile, Restored, s"version ${foundVersion.getVersion}")
        val os: OutputStream = newVersion.getOutputStream(originalFile)
        IOUtils.copy(foundVersion.getInputStream(), os)
        os.close()
        Some(originalFile)
      }
      case _ => None
    }
  }

  def pathsFor(path: Path, user: User)(implicit u: User): List[String] = {
    pathsInternal(path, user, new ListBuffer[String]).toList.reverse
  }

  @tailrec
  private def pathsInternal(path: Path, user: User, addTo: ListBuffer[String])(implicit u: User): ListBuffer[String] = {
    val path1: Option[Path] = getPathByUUID(path.getParentPathId)
    path1 match {
      case None         => addTo
      case Some(parent) => pathsInternal(parent, user, addTo += path.getName)
    }
  }

  override def resetSharing(path: Path, user: User, shares: Seq[PathShare])(implicit u: User): Path = {
    shareAccessor.deleteForPath(path.getShareId)
    //Add the user of the action in
    val withUserIfNeeded = if (shares.nonEmpty && shares.forall(_.getShareType == "users")) {
      shares ++
        List(new PathShare(path.getShareId, "users", user.getUserId, path.getPathId, null, null))
    } else shares

    withUserIfNeeded.foreach(shareMapper.save)
    //At the moment we only reset the share id when revoking access
    if (withUserIfNeeded.isEmpty) {
      path.setShareId(UUID.randomUUID())
      pathMapper.save(path)
    }
    val timeline = if (shares.isEmpty) {
      new ActivityTimeline(user.getUserId, path.getPathId, new Date, Revoke.toString, "", userSession)
    } else {
      val note = shares.map(s => s"${s.getShareType} : ${Option(s.getUsername).getOrElse("public")}").mkString("(", ",", ")")
      new ActivityTimeline(user.getUserId, path.getPathId, new Date, Share.toString, note, userSession)
    }
    activityService.saveActivity(timeline)
    path
  }

  override def listFiles(user: User, pathId: String, showHidden: Boolean,
                         ord: Option[String] = None, t: Option[String] = None)(implicit u: User): Option[Seq[Path]] = {
    val containingPath: Option[Path] = getPathByUUID(UUID.fromString(pathId), false).filter(_.isDirectory())
    sortedFilesUnder(ord, t, containingPath, showHidden)
  }

  def sortedFilesUnder(ord: Option[String], t: Option[String], containingPath: Option[Path],
                       showHidden: Boolean)(implicit u: User): Option[Seq[Path]] = {
    val all: Option[Seq[Path]] = containingPath.map { p =>
      val res = access(showHidden).getChildren(p.getPathId).all().asScala
      val allSec = pathSecurityAccessor.getSecurityForParentPath(p.getPathId).all.asScala

      val secMap = allSec.foldLeft(Map[UUID, List[PathSecurity]]()) { (a, b) =>
        a.updated(b.getPathId, a.get(b.getPathId).map(b :: _).getOrElse(List(b)))
      }
      //TODO good use case for apache spark to use joins and improve this filter ?
      res.filter { x =>
        calculatePermission(secMap, x) map { permissions =>
          permissions.canFolder_View() || permissions.canFile_View() || securityService.canAccessFolder(x)
        } getOrElse { false }
      }
    }
    all.map(orderPaths(ord, t))
  }

  private def calculatePermission(secMap: Map[UUID, List[PathSecurity]], path: Path)(
    implicit user: User): Option[Permission] = {
    val gma = userSession.accessor(classOf[GroupMembersAccessor])
    //    val gma = userSession.accessor(classOf[GroupMembersFlatAccessor])
    secMap.get(path.getPathId).map { secList =>
      val ret = secList.foldLeft(0) { (a, ps) =>
        if (ps.getPrincipalType().equals(PrincipalType.User)) {
          if (ps.getPrincipalId().equals(user.getUserId)) {
            a | getPermission(ps)
          } else {
            a
          }
        } else {
          if (gma.getGroupMembers(ps.getPrincipalId).all.asScala.map(_.getMemberPrincipalId).contains(user.getUserId)) {
            a | getPermission(ps)
          } else
            a
        }
      }
      new Permission(ret)
    }
  }

  private def getPermission(ps: PathSecurity)(
    implicit user: User): Int = {
    val securityType = securityService.securityTypeCache.get(ps.getSecurityType());
    securityType.map(_.getPermission).getOrElse(0)
  }

  override def adminListFiles(path: String, ord: Option[String] = None,
                              t: Option[String] = None)(implicit u: User): Option[Seq[Path]] = {
    if (path.equals("") || path.equals("/")) {
      Some(listSharedFolders)
    } else {
      val containingPath: Option[Path] = adminLocationToPath(path)
      sortedFilesUnder(ord, t, containingPath, false)
    }
  }

  private def orderPaths(ord: Option[String], t: Option[String])(
    paths: Seq[Path])(implicit u: User): Seq[Path] = {

    val (folders, files) = paths.partition(_.isDirectory())
    val (sfolders, sfiles) = ord match {
      case Some("size") =>
        (folders.sortBy(_.getName.toLowerCase), files.sortBy(_.getSize))
      case Some("modified") =>
        (folders.sortBy(_.getName.toLowerCase), files.sortBy(_.getModifiedDate))
      case _ =>
        (folders.sortBy(_.getName.toLowerCase), files.sortBy(_.getName.toLowerCase))
    }
    t match {
      case Some("desc") => sfiles.reverse ++ sfolders.reverse
      case _            => sfolders ++ sfiles
    }
  }

  def locationToPath(user: User, path: String, showHidden: Boolean)(implicit u: User): Option[Path] = {
    val zus = userSession
    val pathLocation = path.split("/").toList.filter(!_.isEmpty)
    Logger.debug(s"List of paths: $pathLocation")
    val rootPath = singleRootPath

    getPath(rootPath, pathLocation, false, showHidden)
  }

  def adminLocationToPath(path: String)(implicit u: User): Option[Path] = {
    val pathLocation = path.split("/").toList.filter(!_.isEmpty)
    assert(pathLocation.nonEmpty)
    Logger.debug(s"List of paths: $pathLocation")
    val topLevel: Option[Path] = listSharedFolders.find(_.getName == pathLocation.head)
    getPath(topLevel.get, pathLocation.tail)
  }

  def singleRootPath(implicit u: User) = {
    val rootPath = new Path
    rootPath.setPathId(Path.ROOT_PATH_PARENT)
    rootPath.withZus(userSession)
  }

  override def listVersions(user: User, path: Path)(implicit u: User): Seq[FileVersion] = {
    versionAccessor.getAllVersions(path.getPathId).asScala.toList.sortBy(i => i.getVersion).reverse
  }

  override def listVersionsWithActivity(user: User, path: Path)(implicit u: User): Seq[(FileVersion, ActivityTimeline)] = {
    val allActivity = activityService.listActivityByPath(path.getPathId)
    listVersions(user, path).map(v => {
      (v, allActivity.find(_.getActivityTimestamp == v.getVersion).map(_.withUsername(user.getName)).getOrElse(ActivityTimeline.empty))
    })
  }

  def getActivity(path: Path)(implicit u: User): Seq[ActivityTimeline] = {
    activityService.listActivityByPath(path.getPathId).sortBy(_.getActivityTimestamp).reverse
  }

  def downloadPath(uuid: UUID, user: User)(implicit u: User): Option[Path] = {
    //Just returns getPath but records activity
    getPathByUUID(uuid).map(p => {
      activityService.saveActivity(user, p, new Date(), Viewed)
      p
    })
  }

  override def getSharesForPath(path: Path)(implicit u: User): Seq[PathShare] = shareAccessor.getShareForId(path.getShareId).all().asScala

  def existingPath(uuid: UUID, includeHidden: Boolean)(implicit u: User) = Option(access(includeHidden).getPath(uuid)).getOrElse(throw new NoSuchUUIDException(uuid))

  override def downloadPublicPath(uuid: UUID)(implicit t: User): Option[Path] = {
    shareAccessor.getShareForId(
      uuid).asScala.find(_.isPublic).map(
        p => existingPath(
          p.getPathId, false)).map(
          _.withZus(userSession))
  }

  override def restrictedPath(uuid: UUID)(implicit u: User): Option[Path] = {
    shareAccessor.getShareForId(uuid).asScala.find(a => a.getShareType == "password" || a.getShareType == "users").map(p => existingPath(p.getPathId, false)).map(_.withZus(userSession))
  }

  override def getRootPath(user: User, name: String)(implicit u: User): Option[Path] = {
    getPath(singleRootPath, List(name), false, false)
  }

  def getFileVersion(uuid: UUID, version: Long, user: User)(implicit u: User): Option[FileVersion] = {
    Option(versionAccessor.getVersion(uuid, new Date(version))).map(_.withZus(userSession))
  }

  def validFolderName(name: String): Either[ApiErrors, Unit] = {
    if (!name.matches("[^\\/:?*\"|]+"))
      Left(ApiErrors.single("Invalid folder name", Messages("invalid.name"), BAD_REQUEST))
    else Right(())
  }

  def createFolder(parentPath: Path, name: String)(implicit u: User): Either[ApiErrors, Path] = {
    try {
      validFolderName(name).right.flatMap { _ =>
        val zus = userSession
        parentPath.setZus(zus)
        if (getPath(parentPath, List(name)).isDefined)
          Left(ApiErrors.single("Folder exists", Messages("api.folder.exists"), CONFLICT))
        else {
          createFolderAwait(parentPath, name).right.map { createdDir =>
            activityService.saveActivity(u, createdDir, createdDir.getCurrentVersion, Created)
            securityService.setOwnerPermissionsFor(createdDir, u.getUserId).fold(er => Logger.error("Error: " + er.toString), identity)
            createdDir
          }
        }
      }
    } catch {
      case e: Exception =>
        Logger.error("Error creating folder: ", e)
        Left(ApiErrors.single(e.getMessage, e.getLocalizedMessage, INTERNAL_SERVER_ERROR))
    }
  }

  def getParentName(path: Path, zus: ZyberUserSession): String = {
    var parentName: String = ""
    val parentPath = getPathByUUID(path.getParentPathId, true)(zus.user)
    parentName = parentPath match {
      case Some(path) => path.getName
      case None       => zus.getRootPath.getName
    }
    return parentName
  }

  def fileDoesNotExist(parent: Path, name: String)(implicit u: User): Either[ApiErrors, Unit] = {
    if (getPath(parent, List(name), false).isDefined)
      Left(ApiErrors.single(s"File already exists: $name", Messages("api.file.exists"), CONFLICT))
    else
      Right(())
  }

  def rename(path: Path, name: String)(implicit u: User): Either[ApiErrors, Path] = {
    for {
      _ <- validFolderName(name).right
      parentPath <- getPathByUUID(path.getParentPathId).toRight(ApiErrors.single(s"Invalid file: ${path.getParentPathId}", Messages("api.invalid.path", ""), BAD_REQUEST)).right
      _ <- fileDoesNotExist(parentPath, name).right
    } yield {
      activityService.saveActivity(u, path, new Date(), Rename,
        s"Renamed from ${path.getName} to $name") //TODO what do we do here for translations ??
      path.rename(name)
      path
    }
  }

  def getPath(parentPath: Path, path: List[String],
              onlyDirectory: Boolean = true, showHidden: Boolean = false)(implicit u: User): Option[Path] = {
    path match {
      case Nil =>
        Option(parentPath)
      case name :: Nil =>
        getFromParent(parentPath, name, showHidden)
          .filter(_.isDirectory || !onlyDirectory)
      case name :: rest => {
        getFromParent(parentPath, name, showHidden).
          filter(_.isDirectory || !onlyDirectory).
          flatMap(getPath(_, rest, onlyDirectory))
      }
    }
  }

  def getOrCreateFolderPath(parentPath: Path, path: List[String],
                            showHidden: Boolean = false)(implicit u: User): Either[ApiErrors, Path] = {

    def doCreateFolder(dstPath: Path, name: String): Either[ApiErrors, Path] = {
      for {
        cf <- createFolderAwait(parentPath, name)
        _ <- securityService.setOwnerPermissionsFor(cf, u.getUserId)
      } yield {
        activityService.saveActivity(u, cf, cf.getCurrentVersion, Created)
        cf
      }
    }

    path match {
      case Nil =>
        Right(parentPath)
      case name :: Nil =>
        getFromParent(parentPath, name, showHidden) match {
          case Some(p) =>
            if (p.isDirectory()) Right(p)
            else Left(ApiErrors.single("Folder exists", Messages("api.folder.exists"), CONFLICT))
          case None => doCreateFolder(parentPath, name)
        }
      case name :: rest => {
        (getFromParent(parentPath, name, showHidden) match {
          case Some(p) =>
            if (p.isDirectory()) Right(p)
            else Left(ApiErrors.single("Folder exists", Messages("api.folder.exists"), CONFLICT))
          case None => doCreateFolder(parentPath, name)
        }).right.flatMap(getOrCreateFolderPath(_, rest))
      }
    }
  }

  override def getDestinationPath(parentPath: Path, fileName: String,
                                  relativePath: String, createDestination: Boolean = false)(implicit u: User): Either[ApiErrors, Path] = {
    val localPath = relativePath.substring(0, relativePath.lastIndexOf(fileName)).split("/").toList.filter(!_.isEmpty)
    if (createDestination)
      getOrCreateFolderPath(parentPath, localPath)
    else
      getPath(parentPath, localPath) toRight (ApiErrors.single("Cannot get path", Messages("invalid_path"), Status.NOT_FOUND))
  }

  def completeUpload(parentPath: Path, flowIdentifier: String,
                     fileName: String, addVersion: Boolean)(
                       implicit u: User): Either[ApiErrors, HasPathOutputStream] = {
    val resumableTransfer = userSession.accessor(classOf[ResumableTransferAccessor])
      .getResumable(UUID.fromString(flowIdentifier))
    if (null == resumableTransfer) {
      Left(ApiErrors.single("Invalid upload identifier", Messages("invalid_upload_id"), Status.NOT_FOUND))
    } else {
      try {
        getOSPath(fileName, u, parentPath, addVersion) match {
          case Some(os) =>
            val blocksAccessor = userSession.accessor(classOf[ResumableTransferBlockAccessor])
            Logger.debug("Chunks: " + resumableTransfer.getTotalChunks)

            for (chunkNumber <- 0 until resumableTransfer.getTotalChunks) {
              Logger.debug("Chunk number: " + (chunkNumber + 1))
              val block = blocksAccessor.getBlock(resumableTransfer.getResumableTransferId, chunkNumber + 1)
              Logger.debug("Block: " + block.getChunkSize)
              os.write(block.getChunkData.array)
            }
            os.close
            val transferMapper = userSession.mapper(classOf[ResumableTransfer])
            blocksAccessor.deleteBlocks(resumableTransfer.getResumableTransferId)
            transferMapper.delete(resumableTransfer)
            Right(os)

          case None => Left { ApiErrors.single("Cannot get path", Messages("invalid_path"), Status.NOT_FOUND) }
        }
      } catch {
        case e: Exception =>
          Logger.error("Error completing upload", e)
          Left(ApiErrors.fromException(e))
      }
    }
  }

  def getFromParent(parentPath: Path, name: String, showHidden: Boolean)(implicit u: User): Option[Path] = {
    val pathOption = Option(access(showHidden).getChildNamed(parentPath.getPathId, name).one)
    pathOption.map(p => {
      if (p.isLinked) {
        Logger.debug(s"Linked to : ${p.getLinkedId}")
        existingPath(p.getLinkedId, showHidden)
      } else p
    })
  }

  def saveChunk(chunkInfo: FlowChunkInfo, bytes: Array[Byte])(implicit u: User): Unit = {
    Logger.debug("Saving chunk")
    val rt = getOrCreateResumableTransfer(chunkInfo)
    val block =
      new ResumableTransferBlocks(rt.getResumableTransferId,
        chunkInfo.flowChunkNumber, bytes, chunkInfo.flowChunkSize);

    userSession.mapper(classOf[ResumableTransferBlocks]).save(block, CassandraOption.ttl(ttlResumableTransfer))
  }

  def testChunk(flowChunkNumber: Int, flowIdentifier: String)(implicit u: User): Either[ApiErrors, Unit] = {
    val blocksAccessor = userSession.accessor(classOf[ResumableTransferBlockAccessor])
    val block = blocksAccessor.getBlock(UUID.fromString(flowIdentifier), flowChunkNumber)
    if (null == block) {
      Left(ApiErrors.single("", "", Status.NO_CONTENT))
    } else {
      Right(())
    }
  }

  def getOrCreateResumableTransfer(chunkInfo: FlowChunkInfo)(implicit u: User): ResumableTransfer = {
    val id = UUID.fromString(chunkInfo.flowIdentifier)
    val resumableTransfer = userSession.accessor(classOf[ResumableTransferAccessor])
      .getResumable(id)
    if (null == resumableTransfer) {
      val crt = new ResumableTransfer(id,
        chunkInfo.flowFilename, chunkInfo.flowTotalChunks, chunkInfo.flowTotalSize)
      userSession.mapper(classOf[ResumableTransfer]).save(crt, CassandraOption.ttl(ttlResumableTransfer))
      crt
    } else {
      resumableTransfer
    }
  }

  private def getOSPath(name: String, user: User, parentPath: Path,
                        addVersion: Boolean = true)(implicit u: User): Option[HasPathOutputStream] = {

    def createFile(name: String): Option[HasPathOutputStream] = {
      //      val file: Path = parentPath.createFile(name)
      createFileAwait(parentPath, name) match {
        case Right(file) =>
          activityService.saveActivity(user, file, file.getCurrentVersion, Created)
          securityService.setOwnerPermissionsFor(file, user.getUserId) match {
            case Right(_)     =>
            case Left(errors) => Logger.error("Error setting owner permissions: " + errors)
          }
          Some(file.getOutputStream(file.getCurrentVersion))
        case Left(e) =>
          Logger.error("Error creating file " + e)
          None
      }
    }

    val zus = userSession
    parentPath.setZus(zus)
    val all = pathAccessor.getChildNamed(parentPath.getPathId, name).all().asScala
    val matching = all.headOption
    matching
      .flatMap { m =>
        if (addVersion) Some(streamForExisting(zus, user, m))
        else {
          val allChildren = pathAccessor.getChildren(parentPath.getPathId).all().asScala
          createFile(nextAvailableName(allChildren, m.getName))
        }
      }.orElse {
        createFile(name)
      }
  }

  def getOSPath(name: String, user: User, path: String)(implicit u: User): Option[HasPathOutputStream] = {
    val zus = userSession
    val localPath = path.split("/").toList.filter(!_.isEmpty)

    getPath(singleRootPath, localPath) match {
      case None             => None
      case Some(parentPath) => getOSPath(name, user, parentPath)
    }
  }

  def getOSPath(name: String, user: User, parentPathId: UUID)(implicit u: User): Option[HasPathOutputStream] = {
    val zus = userSession

    getPathByUUID(parentPathId).filter(_.isDirectory()) match {
      case None             => None
      case Some(parentPath) => getOSPath(name, user, parentPath)
    }
  }

  def getMimetype(path: Path): String = {
    //TODO
    //If we need a more robust mimeType detection we could use: http://stackoverflow.com/a/11283050/1502842
    //    val config = TikaConfig.getDefaultConfig();
    //    val detector = config.getDetector();
    //
    //    val stream = TikaInputStream.get(path.getInputStream);
    //
    //    val metadata = new Metadata();
    //    metadata.add(Metadata.RESOURCE_NAME_KEY, path.getName);
    //    val mediaType = detector.detect(stream, metadata);
    //
    //    val mimeType = mediaType.toString

    val mimeType = tika.detect(path.getInputStream)
    Logger.debug(s"Mimetype for: ${path.getName}, $mimeType")

    mimeType
  }

  def updateMimetypeFor(path: Path)(implicit u: User): Unit = {
    val mimeType = getMimetype(path)
    Logger.debug(s"Updating mimetype: $mimeType")
    path.setMimeType(mimeType)
    pathMapper.save(path)
  }

  override def linkFolder(owner: User, f: User, thePath: Path)(implicit u: User): Path = {
    //    val rootPath: Path = f.getRootPath(userSession)
    val rootPath = singleRootPath
    val directory: Path = rootPath.createDirectory(thePath.getName)
    directory.setLinkedId(thePath.getPathId)
    pathMapper.save(directory)
    directory
  }

  def streamForExisting(zus: ZyberUserSession, user: User, matched: Path)(implicit u: User): HasPathOutputStream = {
    val version: FileVersion = withExisting(zus, user, matched)
    version.getOutputStream(matched)
  }

  def withExisting(zus: ZyberUserSession, user: User, matched: Path,
                   action: Action = Edited, note: String = "")(implicit u: User): FileVersion = {
    val time = new Date()
    val version = new FileVersion(zus, matched.getPathId, time)
    activityService.saveActivity(user, matched, time, action, note)
    versionMapper.save(version)
    matched.setModifiedDate(time)
    pathMapper.save(matched)
    version
  }

  def delete(uuid: UUID)(implicit u: User): Option[Boolean] = {
    val maybePath = Option(pathAccessor.getPath(uuid))
    maybePath.map { path =>
      delete(path)
    }
  }

  def delete(path: Path)(implicit u: User): Boolean = {
    path.withZus(userSession)
    activityService.saveActivity(u, path, new Date(), Delete)
    trashService.saveTrashedFile(u.getUserId, path.getPathId)
    path.delete
  }

  def deleteFiles(pathStrings: Seq[String])(implicit u: User): Either[ApiErrors, JsObject] = {
    val pathsUuids = pathStrings.map(p => Try(UUID.fromString(p))).filter(_.isSuccess).map(_.get)
    val paths = pathsUuids.map(getPathByUUID(_, false)).filter(_.isDefined).map(_.get)
    val (canDelete, cannotDelete) = paths.partition { p => securityService.canDeleteFile(p).isRight }
    canDelete.foreach(delete)
    Right(JsObject(Seq(
      "deleted" ->
        JsString {
          if (canDelete.isEmpty || canDelete.size > 1)
            Messages("successfully_deleted_s", canDelete.size)
          else Messages("successfully_deleted", canDelete.head.getName)
        },
      //        "deleted" -> JsObject(Seq(Messages("successfully_deleted") ->JsNumber(canDelete.size))),
      "cannot_delete" -> JsObject(Seq(Messages("permission_cannot_delete_file") -> JsNumber(cannotDelete.size))))))
  }

  //Append a counter when restoring a file
  def nextAvailableName(all: Seq[Path], pathName: String): String = {
    def nameWithIndex(name: String, index: Int): String = {
      val li = name.lastIndexOf(".")
      if (li < 0) s"$name($index)"
      else {
        s"${name.substring(0, li)}($index)${name.substring(li)}"
      }
    }
    var loop = true
    var i = 1
    var nextName = ""
    while (loop) {
      val loopName = nameWithIndex(pathName, i)
      val exists = all.exists(_.getName == loopName)
      if (!exists) {
        nextName = loopName
        loop = false
      }
      i = i + 1
    }
    nextName
  }

  def restore(uuid: UUID, user: User)(implicit u: User): Path = {
    val path = existingPath(uuid, true).withZus(userSession)
    val allChildren = pathAccessor.getChildren(path.getParentPathId).all().asScala
    val exists = allChildren.exists(_.getName == path.getName)
    if (!exists) {
      path.restore()
    } else {
      path.restore()
      path.rename(nextAvailableName(allChildren, path.getName))
    }
    activityService.saveActivity(user, path, new Date(), Restored)
    trashService.restoreFromTrash(user.getUserId, path.getPathId)
    path
  }

  def destroy(uuid: UUID, user: User)(implicit u: User): Unit = {
    pathAccessor.deletePath(user.getHomeFolder, uuid)
  }

  def destroy(uuid: UUID, parent: UUID)(implicit u: User): Unit = {
    pathAccessor.deletePath(parent, uuid)
  }

  override def movePath(pathId: UUID, dstId: UUID, maybeName: Option[String])(implicit u: User): Either[ApiErrors, Unit] = {
    val res = for {
      path <- getPathByIdAsEither(pathId).right
      parentPath <- getPathByIdAsEither(dstId).right
      _ <- fileDoesNotExist(parentPath, maybeName.getOrElse(path.getName)).right
      _ <- maybeName.filter(n => !n.equals(path.getName)).map(n => rename(path, n)).getOrElse(Right(())).right
      _ <- updateParentPathSecurity(path, parentPath).right
      _ <- doMovePath(path, dstId).right
    } yield {}
    res
  }

  def moveRenamingPaths(pathIds: Seq[UUID], dstPath: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    val paths = pathIds.map(p => getPathByUUID(p, false)).filter(_.isDefined).map(_.get)
    paths.foldLeft[Either[ApiErrors, Integer]](Right(0)) { (a, b) =>
      for {
        cc <- a
        _ <- moveRenamingPath(b, dstPath)
      } yield {
        cc + 1
      }
    }
  }

  def moveRenamingPath(path: Path, dstPath: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    def doMoveRenamingPath(path: Path, dst: Path): Either[ApiErrors, Boolean] = {
      if (!path.getParentPathId.equals(dst.getPathId)) {
        val child = getNamedChild(dst.getPathId, path.getName)
        for {
          _ <- updateParentPathSecurity(path, dst)
        } yield {
          updateAccessSubfolder(path, dst)
          path.setZus(userSession)
          path.move(dst.getPathId)
          if (child.isDefined) {
            val allChildren = pathAccessor.getChildren(dst.getPathId).all().asScala
            val newName = nextAvailableName(allChildren, path.getName)
            path.rename(newName)
          }
          true
        }
      } else {
        Right(false)
      }
    }

    for {
      _ <- securityService.canMove(path)
      _ <- canCopyMoveInto(path, dstPath)
      origParentPathId = path.getParentPathId
      moved <- doMoveRenamingPath(path, dstPath)
    } yield {
      if (moved) {
        val pp = pathAccessor.getPath(origParentPathId)
        val name = if (pp.getPathId.equals(u.getHomeFolder)) "Home" else pp.getName
        activityService.saveActivity(u, path, new Date(), Moved,
          s"Moved from ${name}")
      }
    }
  }

  def canCopyMoveInto(path: Path, dst: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    val ph = getPathHierarchy(dst, List())
    if (ph.exists { p => path.getPathId.equals(p.getPathId) }) {
      Left(ApiErrors.single("operation not allowed", Messages("operation_not_allowed"), Status.BAD_REQUEST))
    } else {
      Right(())
    }
  }

  def copyRenamingPaths(pathIds: Seq[UUID], dstPath: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    val paths = pathIds.map(p => getPathByUUID(p, false)).filter(_.isDefined).map(_.get)

    paths.foldLeft[Either[ApiErrors, Integer]](Right(0)) { (a, b) =>
      for {
        c <- a
        ni <- copyRenamingPath(b, dstPath)
      } yield {
        c + ni
      }
    }
  }

  def copyRenamingPath(path: Path, dstPath: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    for {
      _ <- canCopyMoveInto(path, dstPath)
      c <- copyPath(path, dstPath)
    } yield {
      c
    }
  }

  def copyPath(path: Path, dst: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    if (path.isDirectory) copyFolder(path, dst)
    else copyFile(path, dst).right.map(_ => 1)
  }

  def copyFile(path: Path, dstPath: Path)(implicit u: User): Either[ApiErrors, Path] = {
    def doCopyRenaming(path: Path, dstPath: Path): Path = {
      val child = getNamedChild(dstPath.getPathId, path.getName)
      val copiedPath = path.copy(dstPath, userSession)
      if (child.isDefined) {
        val allChildren = pathAccessor.getChildren(dstPath.getPathId).all().asScala
        val newName = nextAvailableName(allChildren, path.getName)
        copiedPath.rename(newName)
      }
      copiedPath
    }

    for {
      _ <- securityService.canViewFile(path)
      //      _ <- canMoveInto(path, dstPath)
      //      origParentPathId = path.
      copiedPath = doCopyRenaming(path, dstPath)
      //      _ <- securityService.copyPermissionsFor(path, copiedPath)
      _ <- securityService.setOwnerPermissionsFor(copiedPath, u.getUserId)
    } yield {
      activityService.saveActivity(u, copiedPath, new Date(), Copied,
        s"File copied from ${path.getName}")
      copiedPath
    }
  }

  def copyFolder(path: Path, dstPath: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    for {
      _ <- securityService.canViewFolder(path)
      //      _ <- canMoveInto(path, dstPath)
      createdFolder <- createFolderWithAvailableName(path.getName, dstPath)
      _ <- securityService.setOwnerPermissionsFor(createdFolder, u.getUserId)
      cf <- copyFolderContent(path, createdFolder)
    } yield {
      activityService.saveActivity(u, createdFolder, new Date(), Copied,
        s"Folder copied from ${path.getName}")
      cf
    }
  }

  def createFolderWithAvailableName(pathName: String, dstPath: Path)(implicit u: User): Either[ApiErrors, Path] = {
    val child = getNamedChild(dstPath.getPathId, pathName)
    val name: String = if (child.isDefined) {
      val allChildren = pathAccessor.getChildren(dstPath.getPathId).all().asScala
      nextAvailableName(allChildren, pathName)
    } else pathName
    dstPath.setZus(userSession)
    //val createdDir = dstPath.createDirectory(name)
    //TODO do not await future
    createFolderAwait(dstPath, name)
  }

  def copyFolderContent(folder: Path, dst: Path)(implicit u: User): Either[ApiErrors, Integer] = {
    val children = pathAccessor.getChildren(folder.getPathId).all.asScala
    children.foldLeft[Either[ApiErrors, Integer]](Right(0)) { (a, b) =>
      for {
        c <- a
        fc <- copyPath(b, dst)
      } yield {
        c + fc
      }
    }
  }

  private def getPathAsEither(pathId: String)(implicit u: User): Either[ApiErrors, Path] = {
    for {
      pathId <- getUUID(pathId).right
      path <- getPathByIdAsEither(pathId).right
    } yield {
      path
    }
  }

  private def getPathByIdAsEither(pathId: UUID)(implicit u: User) = {
    getPathByUUID(pathId).toRight(invalidPath(pathId.toString))
  }

  override def unshareFolder(folderId: String, dstId: Option[String])(implicit u: User): Either[ApiErrors, Unit] = {
    import Util._
    for {
      path <- getPathAsEither(folderId)
      parentPath <- getPathAsEither(dstId.getOrElse(u.getHomeFolder.toString))
      _ <- securityService.canRemoveAccessToFolder(path)
      _ <- movePath(path.getPathId, parentPath.getPathId, None)
      principalsToRemove = securityService.getPrincipalsForPath(path.getPathId)
        .map(_.getPrincipalId)
        .filter(!_.equals(u.getUserId))
      _ <- securityService.removePrincipalsFromPath(path.getPathId, principalsToRemove)
      _ <- securityService.setOwnerPermissionsFor(path, u.getUserId, true)
    } yield {}
  }

  def directoryTreeForHome(implicit u: User): FolderTree = {
    val rootPath = u.getRootPath(userSession)
    rootPath.setName(Messages("home"))
    directoryTree(rootPath)
  }

  def directoryTreeForShares(implicit u: User): Option[FolderTree] = {
    getSharesPath.flatMap { shares =>
      filterFoldersWithAddPermission(directoryTree(shares))
    }
  }

  //TODO make it tailrec
  private def filterFoldersWithAddPermission(ft: FolderTree)(implicit u: User): Option[FolderTree] = {
    if (userSession.calculatePermission(ft.path, securityService.securityTypeCacheJava).canFolder_Add()) {
      Some(ft.copy(folders = ft.folders.map(filterFoldersWithAddPermission).filter(_.isDefined).map(_.get)))
    } else
      None
  }

  def directoryTree(path: Path)(implicit u: User): FolderTree = {
    //TODO make it tailrec
    def getTree(path: Path): FolderTree = {
      val children = pathAccessor.getChildren(path.getPathId).all().asScala.filter(_.isDirectory())
      FolderTree(path, children.map(getTree))
    }
    getTree(path)
  }

  private def updateParentPathSecurity(path: Path, newParent: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    Right {
      val psMapper = userSession.mapper(classOf[PathSecurity])
      pathSecurityAccessor.getSecurityForPath(path.getParentPathId, path.getPathId).all.asScala.foreach { ps =>
        psMapper.delete(ps)
        ps.setParentPathId(newParent.getPathId)
        psMapper.save(ps)
      }
    }
  }

  //TODO pretty inefficient 
  def updateAccessSubfolder(path: Path, newParent: Path)(implicit u: User): Unit = {
    val us = userSession
    val asa = us.accessor(classOf[AccessSubfoldersAccessor])
    val asm = us.mapper(classOf[AccessSubfolders])

    def doUpdateAccessSubfolder(p: Path): Unit = {
      //      val children = pathAccessor.getChildren(p.getPathId).all.asScala
      val children = pathAccessor.getChildrenByType(p.getPathId, PathType.Directory).all
      val it = children.iterator
      while (it.hasNext) {
        val child = it.next
        if (child.isDirectory()) {
          doUpdateAccessSubfolder(child)
          val accessIt = asa.getAcess(path.getParentPathId, child.getPathId).all.iterator
          while (accessIt.hasNext) {
            val a = accessIt.next
            asm.delete(a)
            a.setParentPathId(newParent.getPathId)
            asm.save(a)
          }
        }
      }
    }

    doUpdateAccessSubfolder(path)
  }

  private def doMovePath(path: Path, dstId: UUID)(implicit u: User) = {
    try {
      Right {
        path.setZus(userSession)
        path.move(dstId)
      }
    } catch {
      case e: Exception => Left(ApiErrors.single(e.getMessage, Messages("internal_error"), INTERNAL_SERVER_ERROR))
    }
  }

  def invalidPath(spath: String) = ApiErrors.single("invalid path",
    Messages("api.invalid.path", spath), NOT_FOUND)

  def getUUID(spath: String, view: Option[String] = None)(implicit u: User): Either[ApiErrors, UUID] = {
    Logger.debug("View: " + view)
    (spath, view) match {
      case (spath, Some(`sharesFolderName`)) if spath.trim.isEmpty() =>
        getNamedChild(
          Path.ROOT_PATH_PARENT, Path.SHARES_FOLDER)
          .map(_.getPathId)
          .toRight(ApiErrors.single("Share folder does not exists", Messages("shares_not_found"), NOT_FOUND))
      case (spath, Some(`homeFolderName`)) if spath.trim.isEmpty() => Right { u.getHomeFolder }
      case (spath, _) if !spath.isEmpty() => {
        Try(UUID.fromString(spath)) match {
          case Failure(e) =>
            Logger.error("Error creating UUID", e)
            Left(ApiErrors.single("Invalid UUID", Messages("invalid_path"), BAD_REQUEST))
          case Success(uuid) => Right(uuid)
        }
      }
      case _ => Left(ApiErrors.single(
          "Invalid parameters: you need to specify either the pathID or view name (home, shares)", 
          Messages("internal_error"), BAD_REQUEST))
    }
  }

  implicit val timeout: Timeout = 10.seconds

  def doCreatePathAsync(dstPath: Path, name: String, pathType: PathType)(implicit u: User): Future[Either[ApiErrors, Path]] = {
    dstPath.setZus(userSession)
    //    (createFolderActor ? CreateDirectory(dstPath, name)).mapTo[Done].map(_.createdPath)

    (createPathReceptionist ? CreatePath(dstPath, name, pathType)).map {
      case Done(createdPath) => Right(createdPath)
      case Error(e)          => Left(ApiErrors.fromException(e))
    }
  }

  def createFolderAsync(dstPath: Path, name: String)(implicit u: User): Future[Either[ApiErrors, Path]] = {
    doCreatePathAsync(dstPath, name, PathType.Directory)
  }

  def createFolderAwait(dstPath: Path, pathName: String)(implicit u: User): Either[ApiErrors, Path] = {
    Await.ready(createFolderAsync(dstPath, pathName), Duration.Inf).value.get match {
      case Success(createdDir) => createdDir
      case Failure(e)          => Left(ApiErrors.fromException(e))
    }
  }

  def createFileAsync(dstPath: Path, name: String)(implicit u: User): Future[Either[ApiErrors, Path]] = {
    doCreatePathAsync(dstPath, name, PathType.File)
  }

  def createFileAwait(dstPath: Path, pathName: String)(implicit u: User): Either[ApiErrors, Path] = {
    Await.ready(createFileAsync(dstPath, pathName), Duration.Inf).value.get match {
      case Success(createdFile) => createdFile
      case Failure(e)           => Left(ApiErrors.fromException(e))
    }
  }

}

