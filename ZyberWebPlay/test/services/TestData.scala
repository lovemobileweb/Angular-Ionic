package services

import java.util.Date
import java.util.UUID

import org.mindrot.jbcrypt.BCrypt

import play.api.i18n.DefaultLangs
import play.api.i18n.DefaultMessagesApi
import zyber.server.ZyberUserSession
import zyber.server.dao.Principal
import zyber.server.dao.Principal.PrincipalType
import zyber.server.dao.User
import zyber.server.dao.UserKeys

trait TestData extends DBTest{

  val email = "zyber5@zyber.com"
  val userId = UUID.randomUUID()
  val password = "s3cret"
  
  val name = "new user"
  
  def createRandomUser(zus: ZyberUserSession): User = {
    val email = new Date().getTime().toHexString
    val testUser = createTestUser(zus, email, UUID.randomUUID)
    
    createUserKey(zus, testUser, new Date().getTime().toHexString)
    
    testUser
  }
  
  def createTestUser(zus: ZyberUserSession,
                     email: String = email,
                     userId: UUID = userId,
                     password: String = password): User = {
    val cd = new Date()
    val testUser = new User(userId, email, null, null, cd, "/")
    testUser.setActive(true)
    val mapper = zus.mapper(classOf[User])
    mapper.save(testUser)
    
    val aprinc = new Principal(userId, PrincipalType.User, testUser.getRootPath(zus).getPathId, cd)
    zus.mapper(classOf[Principal]).save(aprinc)
    
    testUser
  }

  def createUserKey(zus: ZyberUserSession,
                    user: User, password: String = password): UserKeys = {
    val userKey = new UserKeys
    userKey.setUserId(user.getUserId)
    userKey.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()))
    userKey.setPasswordHashType("BCrypt")

    val keyMapper = zus.mapper(classOf[UserKeys])
    keyMapper.save(userKey)
    userKey
  }
  
}