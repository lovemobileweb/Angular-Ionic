package core

import java.io.OutputStream

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Cont, Done, Input, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart
import play.core.parsers.Multipart.PartHandler
import zyber.server.dao.{HasPathOutputStream, Path}

import scala.collection.mutable.ArrayBuffer
import scala.{Left, Right}

//Taken from https://github.com/heiflo/play21-file-upload-streaming
case class StreamingSuccess(filename: String, path: Option[Path])
case class StreamingError(errorMessage: String)

case class ChunkedStreamingSuccess(bytes: Array[Byte])


object StreamingBodyParser extends Controller {

  def streamingBodyParser(streamConstructor: => RequestHeader => String => Option[HasPathOutputStream]) = BodyParser { request =>
    // Use Play's existing multipart parser from play.api.mvc.BodyParsers.
    // The RequestHeader object is wrapped here so it can be accessed in streamingFilePartHandler
    parse.multipartFormData(
      new StreamingBodyParser(streamConstructor(request)).streamingFilePartHandler(request),
      maxLength = 50000 * 1024 * 1024).apply(request)
  }
}

//class RawStreamingBodyParser(outputStream: Option[OutputStream]) {
class RawStreamingBodyParser(f: RequestHeader => Option[OutputStream]) {

  def bodyParser = {
    BodyParser { requestHeader =>
      {
        val outputStream = f(requestHeader)

        def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A] = {
          def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {
            case Input.EOF   => Done(s, Input.EOF)
            case Input.Empty => Cont[E, A](i => step(s)(i))
            case Input.El(e) => {
              val s1 = f(s, e)
              Cont[E, A](i => step(s1)(i))
            }
          }
          Cont[E, A](i => step(state)(i))
        }

        val x = fold[Array[Byte], Option[OutputStream]](outputStream) { (os, data) =>
          os foreach {
            _.write(data)
          }
          os
        }.map { os =>
          os foreach {
            _.close
          }
          Logger.info(s"finished streaming.")
          Left(Results.Status(200))
        }
        x

      }
    }
  }

}

class StreamingBodyParser(streamConstructor: String => Option[HasPathOutputStream]) {

  /**
   * Custom implementation of a PartHandler, inspired by these Play mailing list threads:
   * https://groups.google.com/forum/#!searchin/play-framework/PartHandler/play-framework/WY548Je8VB0/dJkj3arlBigJ
   * https://groups.google.com/forum/#!searchin/play-framework/PartHandler/play-framework/n7yF6wNBL_s/wBPFHBBiKUwJ
   */
  def streamingFilePartHandler(request: RequestHeader): PartHandler[FilePart[Either[StreamingError, StreamingSuccess]]] = {
    Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        // Reference to hold the error message
        var errorMsg: Option[StreamingError] = None

        /* Create the output stream. If something goes wrong while trying to instantiate the output stream, assign the
             error message to the result reference, e.g. `result = Some(StreamingError("network error"))`
             and set the outputStream reference to `None`; the `Iteratee` will then do nothing and the error message will
             be passed to the `Action`. */
        val pathWithOutputStream: Option[HasPathOutputStream] = try {
          streamConstructor(filename)
        } catch {
          case e: Exception => {
            Logger.error(e.getMessage)
            Logger.error("Exception:", e)
            errorMsg = Some(StreamingError(e.getMessage))
            None
          }
        }

        // The fold method that actually does the parsing of the multipart file part.
        // Type A is expected to be Option[OutputStream]
        def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A] = {
          def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {
            case Input.EOF   => Done(s, Input.EOF)
            case Input.Empty => Cont[E, A](i => step(s)(i))
            case Input.El(e) => {
              val s1 = f(s, e)
              errorMsg match { // if an error occurred during output stream initialisation, set Iteratee to Done
                case Some(result) => Done(s, Input.EOF)
                case None         => Cont[E, A](i => step(s1)(i))
              }
            }
          }
          (Cont[E, A](i => step(state)(i)))
        }

        fold[Array[Byte], Option[HasPathOutputStream]](pathWithOutputStream) { (os, data) =>
          os foreach { pwos =>
            pwos.write(data)
          }
          os
        }.map { os =>
          os foreach { _.close }
          errorMsg match {
            case Some(result) =>
              Logger.error(s"Streaming the file $filename failed: ${result.errorMessage}")
              Left(result)

            case None =>
              Logger.info(s"$filename finished streaming.")
              Right(StreamingSuccess(filename, os.flatMap(x => Option(x.getPath))))
          }
        }
    }
  }
}

object StreamingChunksBodyParser {

  def chunksBodyParser = BodyParser { request =>
    // Use Play's existing multipart parser from play.api.mvc.BodyParsers.
    // The RequestHeader object is wrapped here so it can be accessed in streamingFilePartHandler
    parse.multipartFormData(chunksFilePartHandler(request)).apply(request)
  }

  def chunksFilePartHandler(request: RequestHeader): PartHandler[FilePart[Either[StreamingError, ChunkedStreamingSuccess]]] = {
    Multipart.handleFilePart {
      case Multipart.FileInfo(partName, filename, contentType) =>
        
        // Reference to hold the error message
        var errorMsg: Option[StreamingError] = None


        // The fold method that actually does the parsing of the multipart file part.
        // Type A is expected to be Option[OutputStream]
        def fold[E, A](state: A)(f: (A, E) => A): Iteratee[E, A] = {
          def step(s: A)(i: Input[E]): Iteratee[E, A] = i match {
            case Input.EOF   => Done(s, Input.EOF)
            case Input.Empty => Cont[E, A](i => step(s)(i))
            case Input.El(e) => {
              val s1 = f(s, e)
              errorMsg match { // if an error occurred during output stream initialisation, set Iteratee to Done
                case Some(result) => Done(s, Input.EOF)
                case None         => Cont[E, A](i => step(s1)(i))
              }
            }
          }
          (Cont[E, A](i => step(state)(i)))
        }

        fold[Array[Byte], ArrayBuffer[Byte]](ArrayBuffer.empty[Byte]) { (os, data) =>
          os ++= data
        }.map { os =>
          errorMsg match {
            case Some(result) =>
              Logger.error(s"Streaming the file $filename failed: ${result.errorMessage}")
              Left(result)
            case None =>
              Logger.info(s"$filename finished streaming. ${os.size}")
              Right(ChunkedStreamingSuccess(os.toArray))
          }
        }
    }
  }
}