package actors

import akka.actor._
import zyber.server.dao.User
import zyber.server.dao.Path
import services.MultitenancySupport
import javax.inject._
import zyber.server.ZyberSession
import zyber.server.dao.PathType
import scala.concurrent.Future
import akka.pattern.{ pipe }
import java.util.UUID

object CreatePathReceptionist {
  def props = Props[CreatePathReceptionist]

  case class CreatePath(dstPath: Path, name: String, pathType: PathType)

  case class Done(createdPath: Path)

  case class Error(e: Exception)

}

class CreatePathReceptionist @Inject() (
    val session: ZyberSession) extends Actor with MultitenancySupport {

  import CreatePathReceptionist._

  var jobs = Map.empty[(UUID, String), (PathType, List[ActorRef])]
  var children = Set.empty[ActorRef]
  def receive = {
    case CreatePath(dstPath, name, pathType) =>
      val key = (dstPath.getPathId, name)
      val mc = jobs.get(key)
      val client = sender

      if (!mc.isDefined) {
        children += context.actorOf(Props(new CreatePathActor(dstPath, name, pathType)))
        jobs += (key -> (pathType, List(client)))
      } else {
        val (opt, cl) = mc.get
        if (opt.equals(pathType)) {
          jobs = jobs.updated(key, (opt, client :: cl))
        } else {
          client ! Error(new RuntimeException("File already exists"))
        }
      }
    case CreatePathActor.PathCreated(dstPath, createdPath) =>
      val key = (dstPath.getPathId, createdPath.getName)
      val mc = jobs.get(key)
      val done = Done(createdPath)
      mc.foreach(_._2.foreach(_ ! done))
      jobs -= key
      children -= sender
    case CreatePathActor.OncreateError(e, dstPath, name) =>
      val key = (dstPath.getPathId, name)
      val mc = jobs.get(key)
      val error = Error(e)
      mc.foreach(_._2.foreach(_ ! error))
      jobs -= key
      children -= sender
  }
}

object CreatePathActor {
  def createPathActorProps = Props[CreatePathActor]

  case class PathCreated(dstPath: Path, createdPath: Path)

  case class OncreateError(error: Exception, dstPath: Path, name: String)
}

class CreatePathActor(dstPath: Path, name: String, pathType: PathType) extends Actor {
  import CreatePathActor._

  implicit val exec = context.dispatcher

  val createFuture = Future.apply {
    if (PathType.Directory.equals(pathType)) {
      dstPath.createDirectory(name)
    } else {
      dstPath.createFile(name)
    }
  } map (p => PathCreated(dstPath, p))

  createFuture.recover {
    case e: Exception => OncreateError(e, dstPath, name)
  } pipeTo self

  def receive = {
    case pc: PathCreated =>
      context.parent ! pc
      stop()
    case oce: OncreateError =>
      context.parent ! oce
      stop()
    case _: Status.Failure => stop()
  }

  def stop(): Unit = {
    context.stop(self)
  }
}