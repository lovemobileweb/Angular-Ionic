package services

class PasswordReuseException extends RuntimeException {
  override def getMessage: String = "Cannot reuse an existing password"
}
