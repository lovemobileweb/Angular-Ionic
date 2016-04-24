package controllers

import org.specs2.mutable.Specification
import util.{DefaultMultitenancyHelperImp, MultitenancyHelper}
import zyber.server.ZyberSession
import services.LoginService
import services.LoginServiceImp
import java.util.Date
import play.api.test.WithApplication
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import zyber.server.ZyberTestSession
import services.TestData
import play.api.test.FakeRequest
import play.api.test._
import play.api.test.Helpers._
import org.specs2.specification.BeforeEach
import org.mindrot.jbcrypt.BCrypt
import zyber.server.dao.UserAccessor
import zyber.server.dao.UserKeysAccessor
import zyber.server.dao.User
import zyber.server.dao.UserKeys
import zyber.server.dao.admin.Tenant
import play.api.mvc.RequestHeader
import core.ZyberCacheManagerProvider
import net.sf.ehcache.CacheManager

class LoginControllerSpec extends Specification with TestData with BeforeEach {
  
  val ms = new DefaultMultitenancyHelperImp(zyberSession){
    override def getTenant(request: RequestHeader): Option[String] = Some("")
    override def validTenant(subdomain: String): Option[Tenant] = Some(testingTenant)
  }

  val loginInfo = List(("username", email), ("password", password))

  val accountInfo = List(("email", email), ("password", password), ("name", name))

  def application =
    new GuiceApplicationBuilder().
      overrides(bind[ZyberSession].to(zyberSession)).
      overrides(bind[MultitenancyHelper].to(ms)).
      overrides(bind[CacheManager].toProvider[ZyberCacheManagerProvider]).
      build()

  def before = {
    deleteForTestingTenant(zyberSession.getSession, classOf[User])
    deleteForTestingTenant(zyberSession.getSession, classOf[UserKeys])
  }

  "LoginController" should {

    "Return BadRequest on insufficient data" in new WithApplication(application) {
      val fr = FakeRequest(
        POST, zyberapp.routes.LoginController.createAccount().path()).
        withFormUrlEncodedBody("email" -> email)

      val Some(result) = route(fr)

      status(result) must equalTo(BAD_REQUEST)
    }

    "Not allow creating repeated users" in new WithApplication(application) {
      
      val user = createTestUser(zus)
      createUserKey(zus, user)
      
      val fr = FakeRequest(POST, zyberapp.routes.LoginController.createAccount().path()).withFormUrlEncodedBody(accountInfo: _*)

      val Some(result) = route(fr)

      status(result) must equalTo(BAD_REQUEST)
    }

    "Create new users" in new WithApplication(application) {
      val fr = FakeRequest(POST, zyberapp.routes.LoginController.createAccount().path()).withFormUrlEncodedBody(accountInfo: _*)

      val Some(result) = route(fr)

      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome(zyberapp.routes.Application.index.url)

      val userAccesor = zus.accessor(classOf[UserAccessor])
      val userKeysAccesor = zus.accessor(classOf[UserKeysAccessor])

      val maybeUserDB = Option(userAccesor.getUserByEmail(email))

      maybeUserDB must beSome

      val userDB = maybeUserDB.get
      userDB.getEmail mustEqual email
      userDB.getName mustEqual name

      val maybeUserKeyDB = Option(userKeysAccesor.getUserKeysByUserId(userDB.getUserId))
      maybeUserKeyDB must beSome

      val userKeyDB = maybeUserKeyDB.get

      BCrypt.checkpw(password, userKeyDB.getPasswordHash) must beTrue
    }

    "Authenticate valid users" in new WithApplication(application) {
      val user = createTestUser(zus)
      createUserKey(zus, user, password)

      val fr = FakeRequest(POST, zyberapp.routes.LoginController.authenticate().path()).withFormUrlEncodedBody(loginInfo: _*)
      val maybeResult = route(fr)

      maybeResult must beSome
      val result = maybeResult.get
      status(result) must equalTo(SEE_OTHER)
      redirectLocation(result) must beSome(zyberapp.routes.HomeController.home("").url)
    }

  }
}