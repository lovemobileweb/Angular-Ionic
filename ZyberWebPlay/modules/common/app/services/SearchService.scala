package services

import javax.inject.Inject
import com.google.inject.ImplementedBy
import play.api.i18n.{I18nSupport, MessagesApi}
import zyber.server.{ZyberUserSession, ZyberSession}
import zyber.server.dao._
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import play.api.Logger

@ImplementedBy(classOf[SearchServiceImpl])
trait SearchService {
  def search(user: User, name: String, hiddenOnly: Boolean, showHidden: Boolean, rootPath: Path, limit: Boolean):List[Path]
}

class SearchServiceImpl @Inject() (
                                  val session: ZyberSession,
                                  val securityService: SecurityService
                                  ) extends SearchService  with MultitenancySupport {

  def pathAccessor(implicit user: User): PathAccessor = userSession.accessor(classOf[PathAccessor])
  def accessor(implicit showHidden:Boolean, user: User): CommonPathAccessor = if(showHidden) hiddenPathAccessor else pathAccessor
  def hiddenPathAccessor(implicit user: User):PathIncludingDeletedAccessor = userSession.accessor(classOf[PathIncludingDeletedAccessor])

  override def search(user: User, name: String, hiddenOnly: Boolean, showHidden: Boolean, rootPath: Path, limit: Boolean): List[Path] = {
    Logger.debug("Searching in: " + rootPath.getName)
    Logger.debug("Limiting search: " + limit)
    val session1: ZyberUserSession = new ZyberUserSession(session,user)
//    val rootPath: Path = user.getRootPath(session1)
    val paths: ListBuffer[Path] = new ListBuffer[Path]
    //FIXME take permission when gathering paths
    gatherPaths(Seq(rootPath),paths)(user,showHidden)
    val r = paths.filter(p => p.getName.toLowerCase.contains(name.toLowerCase) &&(!hiddenOnly || p.isDeleted)).toList
    if(limit) r.take(10)
    else r
  }

  @tailrec
  private def gatherPaths(paths: Seq[Path], buffer: ListBuffer[Path])(implicit user: User, showHidden:Boolean): Unit = {
//    securityService.canViewOrAccessFolde
    val folders = paths.filter { p => p.isDirectory() && securityService.canViewOrAccessFolder(p).isRight }
    val children = accessor.getChildrenIn(folders.map(_.getPathId).toList.asJava).all().asScala
    val (dirs, files) = children.partition(_.isDirectory)
    
    buffer.appendAll(files.filter(securityService.canViewFile(_).isRight))
    if (dirs.nonEmpty) {
      gatherPaths(dirs, buffer)
    }
  }
}
