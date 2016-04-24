package services

import com.google.inject.ImplementedBy
import java.util.UUID
import zyber.server.dao.Principal
import models.JPermissions
import zyber.server.ZyberSession
import javax.inject.Inject
import zyber.server.dao.User
import zyber.server.dao.PathSecurityAccessor
import scala.collection.JavaConverters._
import zyber.server.dao.PrincipalAccessor
import zyber.server.Permission
import zyber.server.Permissions
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.i18n.Messages
import zyber.server.dao.PathSecurity
import models.JPermissions
import models.JPrincipal
import zyber.server.dao.GroupAccessor
import zyber.server.dao.Path
import zyber.server.dao.PathType
import zyber.server.dao.PathAccessor
import play.api.Logger
import core.ApiErrors
import play.api.http.Status
import models.PermissionSets
import play.api.cache.CacheApi
import zyber.server.dao.SecurityType
import zyber.server.dao.SecurityTypeAccessor
import zyber.server.dao.UserRole
import zyber.server.Abilities
import zyber.server.dao.UserRoleAccessor
import zyber.server.dao.ActivityTimeline
import java.util.Date
import zyber.server.dao.PathIncludingDeletedAccessor
import zyber.server.dao.AccessSubfolders
import zyber.server.dao.AccessSubfoldersAccessor
import zyber.server.dao.GroupMembersFlatAccessor
import zyber.server.dao.GroupMembersAccessor

@ImplementedBy(classOf[SecurityServiceImp])
trait SecurityService {

  def getPrincipalsForPath(pathId: UUID)(implicit user: User): Seq[Principal]

  def getPermissionSetForPrincipal(pathId: UUID, princId: UUID)(implicit user: User): Option[JPermissions]

  def addPrincipalToPath(pathId: UUID, princName: String, doRecursively: Boolean)(implicit user: User): Either[ApiErrors, Principal]

  def addPrincipalToPath(pathId: UUID, principal: Principal, doRecursively: Boolean)(implicit user: User): Either[ApiErrors, Principal]

  def addPrincipalToPath(pathId: UUID, princId: UUID, doRecursively: Boolean = true, saveActivity: Boolean = true)(implicit user: User): Either[ApiErrors, Principal]

  def removePrincipalFromPath(pathId: UUID, princId: UUID, setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit]

  def removePrincipalsFromPath(pathId: UUID, principals: Seq[UUID])(implicit user: User): Either[ApiErrors, Unit]

  def setOwnerPermissionsFor(path: Path, princId: UUID, 
      setRecursively: Boolean = false, saveActivity: Boolean = false)(implicit u: User): Either[ApiErrors, Unit]

  def getPermisionSets(implicit user: User): Seq[JPermissions]

  def setPermissionSetForPrincipal(pathId: UUID, princId: UUID,
                                   permission: JPermissions, setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit]

