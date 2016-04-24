package services

import org.mindrot.jbcrypt.BCrypt
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import play.api.test.WithApplication
import zyber.server.Abilities
import zyber.server.dao.{ User, UserAccessor, UserKeys, UserKeysAccessor }
import zyber.server.dao.GroupAccessor
import zyber.server.dao.PrincipalAccessor
import zyber.server.dao.Principal
import play.api.http.Status
import java.util.UUID
import zyber.server.dao.Group
import zyber.server.dao.GroupMemberFlat
import scala.collection.JavaConverters._
import zyber.server.dao.GroupMember
import org.specs2.specification.AfterAll
import zyber.server.dao.GroupMembersAccessor
import zyber.server.dao.GroupMembersFlatAccessor

class LoginServiceSpec extends Specification with TestData with BeforeEach with AfterAll {

  def before = {
    deleteForTestingTenant(zyberSession.getSession, classOf[User])
    deleteForTestingTenant(zyberSession.getSession, classOf[UserKeys])
    deleteForTestingTenant(zyberSession.getSession, classOf[Group])
    deleteForTestingTenant(zyberSession.getSession, classOf[GroupMemberFlat])
    deleteForTestingTenant(zyberSession.getSession, classOf[GroupMember])
  }

  def afterAll = {
    //    before
  }

  "LoginService " should {
    "Get valid user from email" in new WithApplication(zyberFakeApp) {

      createTestUser(zus)

      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      val maybeUser = loginService.getUser(email)
      maybeUser must beSome
      val user = maybeUser.get
      user.getUserId must beEqualTo(userId)

    }

    "Return false on invalid credentials" in new WithApplication(zyberFakeApp) {

      val testUser = createTestUser(zus)

      val uk = createUserKey(zus, testUser)

      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      loginService.checkCredentials("inexistent", "nocreset") must beFalse

    }

    "Return true on valid credentials" in new WithApplication(zyberFakeApp) {

      val testUser = createTestUser(zus)

      val uk = createUserKey(zus, testUser)

      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      loginService.checkCredentials(email, password) must beTrue

    }

    "Create new User given email, password and name" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      val userTry = loginService.createUser(email, password, name, "en", Abilities.DefaultUserRoles.powerUser.getRoleId)

      userTry should beSuccessfulTry

      val testUser = userTry.get

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

    "Create group given a name and create associated principal" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)

      r must beRight

      val fdb = Option(zus.accessor(classOf[GroupAccessor]).getGroupByName(gm))

      fdb must beSome
      val group = fdb.get

      val mp = Option(zus.accessor(classOf[PrincipalAccessor]).getPrincipaById(group.getGroupId))

      mp must beSome

