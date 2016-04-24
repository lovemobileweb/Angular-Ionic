package controllers.zyberapp

import play.api.i18n.I18nSupport
import play.api.mvc.Controller
import models.JsonFormats
import services.FileService
import services.SecurityService
import play.api.i18n.MessagesApi
import javax.inject.Inject
import services.LoginService
import util.{Secured, MultitenancyHelper}

class SharesController  @Inject() (
  val loginService: LoginService,
  val fileService: FileService,
  val messagesApi: MessagesApi,
  val multitenancyHelper: MultitenancyHelper,
  val securityService: SecurityService) extends Secured with Controller
    with I18nSupport with JsonFormats {
  
  
}