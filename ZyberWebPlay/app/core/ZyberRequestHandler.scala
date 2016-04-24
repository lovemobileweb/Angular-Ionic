package core

import javax.inject.Inject

import controllers.zyberapp.WebDavController
import play.api.Logger
import play.api.http._
import play.api.mvc.{Action, RequestHeader, Results}
import util.WithMultitenancy

class ZyberRequestHandler @Inject() (errorHandler: HttpErrorHandler,
                                     configuration: HttpConfiguration, filters: HttpFilters,
                                     mainRouter: zyberapp.Routes, webDavController: WebDavController
                                     , tenantsRouter: tenants.Routes
                                    )
    extends DefaultHttpRequestHandler(
      mainRouter, errorHandler, configuration, filters) {

  override def routeRequest(request: RequestHeader): Option[play.api.mvc.Handler] = {
    WithMultitenancy.getSubdomain(request.domain) match {
      case Some("admin") =>
        Logger.debug("Admin request")
        tenantsRouter.routes.lift(request)
//              super.routeRequest(request)
      case _ =>
        //        if (request.method == "OPTIONS") {
        //          Logger.debug("WebDAV OPTIONS request: " + request.path);
        //          request.headers.toMap.map(x => { Logger.debug(" Header: " + x._1 + "=" + x._2) })
        //          val ret = Option { webDavController.handleRequest(request) }
        //          Logger.debug("Response: " + ret);
        //          ret
        //        } else 
        if (request.path.startsWith("/WebDAV")) {
          Logger.debug("WebDAV " + request.method + " request: " + request.path);
          request.headers.toMap.map(x => { Logger.debug(" Header: " + x._1 + "=" + x._2) })

          val ret = Option { webDavController.handleRequest(request) }
          Logger.debug("Response: " + ret);
          ret
        } else if (request.method == "OPTIONS") {
          Logger.debug("Routing for options " + request.method + " request: " + request.path);
          Some{ preflight }
        } else {
          Logger.debug("Routing " + request.method + " request: " + request.path);
          super.routeRequest(request)
        }
    }

  }

  def preflight() = Action {
    Results.Ok.withHeaders("Access-Control-Allow-Origin" -> "*",
      "Allow" -> "*",
      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, bearer");
  }
}