      val princ = mp.get
      princ.getType mustEqual Principal.PrincipalType.Group
    }

    "Not allow creating groups with the same name" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)

      r must beRight

      val r2 = loginService.createGroup(gm)

      r2 must beLeft
      val e = r2.left.get
      e.statusCode mustEqual Status.BAD_REQUEST
    }

    "Update group name for valid group" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)

      r must beRight

      val g = r.right.get

      val nn = "newGroup"

      val r2 = loginService.updateGroup(UUID.randomUUID(), nn)
      r2 must beLeft
      val e = r2.left.get
      e.statusCode mustEqual Status.BAD_REQUEST

      val r3 = loginService.updateGroup(g.getGroupId, nn)
      r3 must beRight

      zus.accessor(classOf[GroupAccessor]).getGroupByName(gm) must beNull
    }

    "Return all created groups" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser
      val n = 5
      val gm = "testGroup"

      for (i <- 0 until n) {
        val r = loginService.createGroup(gm + i)
      }

      val groups = loginService.getGroups

      groups must have size (n)
    }

    "Return valid group by id" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      loginService.getGroup(UUID.randomUUID()) must beNone

      loginService.getGroup(cg.getGroupId) must beSome { (g: Group) => g mustEqual cg }
    }

    "Add group member by name for users and create flattened record" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      val em1 = "u1@a.com"
      val em2 = "u2@a.com"
      val em3 = "u3@a.com"

      val u1 = createTestUser(zus, em1, UUID.randomUUID())
      val u2 = createTestUser(zus, em2, UUID.randomUUID())
      val u3 = createTestUser(zus, em3, UUID.randomUUID())

      val members = Seq(u1, u2, u3)

      val r2 = loginService.addMembers(cg.getGroupId, members.map(_.getEmail))
      r2 must beRight(3)

      val gms = zus.accessor(classOf[GroupMembersAccessor]).getGroupMembers(cg.getGroupId).all.asScala
      gms must have size (3)
      val rgms = gms.map(_.getMemberPrincipalId)
      rgms must containTheSameElementsAs(members.map(_.getUserId))

      val gmfa = zus.accessor(classOf[GroupMembersFlatAccessor])
      val r3 = gmfa.getGroupMembers(cg.getGroupId).all.asScala
      r3 must have size (3)

      val rr3 = r3.map(_.getMemberPrincipalId)
      rr3 must containTheSameElementsAs(members.map(_.getUserId))
    }

    "Add group member by name for groups and create flattened records" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      val em1 = "u1@a.com"
      val em2 = "u2@a.com"
      val em3 = "u3@a.com"
      val em4 = "u4@a.com"

      val u1 = createTestUser(zus, em1, UUID.randomUUID())
      val u2 = createTestUser(zus, em2, UUID.randomUUID())
      val u3 = createTestUser(zus, em3, UUID.randomUUID())
      val u4 = createTestUser(zus, em4, UUID.randomUUID())

      val members = Seq(u1, u2)

      val r2 = loginService.addMembers(cg.getGroupId, members.map(_.getEmail))
      r2 must beRight(2)

      val cgn = "compositeGroup"
      val rcg = loginService.createGroup(cgn)
      val cg2 = rcg.right.get

      val r3 = loginService.addMembers(cg2.getGroupId, Seq(u4.getEmail, gm))
      r3 must beRight(2)

      val gms = zus.accessor(classOf[GroupMembersAccessor]).getGroupMembers(cg2.getGroupId).all.asScala
      gms must have size (2)
      val rgms = gms.map(_.getMemberPrincipalId)
      rgms must containTheSameElementsAs(Seq(cg.getGroupId, u4.getUserId))

      val gmfa = zus.accessor(classOf[GroupMembersFlatAccessor])
      val r4 = gmfa.getGroupMembers(cg2.getGroupId).all.asScala
      r4 must have size (3)
      //
      val rr4 = r4.map(_.getMemberPrincipalId)
      rr4 must containTheSameElementsAs(Seq(u1, u2, u4).map(_.getUserId))

    }

    "Return all group members" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      val em1 = "u1@a.com"
      val em2 = "u2@a.com"
      val em3 = "u3@a.com"
      val em4 = "u4@a.com"

      val u1 = createTestUser(zus, em1, UUID.randomUUID())
      val u2 = createTestUser(zus, em2, UUID.randomUUID())
      val u3 = createTestUser(zus, em3, UUID.randomUUID())
      val u4 = createTestUser(zus, em4, UUID.randomUUID())

      val members = Seq(u1, u2, u3)

      val r2 = loginService.addMembers(cg.getGroupId, members.map(_.getEmail))
      r2 must beRight(3)

      val cgn = "compositeGroup"
      val rcg = loginService.createGroup(cgn)
      val cg2 = rcg.right.get

      val r3 = loginService.addMembers(cg2.getGroupId, Seq(u4.getEmail, gm))
      r3 must beRight(2)

      val res = loginService.getGroupMembers(cg2.getGroupId)
      res must have size (2)

      val rres = res.map(_.getMemberPrincipalId)
      rres must containTheSameElementsAs(Seq(u4.getUserId, cg.getGroupId))
    }

    "Remove member and flattened records" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      val em1 = "u1@a.com"
      val em2 = "u2@a.com"
      val em3 = "u3@a.com"
      val em4 = "u4@a.com"

      val u1 = createTestUser(zus, em1, UUID.randomUUID())
      val u2 = createTestUser(zus, em2, UUID.randomUUID())
      val u3 = createTestUser(zus, em3, UUID.randomUUID())
      val u4 = createTestUser(zus, em4, UUID.randomUUID())

      val members = Seq(u1, u2, u3)

      val r2 = loginService.addMembers(cg.getGroupId, members.map(_.getEmail))
      r2 must beRight(3)

      val cgn = "compositeGroup"
      val rcg = loginService.createGroup(cgn)
      val cg2 = rcg.right.get

      val r3 = loginService.addMembers(cg2.getGroupId, Seq(u4.getEmail, gm))
      r3 must beRight(2)

      val res = loginService.getGroupMembers(cg2.getGroupId)
      res must have size (2)

      val rres = res.map(_.getMemberPrincipalId)
      rres must containTheSameElementsAs(Seq(u4.getUserId, cg.getGroupId))

      val res2 = loginService.removeMember(cg2.getGroupId, u4.getUserId)
      res2 must beRight

      val gms = zus.accessor(classOf[GroupMembersAccessor]).getGroupMembers(cg2.getGroupId).all.asScala
      gms must have size (1)
      val rgms = gms.map(_.getMemberPrincipalId)
      rgms must containTheSameElementsAs(Seq(cg.getGroupId))

      val gmfa = zus.accessor(classOf[GroupMembersFlatAccessor])
      val r4 = gmfa.getGroupMembers(cg2.getGroupId).all.asScala
      r4 must have size (3)
      //
      val rr4 = r4.map(_.getMemberPrincipalId)
      rr4 must containTheSameElementsAs(Seq(u1, u2, u3).map(_.getUserId))
    }

    "Remove group and flattened records" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginServiceImp])

      implicit val user = zus.getUser

      val gm = "testGroup"
      val r = loginService.createGroup(gm)
      val cg = r.right.get

      val em1 = "u1@a.com"
      val em2 = "u2@a.com"
      val em3 = "u3@a.com"
      val em4 = "u4@a.com"

      val u1 = createTestUser(zus, em1, UUID.randomUUID())
      val u2 = createTestUser(zus, em2, UUID.randomUUID())
      val u3 = createTestUser(zus, em3, UUID.randomUUID())
      val u4 = createTestUser(zus, em4, UUID.randomUUID())

      val members = Seq(u1, u2, u3)

      val r2 = loginService.addMembers(cg.getGroupId, members.map(_.getEmail))
      r2 must beRight(3)

      val cgn = "compositeGroup"
      val rcg = loginService.createGroup(cgn)
      val cg2 = rcg.right.get

      val r3 = loginService.addMembers(cg2.getGroupId, Seq(u4.getEmail, gm))
      r3 must beRight(2)

      val res = loginService.getGroupMembers(cg2.getGroupId)
      res must have size (2)

      val rres = res.map(_.getMemberPrincipalId)
      rres must containTheSameElementsAs(Seq(u4.getUserId, cg.getGroupId))

      val res2 = loginService.deleteGroup(cg2.getGroupId)
      res2 must beRight
      
      loginService.getGroup(cg2.getGroupId) must beNone

      val gms = zus.accessor(classOf[GroupMembersAccessor]).getGroupMembers(cg2.getGroupId).all.asScala
      gms must be empty
      
      val gmfa = zus.accessor(classOf[GroupMembersFlatAccessor])
      val r4 = gmfa.getGroupMembers(cg2.getGroupId).all.asScala
      r4 must be empty
      
    }
  }

}