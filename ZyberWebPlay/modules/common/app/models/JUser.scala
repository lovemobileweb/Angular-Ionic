package models

import zyber.server.dao.User

case class JUser(
    uuid: String,
    name: String,
    email: String,
    twoFactor:Boolean,
    countryCode:String,
    phoneNumber:String,
    userRole: String,
    language: Option[String] = Some("en")
    )
    
object JUser{
  def fromUser(user: User) = {
    JUser(
        user.getUserId.toString(),
        user.getName, 
        user.getEmail,
        user.isRequireTwoFactor,
        user.getCountryCode,
        user.getPhoneNumber,
        user.getUserRole.toString(),
        Option(user.getLanguage)
      )
  }
}