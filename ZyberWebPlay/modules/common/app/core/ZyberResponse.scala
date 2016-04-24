package core

import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.{Result, Results}

case class ApiError(message: String, userMessage: String,
                    statusCode: Int, context: Option[String] = None)

case class ApiErrors(errors: Seq[ApiError]) {
  def statusCode = errors.map(_.statusCode).max

  def mainError = errors.maxBy { x => x.statusCode }
  def ++(other: ApiErrors): ApiErrors = {
    ApiErrors(errors ++ other.errors)
  }
}

case class WrappedResult[T](t: T, message: String)

case class OnsuccessResult[T](t: T, f: Result => Result)

object ApiErrors {
  def single(message: String, userMessage: String,
             statusCode: Int, context: Option[String] = None) =
    ApiErrors(List(ApiError(message, userMessage, statusCode, context)))

  def fromException(e: Throwable)(implicit messages: Messages) =
    single(e.getMessage, Messages("internal_error"), Status.INTERNAL_SERVER_ERROR)
}

object ZyberResponse extends Results {
  implicit val apiWrites = Json.writes[ApiError]

  type ApiResponse[T] = Either[ApiErrors, T]

  type ApiResponseMsg[T] = Either[ApiErrors, WrappedResult[T]]

  type ApiResponseRunSuccess[T] = Either[ApiErrors, OnsuccessResult[T]]

  def apply[T](block: => ApiResponse[T])(implicit tjs: Writes[T]): Result = {
    try {
      block match {
        case Left(apiErrors) =>
          errorResult(apiErrors)
        case Right(t) =>
          Ok {
            JsObject(Seq(
              "status" -> JsString("SUCCESS"),
              "response" -> Json.toJson(t)))
          }
      }
    } catch {
      case e: Exception =>
        Logger.error("Uncaught exception", e)
        errorResult(ApiErrors.single(e.getMessage, e.getMessage, play.api.http.Status.INTERNAL_SERVER_ERROR))
    }
  }

  def afterResult[T](block: => ApiResponseRunSuccess[T])(implicit tjs: Writes[T]): Result = {
    try {
      block match {
        case Left(apiErrors) =>
          errorResult(apiErrors)
        case Right(ros) =>
          ros.f(Ok {
            JsObject(Seq(
              "status" -> JsString("SUCCESS"),
              "response" -> Json.toJson(ros.t)))
          })
      }
    } catch {
      case e: Exception =>
        Logger.error("Uncaught exception", e)
        errorResult(ApiErrors.single(e.getMessage, e.getMessage, play.api.http.Status.INTERNAL_SERVER_ERROR))
    }
  }

  def withMessage[T](block: => ApiResponseMsg[T])(implicit tjs: Writes[T]): Result = {
    try {
      block match {
        case Left(apiErrors) =>
          errorResult(apiErrors)
        case Right(res) =>
          Ok {
            JsObject(Seq(
              "status" -> JsString("SUCCESS"),
              "message" -> JsString(res.message),
              "response" -> Json.toJson(res.t)))
          }
      }
    } catch {
      case e: Exception =>
        Logger.error("Uncaught exception", e)
        errorResult(ApiErrors.single(e.getMessage, e.getMessage, play.api.http.Status.INTERNAL_SERVER_ERROR))
    }
  }

  def errorResult(apiErrors: ApiErrors): Result = {
    Status(apiErrors.statusCode) {
      JsObject(Seq(
        "status" -> JsString("ERROR"),
        "statusCode" -> JsNumber(apiErrors.statusCode),
        "errors" -> Json.toJson(apiErrors.errors),
        //        "firstError" -> Json.toJson(apiErrors.errors.headOption)))
        "firstError" -> Json.toJson(apiErrors.mainError)))
    }
  }

  def jsonToResponse[A, B](j: play.api.libs.json.JsResult[A]): Either[ApiErrors, A] =
    j.fold(
      errors => Left(ApiErrors(errors.map {
        case (jspath, validation) => ApiError("",
          validation.map(_.message).mkString(","), BAD_REQUEST)
      })),
      value => Right(value))
      
  def jsonToResponseWithDefault[A, B](j: play.api.libs.json.JsResult[A], d: A): Either[ApiErrors, A] =
    j.fold(
      errors => Right(d),
      value => Right(value))
}      