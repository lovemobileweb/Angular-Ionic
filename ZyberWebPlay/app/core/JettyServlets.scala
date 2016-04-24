package core

import javax.inject.{ Inject, _ }

import com.typesafe.config.ConfigFactory
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.{ Context, FilterHolder, ServletHolder }
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

trait Jetty {}

@Singleton
class JettyProvider @Inject() (applicationLifecycle: ApplicationLifecycle) extends Provider[Jetty] {
  var jettyServer: Server = null
  private val useJetty: Boolean = ConfigFactory.load().getBoolean("jetty.enabled") && (!ConfigFactory.load("local").hasPath("jetty.disabled") || !ConfigFactory.load("local").getBoolean("jetty.disabled") )
  if (useJetty) {
    Logger.debug("Starting jetty")
    jettyServer = new Server(9002)
    val context = new Context(jettyServer, "/", Context.SESSIONS)
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetJsHandlerServlet()), "/GetJsHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetCssHandlerServlet()), "/GetCssHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetImageHandlerServlet), "/images/*")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetFontHandlerServlet), "/fonts/*")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetDocumentPageImageHandlerServlet()), "/GetDocumentPageImageHandler/*")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetFileHandlerServlet()), "/GetFileHandler/*")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetImageUrlsHandlerServlet()), "/GetImageUrlsHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetPrintableHtmlHandlerServlet()), "/GetPrintableHtmlHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.LoadFileBrowserTreeDataHandlerServlet()), "/LoadFileBrowserTreeDataHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.ViewDocumentHandlerServlet()), "/ViewDocumentHandler")
    //  context.addServlet(new ServletHolder(new document.viewer.servlet.UploadFileHandlerServlet()), "/UploadFile")//NO needed
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetHtmlResourcesHandlerServlet()), "/GetHtmlResources/*")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetDocumentPageHtmlHandlerServlet()), "/GetDocumentPageHtmlHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.GetPdfWithPrintDialogServlet()), "/GetPdfWithPrintDialog")
    context.addServlet(new ServletHolder(new document.viewer.servlet.ReorderPageHandlerServlet()), "/ReorderPageHandler")
    context.addServlet(new ServletHolder(new document.viewer.servlet.RotatePageHandlerServlet()), "/RotatePageHandler")
    //TODO check filter
    //  val config = new CORSConfiguration()
    context.addFilter(new FilterHolder(new com.thetransactioncompany.cors.CORSFilter()), "/*", org.mortbay.jetty.Handler.ALL)
    jettyServer.start()
  } else {
    Logger.debug("Not starting Jetty for dev")
  }

  lazy val get: Jetty = {
    applicationLifecycle.addStopHook { () =>
      Future.successful {
        if (useJetty && null != jettyServer) {
          Logger.info("Stopping jetty")
          jettyServer.stop()
        } else {
          Logger.info("No jetty to stop")
        }
      }
    }
    new Jetty {}
  }
}
@Singleton
class JettyProviderTests() extends Provider[Jetty] {
  lazy val get: Jetty = {
    new Jetty {}
  }
}