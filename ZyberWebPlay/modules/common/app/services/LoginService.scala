package services

import java.util.Date
import java.util.UUID
import com.authy.AuthyApiClient
import com.authy.api.Params
import controllers.admin.Utils
import models.JPasswordPolicy
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.passay._

import scala.collection.JavaConverters._
import scala.util.Try
import org.mindrot.jbcrypt.BCrypt
import com.google.inject.ImplementedBy
import core.ApiErrors
import exceptions.NoSuchUUIDException
import javax.inject.Inject
import play.api.Logger
import play.api.cache.CacheApi
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import zyber.server.Abilities
import zyber.server.CassandraMapperDelegate
import zyber.server.ZyberSession
import zyber.server.ZyberUserSession
import zyber.server.dao._

@ImplementedBy(classOf[LoginServiceImp])
trait LoginService {
  def expire(implicit user: User, expired: Boolean)

  def findById(uuid: UUID)(implicit u: User): User

  def getUser(email: String)(implicit tenantId: UUID): Option[User]

  def getUserById(uuid: UUID)(implicit u: User): Option[User]
  def getUserImplicitTenantById(uuid: UUID)(implicit tenantId: UUID): Option[User]

  def checkCredentials(email: String, password: String)(implicit tenantId: UUID): Boolean

  def checkPassword(user: User, password: String)(implicit u: User): Boolean

  def createUser(email: String, password: String, name: String,
                 lang: String, userRoleId: UUID, countryCode: String = "", phoneNumber: String = "")(implicit tenantId: UUID): Try[User]

  def createUserApi(email: String, password: String,
                    name: String, lang: String, userRoleId: UUID, countryCode: String = "", phoneNumber: String = "")(implicit tenantId: UUID): Either[ApiErrors, User]

  def getUsers(implicit u: User): Seq[User]

  def getActiveUsers(implicit u: User): Seq[User]

  def deleteUser(uuid: UUID)(implicit user: User): Either[ApiErrors, Unit]

  def changePassword(user: User, newPassword: String)(implicit u: User): Try[Unit]

  def getGroups(implicit u: User): Seq[Group]

  //  def getUserGroups()(implicit u: User): Seq[Group]

  def createGroup(name: String)(implicit u: User): Either[ApiErrors, Group]

  def getGroup(groupId: UUID)(implicit u: User): Option[Group]

  def getGroupMembers(groupId: UUID)(implicit u: User): Seq[GroupMember]

  def addMembers(groupId: UUID, members: Seq[String])(implicit user: User): Either[ApiErrors, Int]

  def removeMember(groupId: UUID, memberId: UUID)(implicit u: User): Either[ApiErrors, Unit]

  def deleteGroup(groupId: UUID)(implicit u: User): Either[ApiErrors, Unit]

  def updateGroup(groupId: UUID, newName: String)(implicit u: User): Either[ApiErrors, Unit]

  def updateUser(uuid: UUID)(name: String, email: String,
                             lang: String, userRoleId: Option[UUID],
                             resetPassword: Option[String], countryCode: String = "", phoneNumber: String = "")(implicit user: User): Either[ApiErrors, Unit]

  def canAccessFilesFolders(implicit u: User): Either[ApiErrors, Unit]

  def canCreateUsers(implicit u: User): Either[ApiErrors, Unit]

  def canCreateAdmin(implicit u: User): Either[ApiErrors, Unit]

  def canCreateGroups(implicit u: User): Either[ApiErrors, Unit]

  def canResetPasswords(implicit u: User): Either[ApiErrors, Unit]

  def canViewActivy(implicit u: User): Either[ApiErrors, Unit]

  def canManageTermstore(implicit u: User): Either[ApiErrors, Unit]

  def canManageAbilities(implicit u: User): Either[ApiErrors, Unit]

  def checkEmail(user: User, email: String, tenantId: UUID): Either[ApiErrors, Unit]

  def getUserRole(user: User): Option[UserRole]

  def userRolesCache(implicit user: User): Map[UUID, UserRole]

  def getUserRoles(implicit user: User): List[UserRole]

  def getPrincipalByNameLike(name: String)(implicit user: User): Seq[Principal]
  def getUserByNameLike(name: String)(implicit user: User): Seq[User]

