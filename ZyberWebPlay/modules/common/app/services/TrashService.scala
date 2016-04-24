package services

import java.util.UUID
import javax.inject.Inject
import com.datastax.driver.mapping.MappingManager
import com.google.inject.ImplementedBy
import zyber.server.ZyberSession
import zyber.server.dao.{Path, TrashedPath, TrashedPathAccessor}
import scala.collection.JavaConverters._
import zyber.server.CassandraMapperDelegate
import zyber.server.dao.User

@ImplementedBy(classOf[TrashServiceImpl])
trait TrashService {
  def saveTrashedFile(userId:UUID, pathId:UUID)(implicit u: User):TrashedPath
  
  def restoreFromTrash(userId:UUID, pathId:UUID)(implicit u: User)
  
  def listTrash(userId:UUID)(implicit u: User):List[TrashedPath]
}

class TrashServiceImpl @Inject() (val session: ZyberSession, fileService: FileService) 
extends TrashService with MultitenancySupport{
  
  def mapper(implicit user: User): CassandraMapperDelegate[TrashedPath] =
    userSession.mapper(classOf[TrashedPath])
  def accessor(implicit user: User): TrashedPathAccessor =
    userSession.accessor(classOf[TrashedPathAccessor])


  override def saveTrashedFile(userId: UUID, pathId: UUID)(implicit user: User): TrashedPath = {
    val existingPath: Path = fileService.existingPath(pathId,false)
    val path: TrashedPath = new TrashedPath(pathId, userId, existingPath.getParentPathId, existingPath.getName, existingPath.isDirectory)
    mapper.save(path)
    path
  }

  override def listTrash(userId: UUID)(implicit user: User): List[TrashedPath] = accessor.getTrashedPathsForUser(userId).all().asScala.toList

  override def restoreFromTrash(userId: UUID, pathId: UUID)(implicit user: User): Unit = {
    accessor.deleteTrashedPath(userId,pathId)
  }
}
