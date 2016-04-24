package binders

import java.util.UUID

import play.api.mvc.PathBindable

object Binders {
  implicit def uuidPathBindable() = new PathBindable[UUID] {
    def bind(key: String, value: String): Either[String, UUID] = {
      try {
        Right(UUID.fromString(value))
      } catch {
        case e: Exception => Left(e.getMessage)
      }
    }

    def unbind(key: String, article: UUID): String = {
      article.toString
    }
  }

}