  def getRootPath(implicit user:User):Path

  //TODO move this to another service
  def sendSMS(implicit user: User): Try[Unit]
  def checkSMS(implicit user: User, token: String): Try[Boolean]
}

class LoginServiceImp @Inject() (
    val session: ZyberSession,
    val messagesApi: MessagesApi,
    implicit val metadataService: MetadataService,
    val securityService: SecurityService,
    cache: CacheApi) extends LoginService with I18nSupport with MultitenancySupport {

  import play.api.http.Status._

  override def expire(implicit user: User, expired: Boolean): Unit = {
    //    user.setPasswordExpired(expired)
    mapper.save(user)
  }

  def getRootPath(implicit user:User):Path = {
    user.getRootPath(new ZyberUserSession(session,user))
  }


  def principalMapper(implicit user: User): CassandraMapperDelegate[Principal] =
    userSession.mapper(classOf[Principal])
  def principalAccessor(implicit user: User): PrincipalAccessor =
    userSession.accessor(classOf[PrincipalAccessor])

  def groupMapper(implicit user: User): CassandraMapperDelegate[Group] =
    userSession.mapper(classOf[Group])
  def groupAccessor(implicit user: User): GroupAccessor =
    userSession.accessor(classOf[GroupAccessor])

  def groupMemberMapper(implicit user: User): CassandraMapperDelegate[GroupMember] =
    userSession.mapper(classOf[GroupMember])
  def groupMemberAccessor(implicit user: User): GroupMembersAccessor =
    userSession.accessor(classOf[GroupMembersAccessor])

  def mapper(implicit user: User): CassandraMapperDelegate[User] =
    userSession.mapper(classOf[User])

  def userAccessor(implicit user: User): UserAccessor =
    userSession.accessor(classOf[UserAccessor])

  def userKeysAccessor(implicit user: User): UserKeysAccessor =
    userSession.accessor(classOf[UserKeysAccessor])

  def passwordAccessor(implicit user: User): PasswordHistoryAccessor =
    userSession.accessor(classOf[PasswordHistoryAccessor])

  //GroupMembersAccessor
  def getUser(email: String)(implicit tenantId: UUID): Option[User] = {
    Option(
      tenantSession.accessor(classOf[UserAccessor])
        .getUserByEmail(email))
      .map { u =>
        u.setTenantId(tenantId)
        u
      }
  }

  override def findById(uuid: UUID)(implicit user: User): User = Option(userAccessor.getById(uuid)).getOrElse(throw new NoSuchUUIDException(uuid))

  def checkCredentials(email: String, password: String)(implicit tenantId: UUID): Boolean = {
    getUser(email)
      .filter(u =>
        u.getActive)
      .flatMap { u =>
        Option(tenantSession.accessor(classOf[UserKeysAccessor])
          .getUserKeysByUserId(u.getUserId))
      }.exists(uk => {
        BCrypt.checkpw(password, uk.getPasswordHash)
      })
  }

  def checkPassword(user: User, password: String)(implicit u: User): Boolean = {
    Option(userKeysAccessor.getUserKeysByUserId(user.getUserId))
      .exists(uk => {
        BCrypt.checkpw(password, uk.getPasswordHash)
      })
  }

  def createUser(email: String, password: String,
                 name: String, lang: String, userRoleId: UUID, countryCode: String = "", phoneNumber: String = "")(implicit tenantId: UUID): Try[User] = Try {
    doCreateUser(email, password, name, lang, userRoleId)
  }

  private def doCreateUser(email: String, password: String,
                           name: String, lang: String, userRoleId: UUID, countryCode: String = "", phoneNumber: String = "")(implicit tenantId: UUID) = {
    val userId = UUID.randomUUID()
    val createdDate = new Date()

    implicit val user = new User(userId, email, null, null, createdDate, name)
    user.setActive(true)
    user.setLanguage(lang)
    user.setUserRole(userRoleId)
    user.setTenantId(tenantId)

    checkPassword(user, password, Utils.getPasswordPolicy)

    val mapper = tenantSession.mapper(classOf[User])
    syncWithAuthy(email, countryCode, phoneNumber, user)
    setPasswordExpiry(user, Utils.getPasswordPolicy)
    syncWithAuthy(email, countryCode, phoneNumber, user)
    mapper.save(user)

    val userKey = new UserKeys
    userKey.setUserId(user.getUserId)
    userKey.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()))
    userKey.setPasswordHashType("BCrypt")
    savePasswordHistory(userKey.getPasswordHash, userKey.getPasswordHashType)

    val keyMapper = tenantSession.mapper(classOf[UserKeys])
    keyMapper.save(userKey)

    val principal = new Principal(
      userId, Principal.PrincipalType.User,
      null, createdDate)

    principal.setDisplayName(email)

    tenantSession.mapper(classOf[Principal]).save(principal)

    val zus = new ZyberUserSession(session, user)
    val userFolder = user.getRootPath(zus)
    securityService.setOwnerPermissionsFor(userFolder, user.getUserId)(user).fold(err => Logger.info("Error" + err), identity)
    securityService.setPermissionsForShares(user.getUserId)(user).fold(err => Logger.info("Error" + err), identity)
    user
  }

  def syncWithAuthy(email: String, countryCode: String, phoneNumber: String, user: User): Unit = {
    implicit val iUser = user
    if (StringUtils.isNotBlank(phoneNumber)) {
      val client: AuthyApiClient = makeAuthyClient
      //Create will override for update.
      if (phoneNumber != user.getPhoneNumber && countryCode != user.getCountryCode) {
        if (Option(user.getAuthyID).isDefined) {
          client.getUsers.deleteUser(user.getAuthyID)
        }
        val created = client.getUsers.createUser(email, phoneNumber, countryCode)
        if (created.getId == 0) throw new RuntimeException("Unable to sync with Authy, is your API key valid?")
        user.setAuthyID(created.getId)
      }
    } else {
      user.setAuthyID(null)
    }
    user.setCountryCode(countryCode)
    user.setPhoneNumber(phoneNumber)
  }

  def makeAuthyClient(implicit user: User): AuthyApiClient = {
    new AuthyApiClient(Utils.getPasswordPolicy.authyKey.get)
  }

  def createUserApi(email: String, password: String,
                    name: String, lang: String, userRoleId: UUID, countryCode: String = "", phoneNumber: String = "")(implicit tenantId: UUID): Either[ApiErrors, User] = {

    try {
      Right(doCreateUser(email, password, name, lang, userRoleId, countryCode, phoneNumber))
    } catch {
      case e: Exception => Left(ApiErrors.fromException(e))
    }
  }

  def getUsers(implicit user: User): Seq[User] =
    userAccessor.getAll.all().asScala

  def getActiveUsers(implicit user: User): Seq[User] =
    userAccessor.getActiveUsers.all().asScala

  def getUserImplicitTenantById(uuid: UUID)(implicit tenantId: UUID): Option[User] = {
    Option(
      tenantSession.accessor(classOf[UserAccessor])
        .getById(uuid))
      .map { u =>
        u.setTenantId(tenantId)
        u
      }
  }

  def getUserById(uuid: UUID)(implicit user: User): Option[User] = {
    Option(userAccessor.getOnePosition(uuid))
  }

  def updateUser(uuid: UUID)(name: String, email: String,
                             lang: String, userRoleId: Option[UUID],
                             resetPassword: Option[String], countryCode: String = "", phoneNumber: String = "")(implicit user: User): Either[ApiErrors, Unit] = {

    try {
      Right {
        getUserById(uuid) foreach { u =>
          u.setName(name)
          u.setEmail(email)
          u.setLanguage(lang)

          userRoleId.foreach { userRole =>
            u.setUserRole(userRole)
          }
          syncWithAuthy(email, countryCode, phoneNumber, u)

          mapper.save(u)
          resetPassword.foreach { pass =>
            changePassword(u, pass).get
          }
          val p = principalAccessor.getPrincipaById(user.getUserId)
          if (null != p) {
            p.setDisplayName(email)
            principalMapper.save(p)
          }
        }
      }
    } catch {
      case e: Exception => Left { ApiErrors.single(e.getMessage, Messages("internal_error"), INTERNAL_SERVER_ERROR) }
    }
  }

  def deleteUser(uuid: UUID)(implicit user: User): Either[ApiErrors, Unit] = {
    try {
      Right {
        userAccessor.deleteUser(uuid)
        val p = principalAccessor.getPrincipaById(user.getUserId)
        if (null != p) {
          p.setActive(false)
          principalMapper.save(p)
        }
      }
    } catch {
      case e: Exception => Left { ApiErrors.fromException(e) }
    }
  }

  //TODO a fair bit of duplication between create and update, should fix
  def changePassword(user: User, newPassword: String)(implicit u: User): Try[Unit] = Try {
    Option(userKeysAccessor.getUserKeysByUserId(user.getUserId)) foreach {
      uk =>
        val policy: JPasswordPolicy = Utils.getPasswordPolicy
        //TODO fix below
        if (policy.noReuse.getOrElse(false)) {
          val toList: List[PasswordHistory] = passwordAccessor.getPasswordHistoryForUser(u.getUserId).all().asScala.toList
          val ok = toList.forall(h => {
            !BCrypt.checkpw(newPassword, h.getPasswordHash)
          })
          if (!ok) {
            throw new PasswordReuseException()
          }
        }
        checkPassword(user, newPassword, policy)
        setPasswordExpiry(user, policy)

        mapper.save(user)

        val hashed: String = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        uk.setPasswordHash(hashed)
        uk.setPasswordHashType("BCrypt")

        savePasswordHistory(hashed, uk.getPasswordHashType)

        val keyMapper = userSession.mapper(classOf[UserKeys])
        keyMapper.save(uk)
    }
  }

  def setPasswordExpiry(user: User, policy: JPasswordPolicy): Unit = {
    val expiryTime = new DateTime().plusDays(policy.expiryDays.getOrElse(365 * 10))
    user.setPasswordExpiry(expiryTime.toDate)
  }

  def checkPassword(user: User, newPassword: String, policy: JPasswordPolicy): Unit = {
    val lengthOption = (policy.minimum, policy.maximum) match {
      case (Some(min), Some(max)) => Some(new LengthRule(min, max))
      case (Some(min), None) => {
        val x = new LengthRule()
        x.setMinimumLength(min)
        Some(x)
      }
      case (None, Some(max)) => {
        val x = new LengthRule()
        x.setMaximumLength(max)
        Some(x)
      }
      case _ => None
    }

    val lower = policy.lowercase.flatMap(x => if (x) Some(new CharacterRule(EnglishCharacterData.LowerCase)) else None)
    val upper = policy.uppercase.flatMap(x => if (x) Some(new CharacterRule(EnglishCharacterData.UpperCase)) else None)
    val digit = policy.numeric.flatMap(x => if (x) Some(new CharacterRule(EnglishCharacterData.Digit)) else None)
    val special = policy.symbols.flatMap(x => if (x) Some(new CharacterRule(EnglishCharacterData.Special)) else None)
    val username = policy.symbols.flatMap(x => if (x) Some(new UsernameRule()) else None)

    val rules: List[Rule] = (lengthOption :: lower :: upper :: digit :: special :: username :: Nil).flatten
    val validator: PasswordValidator = new PasswordValidator(rules.asJava)
    val data: PasswordData = new PasswordData(newPassword)
    data.setUsername(user.getEmail)
    val validate: RuleResult = validator.validate(data)
    if (!validate.isValid) {
      throw new BadPasswordException(validator.getMessages(validate).asScala.mkString(","))
    }

    //Check again with the username instead of email
    data.setUsername(user.getName)
    val validate2: RuleResult = validator.validate(data)
    if (!validate2.isValid) {
      throw new BadPasswordException(validator.getMessages(validate2).asScala.mkString(","))
    }
  }

  def savePasswordHistory(hashed: String, hashType: String)(implicit user: User): Unit = {
    val history = new PasswordHistory()
    history.setUserId(user.getUserId)
    history.setTenantId(user.getTenantId)
    history.setPasswordHash(hashed)
    history.setPasswordHashType(hashType)
    val toSave = userSession.mapper(classOf[PasswordHistory])
    toSave.save(history)
  }

  override def createGroup(name: String)(implicit u: User): Either[ApiErrors, Group] = {
    try {
      if (null != groupAccessor.getGroupByName(name)) { //right now groups are unique
        Left(ApiErrors.single("A group with the same name already exists", Messages("group.exists"), BAD_REQUEST))
      } else Right {
        val groupId = UUID.randomUUID()
        val createdDate = new Date
        val group = new Group(groupId, name, createdDate)
        group.setMembers(0)
        groupMapper.save(group)

        val principal = new Principal(groupId, Principal.PrincipalType.Group,
          null, createdDate)
        principal.setDisplayName(name)
        principalMapper.save(principal)

        group
        //        addUserMembersToGroup(group, createdDate, owner)
      }
    } catch {
      case e: Exception => Left(ApiErrors.fromException(e))
    }
  }

  override def updateGroup(groupId: UUID, newName: String)(implicit u: User): Either[ApiErrors, Unit] = {
    val group = groupAccessor.getGroupById(groupId)
    if (null == group)
      Left(ApiErrors.single("Invalid group", Messages("invalid.group"), BAD_REQUEST))
    //    else if (user.getUserId.equals(group.getOwnerPrincipalId)) Right {
    else Right {
      //groupAccessor.updateName(newName, groupId)
      group.setName(newName)
      groupMapper.save(group)
      val p = principalAccessor.getPrincipaById(groupId)
      if (null != p) {
        p.setDisplayName(newName)
        principalMapper.save(p)
      }
    }
    //    else
    //      Left(Messages("invalid.no_group_owner"))

  }

  def getGroups(implicit user: User): Seq[Group] = {
    groupAccessor.getGroups.all()
      .asScala
  }

  def getGroup(groupId: UUID)(implicit user: User): Option[Group] =
    Option(groupAccessor.getGroupById(groupId))

  def getGroupMembers(groupId: UUID)(implicit user: User): Seq[GroupMember] = {
    val members = groupMemberAccessor.getGroupMembers(groupId)
      .all()
      .asScala

    val (groupMembers, userMembers) = members.partition { gm =>
      val principal = principalAccessor.getPrincipaById(gm.getMemberPrincipalId)
      Principal.PrincipalType.Group.equals(principal.getType)
    }

    val withMaybeGroups = groupMembers.map { gm =>
      (gm, getGroup(gm.getMemberPrincipalId))
    }

    val withMaybeUsers = userMembers.map { gm =>
      (gm, getUserById(gm.getMemberPrincipalId))
    }

    val res = withMaybeGroups.filter(_._2.isDefined).map {
      case (g, p) =>
        g.setName(p.get.getName)
        g
    } ++
      withMaybeUsers.filter(_._2.isDefined).map {
        case (g, p) =>
          g.setName(p.get.getName)
          g
      }
    res
  }

  override def addMembers(groupId: UUID, members: Seq[String])(implicit user: User): Either[ApiErrors, Int] = {
    val maybeGroup = Option(groupAccessor.getGroupById(groupId))
    maybeGroup.map { group =>
      val groupMembersIds = getGroupMembers(groupId).map(_.getMemberPrincipalId)
      implicit val tenantId = user.getTenantId
      val users = members.map(getUser).filter(_.isDefined).map(_.get)
      //      val groups = members.map(groupAccessor.getGroupByName).filter { g =>
      //        null != g && !g.getGroupId.equals(group.getGroupId)
      //      }

      Logger.debug("New users: " + users)
      //      Logger.debug("New groups: " + groups)

      val usersToSave = users.filter { u => !groupMembersIds.contains(u.getUserId) }
      //      val groupsToSave = groups.filter { g => !groupMembersIds.contains(g.getGroupId) }

      val joinedDate = new Date

      val newMembers = usersToSave.size /*+ groupsToSave.size*/

      addUserMembersToGroup(group, joinedDate, usersToSave: _*)
      //      addGroupMembersToGroup(group, joinedDate, groupsToSave: _*)

      group.setMembers(group.getMembers + newMembers)
      groupMapper.save(group)

      Right(newMembers)
    } getOrElse (Left(ApiErrors.single("Invalid group", Messages("invalid.group"), BAD_REQUEST)))
  }

  private def addUserMembersToGroup(group: Group, joinedDate: Date, users: User*)(implicit u: User) = {
    val gmfm = userSession.mapper(classOf[GroupMemberFlat])
    for (user <- users) {
      val member = new GroupMember(group.getGroupId, user.getUserId, joinedDate)
      //      member.setMemberPrincipalType(Principal.PrincipalType.User)
      groupMemberMapper.save(member)
      val gmf = new GroupMemberFlat(group.getGroupId, user.getUserId, Principal.PrincipalType.User, group.getGroupId)
      gmfm.save(gmf)
      //      updateDependentGroups(member, gmf)
    }
  }

  //  private def updateDependentGroups(gm: GroupMember, gmf: GroupMemberFlat)(implicit u: User): Unit = {
  //    val gmfm = userSession.mapper(classOf[GroupMemberFlat])
  //    val dependent = groupMemberAccessor.getGroupMembersByPrincipal(gm.getGroupId).all.asScala
  //
  //    dependent.foreach { x =>
  //      gmf.setGroupId(x.getGroupId)
  //      gmfm.save(gmf)
  //      updateDependentGroups(x, gmf)
  //    }
  //  }

  //  private def addGroupMembersToGroup(group: Group, joinedDate: Date, groups: Group*)(implicit user: User) = {
  //    for (memberGroup <- groups) {
  //      val member = new GroupMember(group.getGroupId, memberGroup.getGroupId, joinedDate)
  ////      member.setMemberPrincipalType(Principal.PrincipalType.Group)
  //      member.setTenantId(user.getTenantId)
  //      member.setZus(userSession)
  ////      member.saveAndUpdateFlatMembership(groupMemberMapper)
  //      //      groupMemberMapper.save(member)
  //    }
  //  }

  //FIXME it's not working for groups
  //  def updateDependentGroupsForGroups(group: Group, gm: GroupMember)(implicit user: User): Unit = {
  //    val gmfa = userSession.accessor(classOf[GroupMembersFlatAccessor])
  //    val gmfm = userSession.mapper(classOf[GroupMemberFlat])
  //
  //    val all = gmfa.getGroupMembers(gm.getMemberPrincipalId).all.asScala
  //    all.foreach { x =>
  //      x.setGroupId(group.getGroupId)
  //      gmfm
  //    }
  //  }

  override def removeMember(groupId: UUID, memberId: UUID)(implicit u: User): Either[ApiErrors, Unit] = {

    val group = groupAccessor.getGroupById(groupId)
    val gma = groupMemberAccessor
    val groupMember = gma.getGroupMember(groupId, memberId)

    if (null == groupMember)
      Left(ApiErrors.single("Invalid group member", Messages("invalid.groupmember"), BAD_REQUEST))
    //    else if (user.getUserId.equals(group.getOwnerPrincipalId)) Right {
    else Right {
      //      groupMember.setZus(userSession)
      //      groupMember.deleteAndUpdateFlatMembership()
      groupMemberMapper.delete(groupMember)
      val count = gma.countMembers(groupId).one().get("members_count", classOf[Long])
      group.setMembers(count.toInt)
      groupMapper.save(group)
    }
  }

  def checkEmail(user: User, email: String, tenantId: UUID): Either[ApiErrors, Unit] = {
    if (user.getEmail != email && getUser(email)(tenantId).isDefined) {
      Left(ApiErrors.single("User already exists: " + email, Messages("account.create.user.exists"), BAD_REQUEST))
    } else {
      Right(())
    }
  }

  override def deleteGroup(groupId: UUID)(implicit u: User): Either[ApiErrors, Unit] = {
    val group = groupAccessor.getGroupById(groupId)

    if (null == group)
      Left(ApiErrors.single("Invalid group", Messages("invalid.group"), BAD_REQUEST))
    //    else if (user.getUserId.equals(group.getOwnerPrincipalId))
    else Right {
      //Right(("YONAS TOASTED THIS UNTIL HE CAN DELETE FLATTENED GROUP MEMBERS.") /*groupAccessor.deleteGroup(groupId)*/ ) // First delete all the flattened stuff....
      groupAccessor.deleteGroup(groupId)
      userSession.accessor(classOf[GroupMembersAccessor]).deleteGroupMembers(groupId)
      //      userSession.accessor(classOf[GroupMembersFlatAccessor]).deleteGroupMembers(groupId)
      groupAccessor.deleteGroup(groupId)
      val principal = principalAccessor.getPrincipaById(groupId)
      if (null != principal)
        principalMapper.delete(principal)
    }
    //    else
    //      Left(Messages("invalid.no_group_owner"))
  }

  def canAccessFilesFolders(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot view files and folders", Messages("cannot_view_files"))(_.canAccessFilesFolders)
  }

  def canCreateUsers(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canCreateAdmin(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateAdmin)
  }

  def canCreateGroups(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canResetPasswords(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canViewActivy(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canManageTermstore(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canManageAbilities(implicit u: User): Either[ApiErrors, Unit] = {
    canDo("user cannot create users", Messages("cannot_create_users"))(_.canCreateUsers)
  }

  def canDo(defMsg: String = "user cannot access functionallity",
            userMsg: String = Messages("permissions_default_error_msg"))(
              f: Abilities => Boolean)(implicit u: User): Either[ApiErrors, Unit] = {
    userRolesCache(u)
      .get(u.getUserRole)
      .map(ur => new Abilities(ur.getAbilities)) match {
        case Some(ur) if f(ur) => Right { () }
        case _                 => Left(ApiErrors.single(defMsg, userMsg, FORBIDDEN))
      }
  }

  def getUserRole(user: User): Option[UserRole] =
    userRolesCache(user).get(user.getUserRole)

  def userRolesCache(implicit user: User): Map[UUID, UserRole] = {
    userRolesCache(user.getTenantId)
  }

  def userRolesCache(implicit tenantId: UUID): Map[UUID, UserRole] = {
    cache.getOrElse[Map[UUID, UserRole]]("user_roles_" + tenantId.toString()) {
      getOrCreateDefaultUserRoles
    }
  }

  protected def getOrCreateDefaultUserRoles(implicit tenantId: UUID): Map[UUID, UserRole] = {
    tenantSession
    val ura = tenantSession.accessor(classOf[UserRoleAccessor])

    val allRoles = ura.getUserRoles.all.asScala
    val tenantRoles = allRoles.filter { ur => ur.getTenantId.equals(tenantId) }

    val resRoles = if (tenantRoles.isEmpty) {
      createDefaultRoles
    } else {
      tenantRoles
    }
    resRoles.map { ur => (ur.getRoleId, ur) } toMap
  }

  protected def createDefaultRoles(implicit tenantId: UUID): Seq[UserRole] = {
    var res = Array[UserRole]()
    val urm = tenantSession.mapper(classOf[UserRole])
    urm.save(Abilities.DefaultUserRoles.user);
    res = res :+ Abilities.DefaultUserRoles.user

    urm.save(Abilities.DefaultUserRoles.powerUser);
    res = res :+ Abilities.DefaultUserRoles.powerUser

    //    urm.save(Abilities.DefaultUserRoles.administrator);
    //    res = res :+ Abilities.DefaultUserRoles.administrator

    res
  }

  def getUserRoles(implicit user: User): List[UserRole] = {
    userRolesCache(user.getTenantId)
      .values
      .toList
      .sortBy(_.getName)
  }

  override def sendSMS(implicit user: User): Try[Unit] = Try {
    makeAuthyClient.getPhoneVerification.start(user.getPhoneNumber, user.getCountryCode, "sms", new Params)
  }

  override def checkSMS(implicit user: User, token: String): Try[Boolean] = Try {
    makeAuthyClient.getPhoneVerification.check(user.getPhoneNumber, user.getCountryCode, token).isOk
  }

  //TODO pretty inefficient 
  def getPrincipalByNameLike(name: String)(implicit user: User): Seq[Principal] = {
    val ids = getActiveUsers.filter { u =>
      u.getEmail.contains(name)
    }.map { u =>
      u.getUserId
    } ++
      groupAccessor.getGroups.all().asScala.filter { g =>
        g.getName.contains(name)
      }.map { g =>
        g.getGroupId
      }
    principalAccessor.getPrincipaById(ids.asJava).all().asScala
  }
  
  //TODO pretty inefficient 
  def getUserByNameLike(name: String)(implicit user: User): Seq[User] = {
    getActiveUsers.filter { u =>
      u.getEmail.contains(name)
    }
  }
}