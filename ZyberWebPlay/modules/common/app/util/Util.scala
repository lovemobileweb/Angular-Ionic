package util

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ Controller, Result }
import zyber.server.dao.Path
import play.api.mvc.ResponseHeader

object Util {
  def pathToResult(path: Path, e: Enumerator[Array[Byte]]): Result = {
    new Controller() {}.Ok.chunked(e)
    .withHeaders(
        "Content-Disposition" -> s"attachment; filename=${path.getName}",
        "Content-Type" -> path.getMimeType)
  }

    def pathToResult(path: Path): Result = {
      new Controller() {}.Ok.chunked(Enumerator.fromStream(path.getInputStream))
      .withHeaders(
          "Content-Disposition" -> s"attachment; filename=${path.getName}",
          "Content-Type" -> path.getMimeType)
    }

  def decode(i: String): String = java.net.URLDecoder.decode(i, "UTF-8")

  implicit class RightBiasedEither[A, B](val e: Either[A, B]) extends AnyVal {
    def foreach[U](f: B => U): Unit = e.right.foreach(f)
    def map[C](f: B => C): Either[A, C] = e.right.map(f)
    def flatMap[C](f: B => Either[A, C]) = e.right.flatMap(f)
  }
  
  def orElseEither[A, B](a: Either[A, B], orElse: Either[A, B]): Either[A, B] = a match{
    case Left(_) => orElse
    case Right(v) => Right(v)
  }
}
