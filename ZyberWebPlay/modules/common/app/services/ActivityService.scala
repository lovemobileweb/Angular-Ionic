package services

import java.util
import java.util.{ UUID, Date }
import javax.inject.Inject
import com.datastax.driver.mapping.MappingManager
import com.google.inject.ImplementedBy
import zyber.server.{ ZyberUserSession, ZyberSession }
import zyber.server.dao.ActivityTimeline.Action
import zyber.server.dao._
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import zyber.server.CassandraMapperDelegate
import zyber.server.dao.ActivityTimelineAccessor
import play.api.Logger

@ImplementedBy(classOf[ActivityServiceImpl])
trait ActivityService {

  def saveActivity(user: User, path: Path,
                   time: Date, action: Action, note: String = "")(implicit u: User): ActivityTimeline

  def saveActivity(user: User, pathId: UUID, time: Date,
                   action: Action, note: String)(implicit u: User): ActivityTimeline

  def saveActivity(activityTimeline: ActivityTimeline)(implicit user: User): ActivityTimeline

  def listActivityByUser(user: User)(implicit u: User): Seq[ActivityTimeline]

  def listActivityByPath(pathId: UUID)(implicit user: User): Seq[ActivityTimeline]

  def listActivityByPaths(pathId: Seq[UUID])(implicit user: User): Seq[ActivityTimeline]

  def listActivityRecursively(paths: Seq[Path])(implicit user: User): List[(Path, List[ActivityTimeline])]

  def listLoginActivity(implicit user: User): Seq[ActivityTimeline]

  def saveLoginActivity(user: User, create: Boolean, note: String)(implicit u: User)
}

class ActivityServiceImpl @Inject() (
    val session: ZyberSession, loginService: LoginService) extends ActivityService with MultitenancySupport {
  implicit def newZeus(implicit user: User) = new ZyberUserSession(session, user)

  def activityMapper(implicit user: User): CassandraMapperDelegate[ActivityTimeline] =
    userSession.mapper(classOf[ActivityTimeline])

  def activityAccessor(implicit user: User): ActivityTimelineAccessor =
    userSession.accessor(classOf[ActivityTimelineAccessor])

  def pathAccessor(implicit user: User): PathAccessor =
    userSession.accessor(classOf[PathAccessor])

  def saveActivity(user: User, path: Path, time: Date,
                   action: Action, note: String = "")(implicit u: User): ActivityTimeline = {
    saveActivity(user, path.getPathId, time, action, note)
  }

  def saveActivity(user: User, pathId: UUID, time: Date,
                   action: Action, note: String )(implicit u: User): ActivityTimeline = {
    val zus = newZeus(user)
    val activity = new ActivityTimeline(user.getUserId, pathId, time, action.toString, note, zus)
    activityMapper.save(activity)
    activity
  }

  override def saveActivity(activityTimeline: ActivityTimeline)(implicit user: User): ActivityTimeline = {
    activityMapper.save(activityTimeline)
    activityTimeline
  }

  override def listActivityByPath(pathId: UUID)(implicit user: User): Seq[ActivityTimeline] = {
    activityAccessor.getActivityByFile(pathId).all().asScala
  }

  override def listActivityByPaths(pathIds: Seq[UUID])(implicit user: User): Seq[ActivityTimeline] = {
    activityAccessor.getActivityByFileIn(pathIds.asJava).all().asScala
  }

  override def listActivityByUser(user: User)(implicit u: User): Seq[ActivityTimeline] = {
    activityAccessor.getActivityByUser(user.getUserId).all().asScala
  }

  override def listLoginActivity(implicit user: User): Seq[ActivityTimeline] = {
    val rootPaths: Seq[Path] = loginService.getActiveUsers.map(p => p.getRootPath(newZeus(p)))
    val java: util.List[UUID] = rootPaths.map(_.getPathId).asJava
    val all: Iterable[ActivityTimeline] = activityAccessor.getActivityByFileIn(java).all().asScala
    all.filter(_.getActivity == Action.Login.toString).toList.sortBy(_.getActivityTimestamp.getTime).reverse
  }

  override def saveLoginActivity(user: User, create: Boolean, note: String)(implicit u: User): Unit = {
    val value: String = (if (create) "Created user" else "Logged in ") + note
    val zus: ZyberUserSession = newZeus(user)
    saveActivity(new ActivityTimeline(user.getUserId, user.getRootPath(zus).getPathId, new Date(), ActivityTimeline.Action.Login.toString, value, zus))
  }

  //TODO this is hideously inefficient at the top level for large trees
  override def listActivityRecursively(paths: Seq[Path])(implicit user: User): List[(Path, List[ActivityTimeline])] = {
    val everyPath: ListBuffer[Path] = new ListBuffer[Path]
    gatherPaths(paths, everyPath)
    val allTimeLines: mutable.Buffer[ActivityTimeline] = activityAccessor.getActivityByFileIn(everyPath.map(_.getPathId).asJava).all().asScala
    val by: Map[UUID, mutable.Buffer[ActivityTimeline]] = allTimeLines.groupBy(_.getPathId)
    by.map(t => (everyPath.find(_.getPathId == t._1).get, t._2.toList)).toList
  }

  @tailrec
  private def gatherPaths(paths: Seq[Path], buffer: ListBuffer[Path])(implicit user: User): Unit = {
    val children = pathAccessor.getChildrenIn(paths.filter(_.isDirectory).map(_.getPathId).toList.asJava).all().asScala
    val (dirs, files) = children.partition(_.isDirectory)
    buffer.appendAll(files)
    if (dirs.nonEmpty) {
      gatherPaths(dirs, buffer)
    }
  }
}
