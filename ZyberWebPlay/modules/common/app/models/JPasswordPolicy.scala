package models

case class JPasswordPolicy(lowercase:Option[Boolean],uppercase:Option[Boolean],
                           symbols:Option[Boolean],noUsername:Option[Boolean],
                           numeric:Option[Boolean],maximum:Option[Int],
                           minimum:Option[Int],noReuse:Option[Boolean],expiryDays:Option[Int]
                           ,twoFactorEnabled:Option[Boolean],twoFactorMandatory:Option[Boolean]
                          ,authyKey:Option[String]
                          )