  def canViewFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canViewOrAccessFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canMoveFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canMove(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canViewFile(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canAddToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canRemoveFromFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canDeleteFile(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canRenameFile(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canModifyFile(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canViewFileHistory(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canRestoreFile(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def canViewPermissions(path: Path)(implicit u: User): Either[ApiErrors, Unit]
  def canAllowAccessToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]
  def canRemoveAccessToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit]

  def securityTypeCacheJava(implicit user: User): java.util.Map[UUID, SecurityType]

  def securityTypeCache(implicit user: User): Map[UUID, SecurityType]

  def setPermissionsForShares(princId: UUID)(implicit u: User): Either[ApiErrors, Unit]

  def describePermissionSet(permissionId: UUID)(implicit user: User): Seq[String]

  def getPermissionFor(role: String)(implicit u: User): Option[JPermissions]

  def canAccessFolder(path: Path)(implicit u: User): Boolean

  def inheritPermissionsFor(path: Path, princId: UUID)(implicit u: User): Either[ApiErrors, Unit]

  def copyPermissionsFor(originPath: Path, destPath: Path /*,princId: UUID*/ )(implicit u: User): Either[ApiErrors, Unit]

  def getOwnerPermissions(implicit u: User): JPermissions
}

class SecurityServiceImp @Inject() (
  val session: ZyberSession,
  val messagesApi: MessagesApi,
  val loginService: LoginService,
  cache: CacheApi,
  activityService: ActivityService) extends SecurityService
    with MultitenancySupport
    with I18nSupport
    with PathsCommon {

  def pathSecurityAccessor(implicit user: User) =
    userSession.accessor(classOf[PathSecurityAccessor])

  def pathSecurityMapper(implicit user: User) =
    userSession.mapper(classOf[PathSecurity])

  def principalAccessor(implicit user: User) =
    userSession.accessor(classOf[PrincipalAccessor])

  def groupAccessor(implicit user: User): GroupAccessor =
    userSession.accessor(classOf[GroupAccessor])

  def pathAccessor(implicit user: User): PathAccessor =
    userSession.accessor(classOf[PathAccessor])

  def deletedPathAccessor(implicit user: User): PathIncludingDeletedAccessor =
    userSession.accessor(classOf[PathIncludingDeletedAccessor])

  def getPrincipalsForPath(pathId: UUID)(implicit user: User): Seq[Principal] = {
    val path = pathAccessor.getPath(pathId)
    if (null != path) {
      val ps = pathSecurityAccessor.getSecurityForPath(path.getParentPathId, pathId).all().asScala

      val principalIds = ps map (_.getPrincipalId)

      val principals = principalAccessor.getPrincipaById(principalIds.asJava).all.asScala
      principals
    } else
      Seq()
  }

  def getPermissionSetForPrincipal(pathId: UUID, princId: UUID)(implicit user: User): Option[JPermissions] = {
    val path = pathAccessor.getPath(pathId)
    val princPerm = Option(pathSecurityAccessor.getSecurityForPrincipal(path.getParentPathId, pathId, princId))
    princPerm.flatMap { ps =>
      val mst = securityTypeCache.get(ps.getSecurityType)
      mst.map { st =>
        JPermissions(
          st.getName,
          st.getSecurityTypeId,
          false,
          Messages(st.getName))
      }
    }
  }

  def getPermisionSets(implicit user: User): Seq[JPermissions] = {
    securityTypeCache.values.toList.sortBy(_.getName).map { st =>
      JPermissions(
        st.getName,
        st.getSecurityTypeId,
        false,
        Messages(st.getName),
        describePermissionSet(st.getSecurityTypeId).map(Messages(_)))
    } toSeq
  }

  private def optionPrincToEither(princ: Option[Principal]): Either[ApiErrors, Principal] = {
    princ.toRight(ApiErrors.single("Invalid principal", Messages("invalid_principal"), Status.BAD_REQUEST))
  }

  def addPrincipalToPath(pathId: UUID, princName: String, doRecursively: Boolean)(implicit user: User): Either[ApiErrors, Principal] = {
    val principal: Either[String, Principal] = getPrincipalByName(princName).toRight(Messages("invalid_principal"))
    addPrincipalToPathRecursively(pathId, optionPrincToEither(getPrincipalByName(princName)), doRecursively)
  }

  def addPrincipalToPath(pathId: UUID, princId: UUID, doRecursively: Boolean = true, saveActivity: Boolean = true)(implicit user: User): Either[ApiErrors, Principal] = {
    Logger.debug("Save activity addPrincipalToPath: " + saveActivity)

    addPrincipalToPathRecursively(pathId, optionPrincToEither(Option(principalAccessor.getPrincipaById(princId))), doRecursively, saveActivity)
  }

  def addPrincipalToPath(pathId: UUID, principal: Principal, doRecursively: Boolean)(implicit user: User): Either[ApiErrors, Principal] = {
    addPrincipalToPathRecursively(pathId, Right(principal), doRecursively)
  }

  def addPrincipalToPathRecursively(pathId: UUID, princ: Either[ApiErrors, Principal],
                                    doRecursively: Boolean = true, saveActivity: Boolean = true)(implicit user: User): Either[ApiErrors, Principal] = {
    doAddPrincipalToPath(pathId, princ, saveActivity) match {
      case Right(principal) if doRecursively =>
        val children = pathAccessor.getChildren(pathId).all().asScala
        addPrincipalToPathChildren(children, princ, saveActivity)
      case Right(principal) => Right(principal)
      case Left(errors)     => Left(errors)

    }
  }

  def addPrincipalToPathChildren(children: Seq[Path], princ: Either[ApiErrors, Principal],
                                 saveActivity: Boolean = true)(implicit user: User): Either[ApiErrors, Principal] = {
    Logger.debug("Save activity addPrincipalToPathChildren: " + saveActivity)

    val initial: Either[ApiErrors, Principal] = princ
    children.foldLeft(initial) {
      (resp, path) =>
        doAddPrincipalToPath(path.getPathId, princ, saveActivity) match {
          case Right(principal) => {
            if (path.isDirectory()) {
              val children = pathAccessor.getChildren(path.getPathId).all().asScala
              addPrincipalToPathChildren(children, princ, saveActivity)
            } else {
              Right(principal)
            }
          }
          case Left(errors) =>
            if (resp.isRight) Left(errors)
            else
              Left(resp.left.get ++ errors)
        }
    }
  }

  def doAddPrincipalToPath(pathId: UUID, princ: Either[ApiErrors, Principal], saveActivity: Boolean = true)(implicit user: User): Either[ApiErrors, Principal] = {
    val path = pathAccessor.getPath(pathId)
    def addPrincipal(principal: Principal): Either[ApiErrors, PathSecurity] = {
      val ps = pathSecurityAccessor.getSecurityForPrincipal(path.getParentPathId, pathId, principal.getPrincipalId)
      if (null != ps) {
        Right(ps)
      } else {
        Right {
          val pathSec = new PathSecurity(pathId, path.getParentPathId, principal.getPrincipalId, principal.getType)
          pathSecurityMapper.save(pathSec)
          allowAccessToFolder(principal, path)
          pathSec
        }
      }
    }

    try {
      for {
        principal <- princ.right
        _ <- addPrincipal(principal).right
        _ <- doSetPermissionSetForPrincipal(pathId, principal.getPrincipalId, getViewerPermissions).right //We add it as viewer by default
      } yield {
        Logger.debug("Save activity: " + saveActivity)
        if (saveActivity) {
          val note = s"Shared with ${principal.getDisplayName}"
          activityService.saveActivity(user, pathId, new Date(), ActivityTimeline.Action.Share, note)
        }
        principal
      }
    } catch {
      case e: Exception => Left(ApiErrors.single(e.getMessage, Messages("internal_error"), Status.INTERNAL_SERVER_ERROR))
    }
  }

  private def allowAccessToFolder(principal: Principal, path: Path)(implicit user: User) = {
    val hierarchy = getPathHierarchy(path, List()).tail
    val accessMapper = userSession.mapper(classOf[AccessSubfolders])
    hierarchy.foreach { p =>
      if (!p.getPathId.equals(path.getPathId)) {
        val access = new AccessSubfolders(p.getPathId, path.getPathId, principal.getPrincipalId)
        accessMapper.save(access)
      }
    }
  }

  def getPrincipalByName(name: String)(implicit user: User): Option[Principal] = {
    val princId =
      loginService.getUser(name)(user.getTenantId).
        map(_.getUserId) orElse {
          Option(groupAccessor.getGroupByName(name)).
            map(_.getGroupId)
        }
    princId.flatMap { id => Option(principalAccessor.getPrincipaById(id)) }
  }

  def setPermissionSetForPrincipal(pathId: UUID, princId: UUID,
                                   permission: JPermissions, setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit] = {
    setPermissionSetForPrincipalRecursively(pathId, princId, permission, setRecursively)
  }

  def describePermissionSet(permissionId: UUID)(implicit user: User): Seq[String] = {
    val st = userSession.accessor(classOf[SecurityTypeAccessor]).getSecurityType(permissionId)
    val p = new Permission(st.getPermission)
    Permissions.values().filter(p.can).map(_.name)
  }

  def setPermissionSetForPrincipalRecursively(pathId: UUID, princId: UUID,
                                              permission: JPermissions, setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit] = {
    doSetPermissionSetForPrincipal(pathId, princId, permission) match {
      case Left(errors) => Left(errors)
      case Right(_) if setRecursively =>
        val initial: Either[ApiErrors, Unit] = Right(())
        pathAccessor.getChildren(pathId).all.asScala.foldLeft(initial) {
          (resp, path) =>
            val childResp = if (path.isDirectory()) {
              setPermissionSetForPrincipalRecursively(path.getPathId, princId, permission)
            } else {
              doSetPermissionSetForPrincipal(path.getPathId, princId, permission)
            }
            (resp, childResp) match {
              case (Right(_), Right(_))           => Right(())
              case (Right(_), Left(errors))       => Left(errors)
              case (Left(errors), Right(_))       => Left(errors)
              case (Left(errors1), Left(errors2)) => Left(errors1 ++ errors2)
            }
        }
      case Right(_) => Right(())
    }
  }

  def doSetPermissionSetForPrincipal(pathId: UUID, princId: UUID,
                                     permission: JPermissions)(implicit user: User): Either[ApiErrors, Unit] = {
    try {
      val path = pathAccessor.getPath(pathId)
      val ps = pathSecurityAccessor.getSecurityForPrincipal(path.getParentPathId, pathId, princId)
      if (null == ps) {
        //        Left(ApiErrors.single("Permission not found", Messages("invalid_path_permission"), Status.NOT_FOUND))
        Right(())
      } else Right {
        ps.setSecurityType(permission.uuid)
        pathSecurityMapper.save(ps)
      }
    } catch {
      case e: Exception => Left(ApiErrors.single(e.getMessage, e.getMessage, Status.INTERNAL_SERVER_ERROR))
    }
  }

  def removePrincipalsFromPath(pathId: UUID, principals: Seq[UUID])(implicit user: User): Either[ApiErrors, Unit] = {
    Right {
      principals.foreach { pid => removePrincipalFromPath(pathId, pid) }
    }
  }

  def removePrincipalFromPath(pathId: UUID, princId: UUID,
                              setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit] = {
    removePrincipalFromPathRecursively(pathId, princId, setRecursively)
  }

  def removePrincipalFromPathRecursively(pathId: UUID, princId: UUID,
                                         setRecursively: Boolean = true)(implicit user: User): Either[ApiErrors, Unit] = {
    val initial: Either[ApiErrors, Unit] = Right(())

    lazy val result = pathAccessor.getChildren(pathId).all.asScala.foldLeft(initial) {
      (resp, path) =>
        val childResp = if (path.isDirectory())
          removePrincipalFromPathRecursively(path.getPathId, princId, setRecursively)
        else
          doRemovePrincipalFromPath(path.getPathId, princId)
        (resp, childResp) match {
          case (Right(_), Right(_))           => Right(())
          case (Right(_), Left(errors))       => Left(errors)
          case (Left(errors), Right(_))       => Left(errors)
          case (Left(errors1), Left(errors2)) => Left(errors1 ++ errors2)
        }
    }
    for {
      _ <- doRemovePrincipalFromPath(pathId, princId).right
      //      _ <- result.right
    } yield {
      if (setRecursively) result
      ()
    }
  }

  def doRemovePrincipalFromPath(pathId: UUID, princId: UUID)(implicit user: User): Either[ApiErrors, Unit] = {
    val path = pathAccessor.getPath(pathId)
    try {
      Right {
        (for {
          principal <- Option(principalAccessor.getPrincipaById(princId))
          ps <- Option(pathSecurityAccessor.getSecurityForPrincipal(path.getParentPathId, pathId, princId))
        } yield {
          val note = s"Share removed for ${principal.getDisplayName}"
          activityService.saveActivity(user, pathId, new Date(), ActivityTimeline.Action.Share, note)
          pathSecurityMapper.delete(ps)
          removeAccessToFolder(principal, path)
        }) getOrElse (())
      }
    } catch {
      case e: Exception => Left(ApiErrors.single(e.getMessage, Messages("internal_error"), Status.NOT_FOUND))
    }
  }

  private def removeAccessToFolder(principal: Principal, path: Path)(implicit user: User) = {
    val hierarchy = getPathHierarchy(path, List()).tail
    val accessMapper = userSession.mapper(classOf[AccessSubfolders])
    hierarchy.foreach { p =>
      if (!p.getPathId.equals(path.getPathId)) {
        val access = new AccessSubfolders(p.getPathId, path.getPathId, principal.getPrincipalId)
        accessMapper.delete(access)
      }
    }
  }

  def setOwnerPermissionsFor(path: Path, princId: UUID, 
      setRecursively: Boolean = false, saveActivity: Boolean = false)(implicit u: User): Either[ApiErrors, Unit] = {
    Logger.debug("Setting default permissions for:" + path.getName)
    val permissions = getOwnerPermissions
    Logger.debug("Permissions: " + permissions)
    for {
      _ <- addPrincipalToPath(path.getPathId, princId, setRecursively, saveActivity).right
      _ <- setPermissionSetForPrincipal(path.getPathId, princId, permissions, setRecursively).right
      _ <- inheritPermissionsFor(path, princId).right
      //      _ <- setPermissionsForShares(princId, permissions).right
    } yield {}
  }

  def getOwnerPermissions(implicit u: User): JPermissions = {
    getPermissionFor("owner") get
  }

  def getViewerPermissions(implicit u: User): JPermissions = {
    getPermissionFor("viewer") get
  }

  def getPermissionFor(role: String)(implicit u: User): Option[JPermissions] = {
    securityTypeCache
      .find { case (uuid, st) => st.getName.equals(role) }
      .map {
        case (uuid, st) =>
          JPermissions(
            st.getName,
            st.getSecurityTypeId,
            false,
            Messages(st.getName))
      }
  }

  def setPermissionsForShares(princId: UUID)(implicit u: User): Either[ApiErrors, Unit] = {
    setPermissionsForShares(princId, getOwnerPermissions)
  }

  def setPermissionsForShares(princId: UUID, permissions: JPermissions)(implicit u: User): Either[ApiErrors, Unit] = {
    val sp = Option(pathAccessor.getChildNamed(Path.ROOT_PATH_PARENT, Path.SHARES_FOLDER).one())
    val principal = optionPrincToEither(Option(principalAccessor.getPrincipaById(princId)))
    for {
      sharesPath <- sp.toRight(ApiErrors.single("Shares not found", Messages("shares_not_found"), Status.NOT_FOUND)).right
      _ <- doAddPrincipalToPath(sharesPath.getPathId, principal, false).right
      _ <- doSetPermissionSetForPrincipal(sharesPath.getPathId, princId, permissions).right
    } yield {}
  }

  def inheritPermissionsFor(path: Path, princId: UUID)(implicit u: User): Either[ApiErrors, Unit] = {
    Logger.debug("Inheriting permissions for: " + path.getName)
    val sp = Option(pathAccessor.getChildNamed(Path.ROOT_PATH_PARENT, Path.SHARES_FOLDER).one())
    if (sp.filter(p => p.getPathId.equals(path.getParentPathId)).isDefined) { //Path.ROOT_PATH_PARENT.equals(path.getParentPathId)
      Right(())
    } else {
      try {
        val parentPath = pathAccessor.getPath(path.getParentPathId)
        val secPath = pathSecurityAccessor.getSecurityForPath(parentPath.getParentPathId, path.getParentPathId).all.asScala
        Logger.debug("Security path to inherit: " + secPath.size)
        secPath.foreach { cp =>
          if (!cp.getPrincipalId.equals(princId)) {
            cp.setParentPathId(path.getParentPathId)
            cp.setPathId(path.getPathId)
            pathSecurityMapper.save(cp)
          }
        }
        Right(())
      } catch {
        case e: Exception => Left(
          ApiErrors.single("Cannot set default settings for path", Messages("internal_error"), Status.INTERNAL_SERVER_ERROR))
      }
    }
  }

  def copyPermissionsFor(originPath: Path, destPath: Path /*,princId: UUID*/ )(implicit u: User): Either[ApiErrors, Unit] = {
    Logger.debug("Copying permissions for: " + originPath.getName)
    try {
      val parentPath = pathAccessor.getPath(originPath.getParentPathId)
      val secPath = pathSecurityAccessor.getSecurityForPath(originPath.getParentPathId, originPath.getPathId).all.asScala
      Logger.debug("Security path to inherit: " + secPath.size)
      Right {
        secPath.foreach { cp =>
          //          if (!cp.getPrincipalId.equals(princId)) {
          cp.setParentPathId(destPath.getParentPathId)
          cp.setPathId(destPath.getPathId)
          pathSecurityMapper.save(cp)
          //          }
        }
      }
    } catch {
      case e: Exception => Left(
        ApiErrors.single("Cannot set default settings for path", Messages("internal_error"), Status.INTERNAL_SERVER_ERROR))
    }

  }

  def canViewOrAccessFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    if (canAccessFolder(path)) {
      Right(())
    } else {
      canViewFolder(path)
    }
  }

  def canViewFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, path.isDirectory() && userSession.calculatePermission(path, securityTypeCacheJava).canFolder_View(),
      "User cannot list files", Messages("permission_cannot_view_folder", path.getName))
  }

  def canMoveFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, path.isDirectory() && userSession.calculatePermission(path, securityTypeCacheJava).canFolder_Move(),
      "User cannot move folder", Messages("permission_cannot_move_folder", path.getName))
  }

