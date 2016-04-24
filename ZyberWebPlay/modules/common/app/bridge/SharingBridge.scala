package bridge

import java.util.UUID
import exceptions.BadRequestException
import models.JPath
import models.extra.SharingSubmission
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc._
import services.{ FileService, LoginService }
import util.Util
import zyber.server.dao.{ Path, PathShare, User }

class SharingBridge(fileService: FileService, loginService: LoginService) extends Controller {

  def setSharesFor(thePath: Path,
    s: SharingSubmission, user: User)(implicit u: User): JPath = {
    assert(thePath.isFile)
    def make(user: User) = new PathShare(thePath.getShareId, s.shareType, user.getUserId, thePath.getPathId, null,
      s.password.map(p => BCrypt.hashpw(p, BCrypt.gensalt())).orNull)

    implicit val tenantId: UUID = u.getTenantId

    val shares: List[PathShare] = s.shareType match {
      case "public" => List(make(user))
      case "password" if s.password.isDefined => List(make(user))
      case "users" if s.users.isDefined => {
        val filtered = s.users.get.split(",").map(loginService.getUser).filterNot(o => o.isDefined && o.get.getUserId == user.getUserId) //TODO share only to active users?
        //Only allow the request through if all users are defined and there was at least one user that wasn't the originator
        if (filtered.forall(_.isDefined) && filtered.nonEmpty) {
          filtered.flatten.map(f => make(f).withUsername(f.getName)).toList
        } else throw new BadRequestException()
      }
      case "group" //probably should be implemented further
      //getUsersByGroup 
      if s.users.isDefined => {
        val filtered = s.users.get.split(",").map(loginService.getUser).filterNot(o => o.isDefined && o.get.getUserId == user.getUserId) //TODO share only to active users?
        //Only allow the request through if all users are defined and there was at least one user that wasn't the originator
        if (filtered.forall(_.isDefined) && filtered.nonEmpty) {
          filtered.flatten.map(f => make(f).withUsername(f.getName)).toList
        } else throw new BadRequestException()
      }
      
      case "revoke" => Nil
      case _ => throw new BadRequestException()
    }
    val updated: Path = fileService.resetSharing(thePath, user, shares)
    JPath.fromPathWithShares(updated, fileService, loginService, List(user))
  }

  def setSharesForFolder(thePath: Path, s: SharingSubmission, user: User)(implicit us: User): JPath = {
    assert(thePath.isDirectory)
    def make(user: User) = new PathShare(thePath.getShareId, s.shareType, user.getUserId, thePath.getPathId, null, null)

    implicit val tenantId: UUID = us.getTenantId
    val filtered = s.users.get.split(",").map(loginService.getUser)
    val shares = if (filtered.forall(_.isDefined) && filtered.nonEmpty) {
      val oldPaths = fileService.listLinked(thePath)
      val toAdd = filtered.flatten.filterNot(u => {
        oldPaths.map(_.getParentPathId).contains(u.getHomeFolder)
      })
      val toDelete = if (s.shareType == "revoke") oldPaths else oldPaths.filterNot(u => {
        filtered.flatten.exists(_.getHomeFolder == u.getParentPathId)
      })
      if (s.shareType != "revoke") assert(toAdd.forall(u => fileService.getRootPath(u, thePath.getName).isEmpty))
      toAdd.foreach(u => {
        fileService.linkFolder(user, u, thePath)
      })

      toDelete.foreach(u => {
        fileService.destroy(u.getPathId, u.getParentPathId)
      })
      val newShares = if (s.shareType == "revoke") Nil else filtered.flatten.map(f => {
        //TODO : do we need this? Maybe not
        make(f)
      }).toList
      newShares
    } else throw new BadRequestException()

    val updated: Path = fileService.resetSharing(thePath, user, shares)
    JPath.fromPathWithShares(updated, fileService, loginService, List(user))
  }

  def downloadRestricted(uuid: UUID, user: User)(implicit u: User): Result = {
    fileService.restrictedPath(uuid) match {
      case Some(path) => {
        if (fileService.getSharesForPath(path).exists(s => s.getShareType == "users" && s.getUserId == user.getUserId)) {
          Util.pathToResult(path)
        } else Forbidden
      }
      case None => NotFound
    }
  }
}
