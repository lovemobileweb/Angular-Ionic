package util

import javax.inject.Inject

import com.google.inject.ImplementedBy
import core.{ApiError, ApiErrors, ZyberResponse}
import play.api.Logger
import play.api.http.Status
import play.api.mvc.{RequestHeader, Result, Results}
import zyber.server.dao.admin.Tenant
import zyber.server.{ZyberSession, ZyberUserSession}

trait WithMultitenancy {

  //Returns tenant's subdomain
  //TODO check if in production to ignore this
  def getTenant(request: RequestHeader): Option[String] = {
    val domain = request.domain
    if ("localhost".equals(domain) || isIpaddress(domain) || "admin.localhost".equals(domain))
      Some("localhost")
    else {
      Logger.debug("Domain: " + domain)
      getSubdomain(domain)
    }
  }

  def isIpaddress(domain: String): Boolean = {
    val pattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    domain.matches(pattern)
  }

  def getSubdomain(domain: String): Option[String] = {
    try {
      if(domain == "admin.localhost") {
        return Some("admin")
      }
      val pattern = "(?:http[s]*\\:\\/\\/)*(.*)\\.(?:[^\\/]*\\..{2,5})".r
      val pattern(subDomain) = domain
      val i = subDomain.indexOf(".")
      val firstSubdomain = if(i < 0) subDomain else subDomain.substring(0, i)
      Some(firstSubdomain)
    } catch {
      case e: Exception =>
        //        Logger.error("Error: No subdomain found", e)
        None
    }
  }
}

object WithMultitenancy extends WithMultitenancy

@ImplementedBy(classOf[DefaultMultitenancyHelperImp])
trait MultitenancyHelper {
  def getTenant(request: RequestHeader): Option[String]

  def validTenant(subdomain: String): Option[Tenant]

  def onInvalidTenant(request: RequestHeader): Result

  def onInvalidTenantApi(request: RequestHeader): Result
}

class DefaultMultitenancyHelperImp @Inject() (
    val session: ZyberSession) extends MultitenancyHelper with WithMultitenancy {

  def validTenant(subdomain: String): Option[Tenant] = {
    if ("localhost".equals(subdomain)) {
      Some(ZyberUserSession.DEFAULT_TENANT)
    } else {
      Option(session.getTenantBySubdomain(subdomain))
    }
  }

  def onInvalidTenant(request: RequestHeader): Result = {
    Results.NotFound("Invalid tenant") //TODO show error page
  }

  def onInvalidTenantApi(request: RequestHeader): Result = ZyberResponse.errorResult(
    ApiErrors(List(ApiError("Invalid tenant", "Invalid tenant", Status.NOT_FOUND, 
        Some("accessing_tenant")))))

}