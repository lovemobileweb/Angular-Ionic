package exceptions

import java.util.UUID

class NoSuchUUIDException(uuid:UUID) extends RuntimeException {
  override def getMessage: String = s"${super.getMessage} : $uuid "
}
