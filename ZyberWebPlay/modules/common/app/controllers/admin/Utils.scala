package controllers.admin

import javax.inject.Inject

import models.JPasswordPolicy
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{WrappedRequest, AnyContent, Request}
import services.MetadataService
import zyber.server.dao.User
import zyber.server.dao.admin.TenantAdmin

case class UserRequest[A](user: TenantAdmin,
                          request: Request[A]) extends WrappedRequest[A](request)

trait Utils {
  implicit def toImplicitOption(implicit ur: UserRequest[_]): Option[UserRequest[_]] = {
    Some(ur)
  }

  def getAction(implicit rs: Request[AnyContent]) = {
    rs.body.asFormUrlEncoded.get("action").headOption
  }
}

object Utils extends Utils {
  implicit val passwordF = Json.format[JPasswordPolicy]

  def getPasswordPolicy(implicit metadataService: MetadataService, user:User) = {
    val current: String = metadataService.getApplicationSetting(policyKey).map(_.getValue).getOrElse("{}")
    Json.parse(current).as[JPasswordPolicy]
  }
  val policyKey = "passwordPolicy"

  val fullAuth = "fullAuth"
}

class Assets @Inject() (val errorHandler: HttpErrorHandler) extends controllers.AssetsBuilder(errorHandler)
