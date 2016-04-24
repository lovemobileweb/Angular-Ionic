package adminservices.admin

import zyber.server.dao.admin.TenantAdmin
import play.api.i18n.I18nSupport
import javax.inject.Inject
import play.api.i18n.MessagesApi
import com.datastax.driver.mapping.MappingManager
import zyber.server.dao.admin.TenantAdminAccessor
import org.mindrot.jbcrypt.BCrypt
import com.google.inject.ImplementedBy
import scala.collection.JavaConverters._
import play.api.i18n.Messages
import com.datastax.driver.mapping.Mapper
import play.api.Logger
import java.util.UUID
import scala.util.Try
import zyber.server.ZyberSession

@ImplementedBy(classOf[LoginServiceImp])
trait LoginService {

  def getUser(username: String): Option[TenantAdmin]

  def checkCredentials(username: String, password: String): Boolean

  def getUsers: Seq[TenantAdmin]

  def createUser(user: TenantAdmin): Either[String, Unit]

  def getUserById(id: UUID): Option[TenantAdmin]
  
  def deleteUser(user: TenantAdmin): Try[Unit]
  
  def updateUser(existent: TenantAdmin, updatedUser: TenantAdmin): Either[String, Unit]
}

class LoginServiceImp @Inject() (
    val session: ZyberSession,
    val messagesApi: MessagesApi) extends LoginService with I18nSupport {

  lazy val mappingManager: MappingManager = session.getMappingManagerForTenants

  lazy val userAccessor: TenantAdminAccessor = mappingManager.createAccessor(classOf[TenantAdminAccessor])

  lazy val userMapper: Mapper[TenantAdmin] = mappingManager.mapper(classOf[TenantAdmin])

  def getUser(username: String): Option[TenantAdmin] = {
    Option(userAccessor.getUserByUsername(username))
  }

  def checkCredentials(username: String, password: String): Boolean =
    getUser(username)
      .filter(u=>
        u.getActive)
      .exists(u=>
        BCrypt.checkpw(password, u.getPassword))

  def getUsers: Seq[TenantAdmin] = {
    val username = play.Play.application.configuration.getString("admin_user")

    val users = userAccessor.getUsers.all().asScala
//    users.filter { user => !username.equals(user.getUsername) }
    users
  }

  def createUser(user: TenantAdmin): Either[String, Unit] = {
    try {
      val maybeUser = getUser(user.getUsername)
      if (maybeUser.isDefined) {
        Left(Messages("user_exists"))
      } else Right {
        user.setPassword(BCrypt.hashpw(user.getPassword, BCrypt.gensalt()))
        userMapper.save(user)
      }
    } catch {
      case e: Exception =>
        Logger.error("Error creating user:", e)
        Left(e.getMessage)
    }
  }

  def getUserById(id: UUID): Option[TenantAdmin] =
    Option(userAccessor.getUserById(id))

  def deleteUser(user: TenantAdmin): Try[Unit] = Try {
    userAccessor.deleteTenant(user.getUserId)
  }

  def updateUser(existent: TenantAdmin, updatedUser: TenantAdmin): Either[String, Unit] = {
    try {
      if (!existent.getUsername.equals(updatedUser.getUsername) &&
        getUser(updatedUser.getUsername).isDefined)
        Left(Messages("user_exists"))
      else Right {
        if(updatedUser.getReset)
          updatedUser.setPassword(BCrypt.hashpw(updatedUser.getPassword, BCrypt.gensalt()))
        else
          updatedUser.setPassword(existent.getPassword)
        userMapper.save(updatedUser)
      }
    } catch {
      case e: Exception => 
        Logger.error("Error updating user: ", e)
        Left(e.getMessage)
    }
  }
}