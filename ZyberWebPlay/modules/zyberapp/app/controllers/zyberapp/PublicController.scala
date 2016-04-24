package controllers.zyberapp

import java.util.UUID
import javax.inject.Inject
import org.mindrot.jbcrypt.BCrypt
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import services.FileService
import util.{Secured, MultitenancyHelper}
import zyber.server.dao.Path
import play.api.libs.concurrent.Execution.Implicits._
import zyber.server.dao.User
import services.LoginService
import scala.sys.process._
class PublicController @Inject()(
    val loginService: LoginService,
    val fileService: FileService,
    val messagesApi: MessagesApi,
    val multitenancyHelper: MultitenancyHelper) extends Controller with I18nSupport with Secured {

  def downloadFile(uuid: UUID) = TenantAction { implicit rs =>
    implicit val u = new User
    u.setTenantId(rs.tenant.getTenantId)
    fileService.downloadPublicPath(uuid) match {
      case None => NotFound
      case Some(path) =>
        val e = Enumerator.fromStream(path.getInputStream)
        pathToResult(path, e)
    }
  }


  def gitUpdate() = Action { implicit rs =>
    import scala.sys.process._
    //import play.api.Play.current
    
    val cmd = "git pull "// + Play.application.path
    val output = cmd.!! // Captures the output  
    Ok("Reloaded from git!")
  }

  def downloadPasswordRestrictedFile(uuid: UUID) = TenantAction { implicit rs =>
    implicit val u = new User
    u.setTenantId(rs.tenant.getTenantId)
    fileService.restrictedPath(uuid) match {
      case None => NotFound
      case Some(path) =>
        val found = fileService.getSharesForPath(path).find(_.getShareType == "password")
        found.map(share => {
         val opt = rs.headers.get("Authorization").flatMap { authorization =>
            authorization.split(" ").drop(1).headOption.filter { encoded =>
             new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
               case u :: p :: Nil => BCrypt.checkpw(p, share.getPassword)
               case _ => false
             }
           }
         }
          opt.map(a => {
            val e = Enumerator.fromStream(path.getInputStream)
            pathToResult(path, e)
          }).getOrElse(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured Area""""))
        }).getOrElse(NotFound)
    }
  }

  private def pathToResult(path: Path, e: Enumerator[Array[Byte]]): Result = {
    Ok.chunked(e).withHeaders("Content-Disposition" -> s"attachment; filename=${path.getName}")
  }
  
  
}
