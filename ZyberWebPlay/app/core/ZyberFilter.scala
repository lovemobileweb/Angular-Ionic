package core

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

class ZyberFilters @Inject() (
    gzipFilter: GzipFilter) extends HttpFilters {
  def filters = Seq(gzipFilter, new LoggingFilter())
}

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class LoggingFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>

      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.debug(s"${requestHeader.method} ${requestHeader.uri} " +
        s"took ${requestTime}ms and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString,
          //TODO play cors config is not working, adding headers here:
          "Access-Control-Allow-Origin" -> "*",
          "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS, PUT, DELETE",
          "Access-Control-Allow-Headers" -> "x-requested-with,content-type,Cache-Control,Pragma,Date, bearer",
          "Access-Control-Allow-Credentials" -> "true"
          )
    }
  }
}

