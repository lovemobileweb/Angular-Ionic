package zyber.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zyber.server.dao.GroupMember;
import zyber.server.dao.GroupMembersAccessor;
import zyber.server.dao.InTenant;
import zyber.server.dao.Path;
import zyber.server.dao.PathSecurity;
import zyber.server.dao.PathSecurityAccessor;
import zyber.server.dao.Principal;
import zyber.server.dao.Principal.PrincipalType;
import zyber.server.dao.SecurityType;
import zyber.server.dao.User;
import zyber.server.dao.UserAccessor;
import zyber.server.dao.UserKeys;
import zyber.server.dao.admin.Tenant;
import akka.routing.Group;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

public class ZyberUserSession {
	public final Logger log;

	public final ZyberSession session;
	public final User user;
	public final UUID tenantId;

	public static final Tenant DEFAULT_TENANT = new Tenant(
			UUID.fromString("11111111-1111-1111-1111-111111111111"),
			"localhost");

	/**
	 * Constructor for getting user from db
	 * 
	 * @param session
	 * @param username
	 */
	public ZyberUserSession(ZyberSession session, String username, UUID tenantId) {
		this.session = session;
		this.tenantId = tenantId;

		if (username != null) {
			this.user = session.findUser(tenantId, username);
		} else {
			throw new IllegalStateException("Username not valid.");
		}

		this.log = LoggerFactory.getLogger("ZyberUserSession-" + username);
	}

	/**
	 * Constructor for using authenticated user
	 * 
	 * @param session
	 * @param user
	 *            Authenticated user
	 */
	public ZyberUserSession(ZyberSession session, User user) {
		this.session = session;
		if (user != null) {
			this.user = user;
		} else {
			throw new IllegalStateException("Username not valid.");
		}
		this.tenantId = this.user.getTenantId();
		this.log = LoggerFactory.getLogger("ZyberUserSession-"
				+ user.getEmail());
	}

	public ZyberUserSession(ZyberSession session, User user, UUID tenantId) {
		this.session = session;
		this.tenantId = tenantId;
		this.user = user;

		this.log = LoggerFactory.getLogger("ZyberUserSession-"
				+ user.getEmail());
	}

	public ZyberUserSession(ZyberSession session, UUID tenantId) {
		this.session = session;
		this.tenantId = tenantId;
		this.user = null;
		this.log = LoggerFactory.getLogger("ZyberUserSession-" + tenantId);
	}

	public ZyberUserSession(ZyberSession session, UUID tenantId, String prefix) {
		this.session = session;
		this.tenantId = tenantId;
		this.user = getOrCreateUser(prefix,
				Long.toHexString(new Date().getTime()));
		this.log = LoggerFactory.getLogger("ZyberUserSession-" + tenantId);
	}

	public ZyberUserSession(ZyberSession session, UUID tenantId, String prefix,
			String password) {
		this.session = session;
		this.tenantId = tenantId;
		this.user = getOrCreateUser(prefix, password);
		this.log = LoggerFactory.getLogger("ZyberUserSession-" + tenantId);
	}

	public Path getRootPath() {
		return user.getRootPath(this);
	}

	public User getUser() {
		return user;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends InTenant> CassandraMapperDelegate<T> mapper(Class<T> klass) {
		if (klass.equals(GroupMapper.class))
			return new GroupMapper(session.getMappingManager().mapper(
					Group.class), tenantId);
		if (klass.equals(GroupMemberMapper.class))
			return new GroupMemberMapper(session.getMappingManager().mapper(
					GroupMember.class), tenantId);
		Mapper<T> map = session.getMappingManager().mapper(klass);
		return new CassandraMapperDelegate<T>(map, tenantId);
	}

	/**
	 * Get an accessor that automatically filters results by the current tenant.
	 */
	public <T> T accessor(Class<T> klass) {
		try {
			Constructor<T> c = klass.getConstructor(MappingManager.class,
					UUID.class);
			return c.newInstance(session.getMappingManager(), tenantId);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public User findUser(String email) {
		UserAccessor userAccessor = accessor(UserAccessor.class);
		User user = userAccessor.getUserByEmail(email);
		return user;
	}

	public Permission calculatePermission(Path onPath,
			Map<UUID, SecurityType> cache) {
		return doCalculatePermission(onPath, cache);
	}

	protected Permission doCalculatePermission(Path onPath,
			Map<UUID, SecurityType> cache) {
		int ret = 0;
		// list permissions on folder.
		// ... for each ... list the group permissions.
		PathSecurityAccessor psa = accessor(PathSecurityAccessor.class);
		GroupMembersAccessor gma = accessor(GroupMembersAccessor.class);
//		GroupMembersFlatAccessor gma = accessor(GroupMembersFlatAccessor.class);


		for (PathSecurity ps : psa.getSecurityForPath(onPath.getParentPathId(), onPath.getPathId())) {
			if (ps.getPrincipalType().equals(PrincipalType.User)) {
				if (ps.getPrincipalId().equals(user.getUserId())) {
					// ret |= ps.getPermission(this);
					ret |= getPermission(ps, cache);
				}
			} else {
//				for (GroupMemberFlat gm : gma.getGroupMembers(ps.getPrincipalId())) {
				for (GroupMember gm : gma.getGroupMembers(ps.getPrincipalId())) {
					if (gm.getMemberPrincipalId().equals(user.getUserId())) {
						ret |= getPermission(ps, cache);
					}
				}
			}
		}
		return new Permission(ret);

	}

	public User getOrCreateUser(String email, String password) {
		User u = findUser(email);
		if (u == null) {
			CassandraMapperDelegate<User> mapper = mapper(User.class);
			CassandraMapperDelegate<UserKeys> ukMapper = mapper(UserKeys.class);
			UUID userId = UUID.randomUUID();
			Date createdDate = new Date();
			User user = new User(userId, email, "12", null, createdDate, email);
			user.setActive(true);
			UserKeys userKey = new UserKeys();
			userKey.setUserId(user.getUserId());
			userKey.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
			userKey.setPasswordHashType("BCrypt");

			// User user = new
			// User(userId,email.substring(email.lastIndexOf("@")+1), email);
			mapper.save(user);
			ukMapper.save(userKey);

			Principal principal = new Principal(userId,
					Principal.PrincipalType.User, null, createdDate);

			principal.setDisplayName(email);
			mapper(Principal.class).save(principal);

			// TODO Question also create path here. ??

			return user;
		} else
			return u;
		// Select if the user exists with that email. If not create one and set
		// the password.
		// Otherwise, just return the one that is there.
	}

	public int getPermission(PathSecurity ps, Map<UUID, SecurityType> cache) {
		SecurityType securityType = cache.get(ps.getSecurityType());
		if (null != securityType)
			return securityType.getPermission();
		return 0;
	}
}
