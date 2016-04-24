package services

import java.util.UUID
import zyber.server.dao.Path
import zyber.server.dao.User
import zyber.server.dao.PathAccessor
import scala.annotation.tailrec
import core.ApiErrors
import play.api.i18n.Messages
import play.api.i18n.I18nSupport
import play.api.http.Status
import zyber.server.dao.CommonPathAccessor
import zyber.server.dao.PathIncludingDeletedAccessor

trait PathsCommon { this: I18nSupport with MultitenancySupport =>
  import Status._

  def pathAccessor(implicit user: User): PathAccessor

  def deletedPathAccessor(implicit user: User): PathIncludingDeletedAccessor

  protected def access(showHidden: Boolean)(implicit u: User): CommonPathAccessor = {
    if (showHidden) deletedPathAccessor else pathAccessor
  }

  def getPathByUUID(uuid: UUID, showHidden: Boolean = false)(implicit u: User): Option[Path] = {
    val zus = userSession
    Option(access(showHidden).getPath(uuid)).map(_.withZus(zus))
  }
  def getNamedChild(parentId: UUID, name: String)(implicit u: User): Option[Path] = {
    Option(pathAccessor.getChildNamed(parentId, name).one())
  }

  def getSharesPath()(implicit u: User): Option[Path] = {
    getNamedChild(Path.ROOT_PATH_PARENT, Path.SHARES_FOLDER)
  }

  def getLocalizedFolderName(path: Path)(implicit u: User): String = {

    if (path.getPathId.equals(u.getHomeFolder)) Messages("home")
    else if (path.getPathId.equals(getSharesPath)) {
      Messages("shares")
    } else
      path.getName

  }

  def getPathHierarchy(pathId: UUID)(implicit u: User): Either[ApiErrors, Seq[Path]] = {
    for {
      startPath <- getPathByUUID(pathId).toRight(ApiErrors.single("invalid path", Messages("api.invalid.path", pathId), NOT_FOUND)).right
    } yield {
      getPathHierarchy(startPath, List())
    }
  }
  
   def getPathHierarchy(path: Path)(implicit u: User): List[Path] = {
     getPathHierarchy(path, List())
   }


  @tailrec
  protected final def getPathHierarchy(path: Path, hierarchy: List[Path])(implicit u: User): List[Path] = {
    val sp = getSharesPath
    if (u.getHomeFolder.equals(path.getPathId)) {
      path.setName(Messages("home"))
      path :: hierarchy
    } else if (sp.filter(_.getPathId.equals(path.getPathId)).isDefined) {
      sp
        .map { p =>
          p.setName(Messages("shares"))
          p
        }.map(p => p :: hierarchy)
        .getOrElse(hierarchy)
    } else {
      val mp = getPathByUUID(path.getParentPathId)
      if (mp.isDefined)
        getPathHierarchy(mp.get, path :: hierarchy)
      else
        hierarchy
    }
  }

}