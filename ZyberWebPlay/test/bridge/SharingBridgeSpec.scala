package bridge

import exceptions.BadRequestException
import models.JPath
import models.extra.SharingSubmission
import org.specs2.mutable.Specification
import services.{ LoginServiceImp, ActivityServiceImpl, FileServiceImpl }
import zyber.server.ZyberTestSession
import services.DBTest
import play.api.test.WithApplication
import services.TrashServiceImpl
import play.api.inject.guice.GuiceApplicationBuilder
import zyber.server.ZyberSession
import play.api.inject.bind
import services.LoginService
import zyber.server.Abilities

class SharingBridgeSpec extends Specification with DBTest {


  "Sharing bridge" should {
    "Sharing files" in new WithApplication(zyberFakeApp) {

      implicit val user = zus.user

//      val loginService = new LoginServiceImp(zyberSession, messagesApi)
      val loginService = app.injector.instanceOf(classOf[LoginService])

//      val activityService = new ActivityServiceImpl(zyberSession, loginService)
//      val fileService: FileServiceImpl = new FileServiceImpl(zyberSession, activityService,
//        new TrashServiceImpl(zyberSession, fileService), messagesApi)
      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val user2 = loginService.createUser("aaa@bbbb.com", "foo", "bar", "en", Abilities.DefaultUserRoles.powerUser.getRoleId).get

      val bridge = new SharingBridge(fileService, loginService)

      val path = fileService.getOSPath("tmp", user, "").get.getPath

      //Test the default sharing is nothing
      val shares: JPath = JPath.fromPathWithShares(path, fileService, loginService)
      shares.sharing shouldEqual ""

      //Test public
      val public = SharingSubmission("public", None, None)

      bridge.setSharesFor(path, public, user)
      val publicShare = JPath.fromPathWithShares(path, fileService, loginService)
      publicShare.sharing shouldEqual "public"

      val originalId = path.getShareId
      //Test revoke
      val revoke = SharingSubmission("revoke", None, None)
      bridge.setSharesFor(path, revoke, user)
      val revokedShare = JPath.fromPathWithShares(path, fileService, loginService)
      revokedShare.sharing shouldEqual ""
      revokedShare.shareId mustNotEqual originalId.toString

      //Test password
      val password = SharingSubmission("password", Some("password"), None)
      bridge.setSharesFor(path, password, user)
      val passShare = JPath.fromPathWithShares(path, fileService, loginService)
      passShare.sharing shouldEqual "password"

      //Test missing password
      bridge.setSharesFor(path, SharingSubmission("password", None, None), user) must throwA[BadRequestException]

      //Test users
      bridge.setSharesFor(path, SharingSubmission("users", None, Some(List(user2).map(_.getEmail).mkString)), user)
      val userShare = JPath.fromPathWithShares(path, fileService, loginService, List(user))
      userShare.sharing shouldEqual "users"
      userShare.shares.size mustEqual 1
      userShare.shares.head.userEmail.get mustEqual user2.getEmail
    }
  }
}

