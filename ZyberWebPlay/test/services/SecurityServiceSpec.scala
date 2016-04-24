package services

import org.specs2.mutable.Specification
import play.api.test.WithApplication
import zyber.server.Abilities
import zyber.server.dao.PathSecurityAccessor
import scala.collection.JavaConverters._
import org.specs2.specification.BeforeEach
import zyber.server.dao.UserKeys
import zyber.server.dao.GroupMemberFlat
import zyber.server.dao.GroupMember
import zyber.server.dao.Group
import zyber.server.dao.User
import zyber.server.dao.Principal
import zyber.server.dao.PathSecurity
import zyber.server.dao.Path
import zyber.server.dao.SecurityTypeAccessor
import zyber.server.Permission
import zyber.server.Permissions
import models.PermissionSets
import org.specs2.specification.BeforeAll

class SecurityServiceSpec extends Specification with TestData with BeforeAll {

  def beforeAll = {
    deleteForTestingTenant(zyberSession.getSession, classOf[User])
    deleteForTestingTenant(zyberSession.getSession, classOf[UserKeys])
    deleteForTestingTenant(zyberSession.getSession, classOf[Principal])
    deleteForTestingTenant(zyberSession.getSession, classOf[PathSecurity])
    deleteForTestingTenant(zyberSession.getSession, classOf[Path])
    deleteForTestingTenant(zyberSession.getSession, classOf[Group])
    deleteForTestingTenant(zyberSession.getSession, classOf[Group])
  }