  def canMoveFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, path.isFile() && userSession.calculatePermission(path, securityTypeCacheJava).canFile_Move(),
      "User cannot move folder", Messages("permission_cannot_move_folder", path.getName))
  }

  def canMove(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    val p = userSession.calculatePermission(path, securityTypeCacheJava)
    canOnPath(path, (path.isFile() && p.canFile_Move()) || (path.isDirectory() && p.canFolder_Move()),
      "User cannot move selected file", Messages("permission_cannot_move_folder", path.getName))
  }

  def canViewFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFile_View(),
      "User cannot download file", Messages("permission_cannot_view_file", path.getName))
  }

  def canAddToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, path.isDirectory() && userSession.calculatePermission(path, securityTypeCacheJava).canFolder_Add(),
      "User cannot add files to folder", Messages("permission_cannot_add_folder", path.getName))
  }

  def canRemoveFromFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFolder_Remove(),
      "User cannot remove files from folder", Messages("permission_cannot_remove_folder", path.getName))
  }

  def canViewActivity(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).can_ViewActivity(),
      "User cannot view file activity", Messages("permission_cannot_view_activity", path.getName))
  }

  def canRestoreFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).can_Restore(),
      "User cannot restore file", Messages("permission_cannot_restore_file", path.getName))
  }

  def canDeleteFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFile_Delete(),
      "User cannot delete file", Messages("permission_cannot_delete_file", path.getName))
  }

  def canRenameFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFile_Rename(),
      "User cannot rename file", Messages("permission_cannot_rename_file", path.getName))
  }

  def canModifyFile(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFile_Modify(),
      "User cannot modify file", Messages("permission_cannot_modify_file", path.getName))
  }

  def canViewFileHistory(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFile_ViewHistory(),
      "User cannot view file history", Messages("permission_cannot_history_file", path.getName))
  }

  def canViewPermissions(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).can_ViewPermissions(),
      "User cannot see file permissions", Messages("permission_cannot_view_permissions", path.getName))
  }

  def canAllowAccessToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFolder_AllowAccess(),
      "User cannot change file permissions", Messages("permission_cannot_allow_access", path.getName))
  }
  def canRemoveAccessToFolder(path: Path)(implicit u: User): Either[ApiErrors, Unit] = {
    canOnPath(path, userSession.calculatePermission(path, securityTypeCacheJava).canFolder_RemoveAccess(),
      "User cannot change file permissions", Messages("permission_cannot_remove_access", path.getName))
  }

  private def canOnPath(path: Path, can: Boolean,
                        msg: String = "Cannot access resource",
                        userMsg: String = Messages("permissions_default_error_msg"),
                        statusCode: Int = Status.FORBIDDEN)(implicit u: User): Either[ApiErrors, Unit] = {

    if (Path.ROOT_PATH_PARENT.equals(path.getPathId) || can) Right { () }
    else Left {
      ApiErrors.single(msg,
        userMsg,
        statusCode, Some("authorization"))
    }
  }

  def securityTypeCacheJava(implicit user: User): java.util.Map[UUID, SecurityType] = {
    securityTypeCache.asJava
  }

  def securityTypeCache(implicit user: User): Map[UUID, SecurityType] = {
    cache.getOrElse[Map[UUID, SecurityType]]("security_type_" + user.getTenantId.toString()) {
      getOrCreateSecurityTypes
    }
  }

  protected def getOrCreateSecurityTypes(implicit user: User): Map[UUID, SecurityType] = {
    val sta = userSession.accessor(classOf[SecurityTypeAccessor])

    val allTypes = sta.getSecurityTypes.all.asScala
    val tenantTypes = allTypes.filter { st => st.getTenantId.equals(user.getTenantId) }

    val resTypes = if (tenantTypes.isEmpty) {
      createSecurityTypes
    } else {
      tenantTypes
    }
    resTypes.map { st => (st.getSecurityTypeId, st) } toMap
  }

  protected def createSecurityTypes(implicit user: User): Seq[SecurityType] = {
    var res = Array[SecurityType]()
    val spm = userSession.mapper(classOf[SecurityType])
    val viewer = new SecurityType("viewer", UUID.randomUUID(), PermissionSets.viewerValue)
    spm.save(viewer)
    res = res :+ viewer
    val editor = new SecurityType("editor", UUID.randomUUID(), PermissionSets.editorValue)
    spm.save(editor)
    res = res :+ editor
    val employee = new SecurityType("employee", UUID.randomUUID(), PermissionSets.employeeValue)
    spm.save(employee)
    res = res :+ employee
    val owner = new SecurityType("owner", UUID.randomUUID(), PermissionSets.ownerValue)
    spm.save(owner)
    res = res :+ owner
    res
  }

  override def canAccessFolder(path: Path)(implicit u: User): Boolean = {
    path.isDirectory() && {
      val accessAccessor = userSession.accessor(classOf[AccessSubfoldersAccessor])
      accessAccessor.countAccess(path.getPathId, userPrincipals.asJava).one().get("access_count", classOf[Long]) > 0
    }
  }

  def userPrincipals(implicit u: User): Seq[UUID] = {
    //    val gmfa = userSession.accessor(classOf[GroupMembersFlatAccessor])
    val gma = userSession.accessor(classOf[GroupMembersAccessor])

    //        List(u.getUserId) ++ gmfa.getUserGroups(u.getUserId).asScala.map(_.getGroupId)
    List(u.getUserId) ++ gma.getGroupMembersByPrincipal(u.getUserId).all.asScala.map(_.getGroupId)
  }

}