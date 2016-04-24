package adminservices.admin

import com.google.inject.ImplementedBy
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import zyber.server.dao.admin.Tenant
import com.datastax.driver.mapping.MappingManager
import scala.collection.JavaConverters._
import zyber.server.dao.admin.TenantsAccessor
import scala.util.Try
import com.datastax.driver.mapping.Mapper
import javax.inject.Inject
import java.util.UUID
import play.api.Logger
import play.api.i18n.Messages
import java.util.Date
import zyber.server.dao.User
import zyber.server.dao.rawaccessors.UserAccessor
import zyber.server.dao.UserKeys
import zyber.server.dao.Principal
import org.mindrot.jbcrypt.BCrypt
import zyber.server.dao.rawaccessors.UserKeysAccessor
import zyber.server.ZyberSession
import services.{ LoginService => TenantLoginService }
import zyber.server.Abilities

@ImplementedBy(classOf[TenantsServiceImp])
trait TenantsService {

  def getTenants: Seq[Tenant]

  def createTenant(tenant: Tenant): Either[String, Unit]

  def getTenant(id: UUID): Option[Tenant]

  def updateTenant(existent: Tenant, newTenant: Tenant): Either[String, Unit]

  def deleteTenant(tenant: Tenant): Try[Unit]

  def tenantUsers(tenant: Tenant): Seq[User]

  def getTenantUser(tenant: Tenant, email: String): Option[User]

  def createUser(tenant: Tenant, email: String, password: String,
                 name: String, lang: String): Try[User]

  def getTenantUserById(tenant: Tenant, id: UUID): Option[User]

  def deleteTenantUser(tenant: Tenant, user: User): Try[Unit]

  def updateTenantUser(tenant: Tenant, user: User, pass: String): Try[Unit]
}

class TenantsServiceImp @Inject() (
    val session: ZyberSession,
    val messagesApi: MessagesApi,
    val tenantLoginService: TenantLoginService) extends TenantsService with I18nSupport {

  lazy val mappingManager: MappingManager = session.getMappingManagerForTenants
  lazy val tenantAccessor: TenantsAccessor = mappingManager.createAccessor(classOf[TenantsAccessor])
  lazy val tenantMapper = mappingManager.mapper(classOf[Tenant])

  lazy val tenantUsersAccessor: UserAccessor = mappingManager.createAccessor(classOf[UserAccessor])
  lazy val tenantUserMapper: Mapper[User] = mappingManager.mapper(classOf[User])

  lazy val tenantUsersKeysAccessor: UserKeysAccessor = mappingManager.createAccessor(classOf[UserKeysAccessor])
  lazy val tenantUserKeysMapper: Mapper[UserKeys] = mappingManager.mapper(classOf[UserKeys])

  def getTenants: Seq[Tenant] =
    tenantAccessor.getTenants.all().asScala.toList

  def createTenant(tenant: Tenant): Either[String, Unit] = {
    try {
      if (null != tenantAccessor.getTenantBySubdomain(tenant.getSubdomain)) {
        Left(Messages("subdomain_used"))
      } else Right {
        tenantMapper.save(tenant)
      }
    } catch {
      case e: Exception =>
        Logger.debug("Error creating tenant: ", e)
        Left(e.getMessage)
    }
  }

  def getTenant(id: UUID): Option[Tenant] =
    Option(tenantAccessor.getTenantById(id))

  def updateTenant(existent: Tenant, newTenant: Tenant): Either[String, Unit] = {
    try {
      if (!existent.getSubdomain.equals(newTenant.getSubdomain) &&
        null != tenantAccessor.getTenantBySubdomain(newTenant.getSubdomain)) {
        Left(Messages("subdomain_used"))
      } else Right {
        tenantMapper.save(newTenant)
      }
    } catch {
      case e: Exception =>
        Logger.debug("Error creating tenant: ", e)
        Left(e.getMessage)
    }
  }

  def deleteTenant(tenant: Tenant): Try[Unit] = Try {
    tenantAccessor.deleteTenant(tenant.getTenantId)
  }

  def tenantUsers(tenant: Tenant): Seq[User] = {
    tenantUsersAccessor.getActiveUsers(tenant.getTenantId).all().asScala
  }

  def getTenantUser(tenant: Tenant, email: String): Option[User] = {
    Option(tenantUsersAccessor.getUserByEmail(tenant.getTenantId, email))
  }

  def getTenantUserById(tenant: Tenant, id: UUID): Option[User] = {
    Option(tenantUsersAccessor.getById(tenant.getTenantId, id))
  }

  //FIXME change UI to use AngularJS and reuse services and views to create Users, allow administrator to select user role ??
  def createUser(tenant: Tenant, email: String, password: String,
                 name: String, lang: String): Try[User] = {
    
    tenantLoginService.createUser(email, password, name, lang, Abilities.DefaultUserRoles.powerUser.getRoleId)(tenant.getTenantId)
    
  }

  def deleteTenantUser(tenant: Tenant, user: User): Try[Unit] = Try {
    tenantUsersAccessor.deleteUser(tenant.getTenantId, user.getUserId)
  }

  def updateTenantUser(tenant: Tenant, user: User, pass: String): Try[Unit] = Try {
    
//    user.setEmail(email)
//    user.setName(name)
//    tenantUserMapper.save(user)

    Option(tenantUsersKeysAccessor.getUserKeysByUserId(tenant.getTenantId, user.getUserId)) foreach {
      uk =>
        uk.setPasswordHash(BCrypt.hashpw(pass, BCrypt.gensalt()))
        uk.setPasswordHashType("BCrypt")

        tenantUserKeysMapper.save(uk)
    }
  }

}