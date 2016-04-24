package controllers.admin

import play.api._
import play.api.mvc._
import play.api.i18n.I18nSupport
import adminservices.admin.LoginService
import javax.inject.Inject
import play.api.i18n.MessagesApi
import adminservices.admin.TenantsService
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import zyber.server.dao.admin.Tenant
import java.util.UUID
import java.util.Date
import scala.util.Failure
import play.api.i18n.Messages
import scala.util.Success
import zyber.server.dao.User

class TenantsController @Inject() (
    val loginService: LoginService,
    val tenantsService: TenantsService,
    val messagesApi: MessagesApi) extends Secured with Controller with I18nSupport with Utils {

  val tenantForm = Form {
    mapping(
      "tenant_id" -> optional(text),
      "name" -> nonEmptyText,
      "contact_name" -> nonEmptyText,
      "contact_phone" -> optional(text).verifying(Messages("invalid_phone"), (phone: Option[String]) => phone.map(_.matches("^[0-9]+$")).getOrElse(true)),
      "contact_email" -> email.verifying(nonEmpty),
      "subdomain" -> nonEmptyText.verifying(Messages("invalid_subdomain"), sub => sub.matches("^[a-zA-Z0-9-]+$"))) {
        (id, name, contactName, contactPhone, contactEmail, subdomain) =>
          val uuid = id.map(UUID.fromString(_)).getOrElse(UUID.randomUUID())
          new Tenant(uuid, name, contactName, contactPhone.getOrElse(null), contactEmail, new Date, subdomain)
      } { (t: Tenant) =>

        Some((Option(t.getTenantId.toString()), t.getTenantName,
          t.getContactName, Option(t.getContactPhone),
          t.getContactEmail, t.getSubdomain))
      }
  }

  val newAccountForm = Form {
    tuple(
      "email" -> email.verifying(nonEmpty),
      "password" -> nonEmptyText(minLength = 6).
        verifying(Messages("account.create.password"), pass => pass.matches("^\\S+$")),
      "name" -> nonEmptyText(minLength = 6))
  }

  val changePassForm = Form {
    single(
      "password" -> nonEmptyText(minLength = 6).
        verifying(Messages("account.create.password"), pass => pass.matches("^\\S+$")))
  }

  def index = Authenticated { implicit rs =>
    println("Tenants")
    Redirect(routes.TenantsController.tenants)
  }

  def tenants = Authenticated { implicit rs =>
    Ok(views.html.admin.tenants.tenants(tenantsService.getTenants))
  }

  def createTenant = Authenticated { implicit rs =>
    Ok(views.html.admin.tenants.createTenant(tenantForm))
  }

  def addTenant = Authenticated { implicit rs =>
    tenantForm.bindFromRequest.fold(
      fwe => BadRequest(views.html.admin.tenants.createTenant(fwe)),
      tenant => {
        tenantsService.createTenant(tenant) match {
          case Left(msg) =>
            Redirect(routes.TenantsController.tenants()).flashing("error" -> msg)
          case Right(_) =>
            Redirect(routes.TenantsController.tenants()).flashing("success" -> Messages("tenant_created"))
        }
      })
  }

  def withTenant(id: String)(f: Tenant => Result): Result = {
    tenantsService.getTenant(UUID.fromString(id)) map { tenant =>
      f(tenant)
    } getOrElse {
      Redirect(routes.TenantsController.tenants()).flashing("error" -> Messages("invalid_tenant"))
    }
  }

  def editTenant(id: String) = Authenticated { implicit rs =>
    withTenant(id) { tenant =>
      Ok(views.html.admin.tenants.editTenant(tenantForm.fill(tenant), tenant.getTenantId.toString()))
    }
  }

  def updateDeleteTenant(id: String) = Authenticated { implicit rs =>
    getAction match {
      case Some("delete") => deleteTenant(id)(rs)
      case _              => updateTenant(id)(rs)
    }
  }

  def updateTenant(id: String)(implicit rs: UserRequest[_]): Result = {
    withTenant(id) { existent =>
      tenantForm.bindFromRequest.fold(
        fwe => BadRequest(views.html.admin.tenants.editTenant(fwe, id)),
        tenant => {
          tenant.setTenantId(UUID.fromString(id))
          tenantsService.updateTenant(existent, tenant) match {
            case Left(msg) =>
              Redirect(routes.TenantsController.tenants()).flashing("error" -> msg)
            case Right(_) =>
              Redirect(routes.TenantsController.tenants()).flashing("success" -> Messages("tenant_updated"))
          }
        })
    }
  }

  def deleteTenant(id: String)(implicit rs: UserRequest[_]): Result = {
    withTenant(id) { tenant =>
      tenantsService.deleteTenant(tenant) match {
        case Failure(e) =>
          Logger.debug("Error deleting tenant: ", e)
          Redirect(routes.TenantsController.tenants()).flashing("error" -> e.getMessage)
        case Success(_) =>
          Redirect(routes.TenantsController.tenants()).flashing("success" -> Messages("tenant_deleted"))
      }
    }
  }

  def viewTenant(id: String) = Authenticated { implicit rs =>
    withTenant(id) { tenant =>
      Ok(views.html.admin.tenants.viewTenant(tenant))
    }
  }

  def visitTenant(id: String) = Authenticated { implicit rs =>
    withTenant(id) { tenant =>
      val host = rs.request.host
      val tenantUrl = host.replaceFirst("admin", tenant.getSubdomain)
      Logger.debug("tenantUrl: " + tenantUrl)
      Redirect("http://" + tenantUrl)
    }
  }

  def tenantUsers(id: String) = Authenticated { implicit rs =>
    withTenant(id) { t =>
      Ok(views.html.admin.tenants.users(t, tenantsService.tenantUsers(t)))
    }
  }

  def newAccount(id: String) = Authenticated { implicit rs =>
    withTenant(id) { t =>
      Ok(views.html.admin.tenants.createAccount(t, newAccountForm))
    }
  }

  def createAccount(id: String) = Authenticated { implicit rs =>
    withTenant(id) { t =>
      newAccountForm.bindFromRequest.fold(
        fwe => BadRequest(views.html.admin.tenants.createAccount(t, fwe)),
        {
          case (email1, password, name) =>
            tenantsService.getTenantUser(t, email1) match {
              case Some(user) =>
                val formWithError = newAccountForm.fill((email1, password, name)).withError("username", Messages("account.create.user.exists"))
                BadRequest(views.html.admin.tenants.createAccount(t, formWithError))
              case None =>
                val prefLang = messagesApi.preferred(rs).lang.code
                tenantsService.createUser(t, email1, password, name, prefLang) match {
                  case Success(nUser) =>
                    Redirect(routes.TenantsController.tenantUsers(id)).
                      flashing("success" -> Messages("account.create.success"))
                  case Failure(e) =>
                    Logger.error("Error creating account", e)
                    Redirect(routes.TenantsController.tenantUsers(id))
                      .flashing("error" -> e.getMessage)
                }
            }
        })
    }
  }

  def editUser(id: String, userId: String) = Authenticated { implicit rs =>
    withTenantUser(id, userId) { t =>
      u =>
        Ok(views.html.admin.tenants.editAccount(t, u, changePassForm))
    }
  }

  def updateDeleteUser(id: String, userId: String) = Authenticated { implicit rs =>
    getAction match {
      case Some("delete") => deleteUser(id, userId)(rs)
      case _              => updateUser(id, userId)(rs)
    }
  }

  def deleteUser(tenantId: String, userId: String)(implicit rs: UserRequest[_]): Result = {
    withTenantUser(tenantId, userId) { t =>
      u =>
        tenantsService.deleteTenantUser(t, u) match {
          case Failure(e) =>
            Logger.debug("Error deleting tenant: ", e)
            Redirect(routes.TenantsController.tenantUsers(tenantId)).flashing("error" -> e.getMessage)
          case Success(_) =>
            Redirect(routes.TenantsController.tenantUsers(tenantId)).flashing("success" -> Messages("user_deleted"))
        }
    }
  }

  def updateUser(tenantId: String, userId: String)(implicit rs: UserRequest[_]): Result = {
    withTenantUser(tenantId, userId) { t =>
      u =>
        changePassForm.bindFromRequest.fold(
          fwe => BadRequest(views.html.admin.tenants.editAccount(t, u, fwe)),
          {
            case (password) =>
              tenantsService.updateTenantUser(t, u, password) match {
                case Success(_) =>
                  Redirect(routes.TenantsController.tenantUsers(tenantId)).
                    flashing("success" -> Messages("account.create.success"))
                case Failure(e) =>
                  Logger.error("Error creating account", e)
                  Redirect(routes.TenantsController.tenantUsers(tenantId))
                    .flashing("error" -> e.getMessage)
              }
          })
    }
  }

  def withTenantUser(tenantId: String, userId: String)(f: Tenant => User => Result): Result = {
    withTenant(tenantId) { t =>
      tenantsService.getTenantUserById(t, UUID.fromString(userId)) map { u =>
        f(t)(u)
      } getOrElse {
        Redirect(routes.TenantsController.tenants()).flashing("error" -> Messages("invalid_user"))
      }
    }
  }
}