  "SecurityService" should {
    "Add principals to path by email with default viewer permission for sharing" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginService])
      val securityService = app.injector.instanceOf(classOf[SecurityService])
      val fileService = app.injector.instanceOf(classOf[FileService])

      implicit val user = createRandomUser(zus)

      val userPath = fileService.getPathByUUID(user.getHomeFolder).get
      val folder = fileService.createFolder(userPath, "folder").right.get

      val user2 = createRandomUser(zus)
      
      val added = securityService.addPrincipalToPath(folder.getPathId, user2.getEmail, true)
      added must beRight

      val secAccessor = zus.accessor(classOf[PathSecurityAccessor])
      val secForPath = secAccessor.getSecurityForPath(folder.getParentPathId, folder.getPathId).all.asScala

      secForPath.exists { p => p.getPrincipalId.equals(user2.getUserId) } must beTrue
      val secPrinc = secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)
      val secTypeAccessor = zus.accessor(classOf[SecurityTypeAccessor])
      val secTypePrinc = secTypeAccessor.getSecurityType(secPrinc.getSecurityType)

      val permission = new Permission(secTypePrinc.getPermission)

      PermissionSets.viewerValue must be equalTo permission.permission
      PermissionSets.editorValue must not equalTo permission.permission
      PermissionSets.employeeValue must not equalTo permission.permission
      PermissionSets.ownerValue must not equalTo permission.permission

      PermissionSets.viewer.forall(permission.can) must beTrue
      PermissionSets.editor.forall(permission.can) must beFalse
      PermissionSets.employee.forall(permission.can) must beFalse
      PermissionSets.owner.forall(permission.can) must beFalse
    }

    "Remove principals from path" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginService])
      val securityService = app.injector.instanceOf(classOf[SecurityService])
      val fileService = app.injector.instanceOf(classOf[FileService])

      implicit val user = createRandomUser(zus)
      
      val userPath = fileService.getPathByUUID(user.getHomeFolder).get
      val folder = fileService.createFolder(userPath, "folder").right.get

      val user2 = createRandomUser(zus)
      
      val added = securityService.addPrincipalToPath(folder.getPathId, user2.getEmail, true)
      added must beRight

      val secAccessor = zus.accessor(classOf[PathSecurityAccessor])
      var secForPath = secAccessor.getSecurityForPath(folder.getParentPathId, folder.getPathId).all.asScala
      secForPath.exists { p => p.getPrincipalId.equals(user2.getUserId) } must beTrue
      
      securityService.removePrincipalFromPath(folder.getPathId, user2.getUserId) must beRight
      
      secForPath = secAccessor.getSecurityForPath(folder.getParentPathId, folder.getPathId).all.asScala
      secForPath.exists { p => p.getPrincipalId.equals(user2.getUserId) } must beFalse
      Option(secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)) must beNone
    }

    "Update permissions for principal" in new WithApplication(zyberFakeApp) {
      val loginService = app.injector.instanceOf(classOf[LoginService])
      val securityService = app.injector.instanceOf(classOf[SecurityService])
      val fileService = app.injector.instanceOf(classOf[FileService])

      implicit val user = createRandomUser(zus)

      val userPath = fileService.getPathByUUID(user.getHomeFolder).get
      val folder = fileService.createFolder(userPath, "folder").right.get

      val user2 = createRandomUser(zus)

      val added = securityService.addPrincipalToPath(folder.getPathId, user2.getEmail, true)
      added must beRight

      val secAccessor = zus.accessor(classOf[PathSecurityAccessor])
      val secTypeAccessor = zus.accessor(classOf[SecurityTypeAccessor])

      val secForPath = secAccessor.getSecurityForPath(folder.getParentPathId, folder.getPathId).all.asScala
      secForPath.exists { p => p.getPrincipalId.equals(user2.getUserId) } must beTrue

      var secPrinc = secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)
      var secTypePrinc = secTypeAccessor.getSecurityType(secPrinc.getSecurityType)

      //update to editor
      var permission = new Permission(secTypePrinc.getPermission)
      PermissionSets.viewerValue must be equalTo permission.permission

      var newPermission = securityService.getPermissionFor("editor").get

      securityService.setPermissionSetForPrincipal(folder.getPathId, user2.getUserId, newPermission) must beRight

      secPrinc = secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)

      secTypePrinc = secTypeAccessor.getSecurityType(secPrinc.getSecurityType)

      permission = new Permission(secTypePrinc.getPermission)

      PermissionSets.viewerValue must not equalTo permission.permission
      PermissionSets.editorValue must be equalTo permission.permission
      PermissionSets.employeeValue must not equalTo permission.permission
      PermissionSets.ownerValue must not equalTo permission.permission

      PermissionSets.editor.forall(permission.can) must beTrue

      //update to employee
      newPermission = securityService.getPermissionFor("employee").get
      securityService.setPermissionSetForPrincipal(folder.getPathId, user2.getUserId, newPermission) must beRight
      secPrinc = secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)
      secTypePrinc = secTypeAccessor.getSecurityType(secPrinc.getSecurityType)

      permission = new Permission(secTypePrinc.getPermission)

      PermissionSets.viewerValue must not equalTo permission.permission
      PermissionSets.editorValue must not equalTo permission.permission
      PermissionSets.employeeValue must be equalTo permission.permission
      PermissionSets.ownerValue must not equalTo permission.permission

      PermissionSets.employee.forall(permission.can) must beTrue

      //update to owner
      newPermission = securityService.getPermissionFor("owner").get
      securityService.setPermissionSetForPrincipal(folder.getPathId, user2.getUserId, newPermission) must beRight
      secPrinc = secAccessor.getSecurityForPrincipal(folder.getParentPathId, folder.getPathId, user2.getUserId)
      secTypePrinc = secTypeAccessor.getSecurityType(secPrinc.getSecurityType)

      permission = new Permission(secTypePrinc.getPermission)

      PermissionSets.viewerValue must not equalTo permission.permission
      PermissionSets.editorValue must not equalTo permission.permission
      PermissionSets.employeeValue must not equalTo permission.permission
      PermissionSets.ownerValue must be equalTo permission.permission

      PermissionSets.owner.forall(permission.can) must beTrue
    }

  }
}