package models

import java.util.Date
import services.{FileService, LoginService}
import zyber.server.dao.{User, PathShare, Path}
import play.api.Logger

case class JPath(
    name: String,
    size: Long,
    modifiedPretty: Date,
    isDirectory: Boolean,
    isDeleted: Boolean,
    uuid: String,
    linked:Boolean,
    shareId:String,
    sharing:String,
    image:Option[String],
    fullPath:Option[String],
    directoryPath:Option[String],
    shares:List[JShare],
    mimeType: String,
    parentUuid:Option[String] = None
)

case class JShare(shareType:String, userName:Option[String],userEmail:Option[String])

object JShare {
  def fromList(shares: List[PathShare], loginService: LoginService)(implicit u: User): List[JShare] = {
    shares.map(s => {
      val (username,email) = if(s.getShareType == "users") {
        val userObject = loginService.findById(s.getUserId)
        (Some(userObject.getName), Some(userObject.getEmail))
      } else (None,None)
      JShare(s.getShareType, username,email)
    })
  }
}

object JPath{
  def fromPath(path: Path, shares:List[JShare]=Nil): JPath = {
    JPath(
        path.getName,
        path.getSize,
        path.getModifiedDate,
        path.isDirectory,
        path.isDeleted,
        path.getPathId.toString,
        path.isLinked,
        Option(path.getShareId).map(_.toString).orNull,
        shares.headOption.map(_.shareType).getOrElse(""),
        if(path.isDirectory) Some("/assets/images/icons/folder.png") else None,
        None,
        None,
        shares,
        path.getMimeType
    )
  }

  def fromPathWithShares(path:Path, fileService:FileService, 
      loginService: LoginService, filterOut:Seq[User]=Nil, mimeType: String = "")(implicit u: User): JPath = {
    fromPath(path, JShare.fromList(
        fileService.getSharesForPath(path).toList.filterNot(
            p => p.getShareType == "users" && filterOut.exists(u => u.getUserId == p.getUserId)),loginService))
  }
}