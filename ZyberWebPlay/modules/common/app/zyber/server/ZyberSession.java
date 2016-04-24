package zyber.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.SessionHolder;
import com.SessionHolder2;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.IOUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zyber.server.dao.ApplicationSetting;
import zyber.server.dao.ApplicationSettingsAccessor;
import zyber.server.dao.PathType;
import zyber.server.dao.Principal.PrincipalType;
import zyber.server.dao.admin.Tenant;
import zyber.server.dao.admin.TenantAdmin;
import zyber.server.dao.admin.TenantsAccessor;
import zyber.server.dao.admin.TenantsSettingsAccessor;
import zyber.server.dao.admin.TennantsSetting;
import zyber.server.dao.User;
import zyber.server.dao.UserKeys;
import zyber.server.dao.rawaccessors.UserAccessor;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

public class ZyberSession implements Closeable {
	private final Cluster cluster;
	private final Session csession;
	private final MappingManager manager;
	public static final Logger log = LoggerFactory
			.getLogger(ZyberSession.class);
	public static final String KEYSPACE_NAME = "zyber";
	public static final String KEYSPACE_SECURE_NAME = "zyber_secure";
	public static final String ZYBER_CQL = "/zyber/ZyberGen.cql";

	public static final String TENANTS_KEYSPACE = "zyber_tenants";
	public static final String TENANTS_CQL = "/zyber/db_zyber_tenants.cql";


	private Map<String, String> ConfigTable = new HashMap<String, String>();

	protected static ZyberSession global_session;
	
	public static ZyberSession getZyberSession(String adminUser, String adminPass) {
		if (global_session == null) {
			global_session = new ZyberSession(adminUser, adminPass);
			checkTenantsSchema(adminUser, adminPass);
			checkZyberSchema();
		}
		return global_session;
	}

	public static ZyberSession getZyberSession() {
		return getZyberSession(null, null);
	}

	private static void checkZyberSchema() {
		String runningHash = null;
		try {
			runningHash = global_session
					.getConfigValue(CONF_KEY__CQL_SCHEMA_HASH);
		} catch (Exception e) {
		}
		String fileHash = getSchemaFileHash(getZyberCQL());

		if (runningHash == null || !runningHash.equals(fileHash)) {
			ZyberSession.log
					.info("Schema file on disk different than running schema. Dropping and re-creating.");
			global_session.zyberSchemaUpdate(true);
		}
	}

	/**
	 * Default constructor for development. Connects to a cluster at localhost
	 * and creates the cluster and keyspaces if they are missing.
	 */
	public ZyberSession() {
		this(null, null);
	}
	private ZyberSession(String tenantsAdminUser, String tenantsAdminPass) {
		if (global_session == null)
			global_session = this;
		else
			throw new IllegalStateException("Only create one zyber session.");

		Config load = ConfigFactory.load("local.conf");
		if(load.hasPath("reuseZyber") && load.getBoolean("reuseZyber")) {
			cluster = SessionHolder.getCluster();
			manager = SessionHolder.getManager();
			csession = SessionHolder.getCsession();
		} else {
			cluster = SessionHolder2.getCluster();
			manager = SessionHolder2.getManager();
			csession = SessionHolder2.getCsession();
		}

		CodecRegistry.DEFAULT_INSTANCE.register(new TypeCodec<PathType>(
				DataType.varchar(), PathType.class) {

			@Override
			public ByteBuffer serialize(PathType value,
					ProtocolVersion protocolVersion)
					throws InvalidTypeException {
				return ByteBuffer.wrap(value.name().getBytes());
			}

			@Override
			public PathType deserialize(ByteBuffer bytes,
					ProtocolVersion protocolVersion)
					throws InvalidTypeException {
				return PathType.valueOf(new String(bytes.array()));
			}

			@Override
			public PathType parse(String value) throws InvalidTypeException {
				return PathType.valueOf(value);
			}

			@Override
			public String format(PathType value) throws InvalidTypeException {
				return value.name();
			}
		});

		CodecRegistry.DEFAULT_INSTANCE.register(new TypeCodec<PrincipalType>(
				DataType.varchar(), PrincipalType.class) {

			@Override
			public ByteBuffer serialize(PrincipalType value,
					ProtocolVersion protocolVersion)
					throws InvalidTypeException {
				return ByteBuffer.wrap(value.name().getBytes());
			}

			@Override
			public PrincipalType deserialize(ByteBuffer bytes,
					ProtocolVersion protocolVersion)
					throws InvalidTypeException {
				return PrincipalType.valueOf(new String(bytes.array()));
			}

			@Override
			public PrincipalType parse(String value)
					throws InvalidTypeException {
				return PrincipalType.valueOf(value);
			}

			@Override
			public String format(PrincipalType value)
					throws InvalidTypeException {
				return value.name();
			}
		});

		tenantsSchemaUpdate(false, tenantsAdminUser, tenantsAdminPass);
		zyberSchemaUpdate(false);
		csession.execute("use " + KEYSPACE_NAME + ";");
	}

	public void zyberSchemaUpdate(boolean dropAndCreateUsuallyForTesting) {
		if (dropAndCreateUsuallyForTesting) {
			csession.execute("DROP KEYSPACE IF EXISTS " + KEYSPACE_NAME + ";");
			csession.execute("DROP KEYSPACE IF EXISTS " + KEYSPACE_SECURE_NAME
					+ ";");
		}

		boolean createColFams = schemaUpdateCreateKeySpaces();

		csession.execute("use " + KEYSPACE_NAME + ";");

		if (createColFams) {
			log.info("Creating column families");

			String cqlCreateTables = getZyberCQL();
			for (String x : cqlCreateTables.split(";")) {
				String csql = (x.replaceAll("\r", "").replaceAll("\t", ""))
						.trim();
				while (csql.startsWith("\n"))
					csql = csql.substring(1).trim();

				boolean allComments = true;
				for (String bit : csql.split("\n"))
					if (!bit.trim().startsWith("--"))
						allComments = false;

				if (csql.length() == 0)
					continue; // Skip empty lines (EOF in most cases).
				if (csql.startsWith("--") && csql.indexOf("\n") == -1)
					continue; // Skip single comment lines.
				csql += ";";
				try {
					if (!allComments)
						csession.execute(csql);

					if (allComments)
						log.info("Skipped Comments: " + x);
					else if (x.indexOf("(") > 0)
						log.info(csql.substring(0, csql.indexOf("(") - 1));
					else
						log.info("Executed CSQL: " + x);
				} catch (Exception e) {
					throw new IllegalStateException("While executing: [["
							+ csql + "]]", e);
				}
			}

			setConfigValue(CONF_KEY__CQL_SCHEMA_HASH,
					ZyberTestSession.getSchemaFileHash());
		}
	}

	/** Return TRUE if it created the keyspace. */
	private boolean schemaUpdateCreateKeySpaces(String keyspaceName) {
		try {
			csession.execute("USE " + keyspaceName + "");
			return false;
		} catch (InvalidQueryException e) {
			log.info("Keyspace '" + keyspaceName
					+ "' does not exist, creating.");
			String createKeyspaceKeysCSQL = "CREATE KEYSPACE "
					+ keyspaceName
					+ " WITH replication = {'class':'SimpleStrategy', 'replication_factor':1};";
			csession.execute(createKeyspaceKeysCSQL);
			return true;
		}
	}

	private boolean schemaUpdateCreateKeySpaces() {
		boolean createdZyber = schemaUpdateCreateKeySpaces(KEYSPACE_NAME);
		boolean createdSecure = schemaUpdateCreateKeySpaces(KEYSPACE_SECURE_NAME);
		return createdZyber || createdSecure;
	}

	public MappingManager getMappingManager() {
		return manager;
	}

	public void close() {
		cluster.close();
	}

	public Session getSession() {
		// return cluster.connect(KEYSPACE_NAME);
		return csession;
	}

	public User getOrCreateUser(UUID tenantId, String email, String password) {
		User u = findUser(tenantId, email);
		if (u == null) {
			Mapper<User> mapper = manager.mapper(User.class);
			Mapper<UserKeys> ukMapper = manager.mapper(UserKeys.class);
			UUID userId = UUID.randomUUID();
			User user = new User(userId, email, "12", null, new Date(), email);
			UserKeys userKey = new UserKeys();
			userKey.setUserId(user.getUserId());
			userKey.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
			userKey.setPasswordHashType("BCrypt");

			// User user = new
			// User(userId,email.substring(email.lastIndexOf("@")+1), email);
			mapper.save(user);
			ukMapper.save(userKey);
			// TODO Question also create path here. ??

			return user;
		} else
			return u;
		// Select if the user exists with that email. If not create one and set
		// the password.
		// Otherwise, just return the one that is there.

	}

	public User findUser(UUID tenantId, String email) {
		MappingManager manager = getMappingManager();
		UserAccessor userAccessor = manager.createAccessor(UserAccessor.class);
		User user = userAccessor.getUserByEmail(tenantId, email);
		return user;
	}

	// For debugging only
	public List<User> findAllUsers(UUID tenantId) {
		MappingManager manager = getMappingManager();
		UserAccessor userAccessor = manager.createAccessor(UserAccessor.class);
		return userAccessor.getAll(tenantId).all();
	}

	public static final String CONF_KEY__DEFAULT_BLOCK_SIZE = "BLOCK_SIZE";
	public static final String CONF_KEY__DEFAULT_CIPHER = "DEFAULT_CIPHER";
	public static final String CONF_KEY__DEFAULT_KEY_TYPE = "DEFAULT_KEY_TYPE";
	public static final String CONF_KEY__DEFAULT_KEY_LENGTH = "DEFAULT_KEY_LENGTH";
	public static final String CONF_KEY__CQL_SCHEMA_HASH = "CQL_SCHEMA_HASH";

	private ApplicationSettingsAccessor asa;

	public String getConfigValue(String key) {
		String ret = ConfigTable.get(key);
		if (ret != null)
			return ret;
		ZyberUserSession zus = new ZyberUserSession(this, new UUID(0L, 0L));
		if (asa == null) {
			asa = zus.accessor(ApplicationSettingsAccessor.class);
		}
		// ApplicationSetting setting = asa.getSetting(key);
		ApplicationSetting setting = asa.getGlobalSetting(key);

		if (setting != null) {
			ConfigTable.put(key, setting.getValue());
			return setting.getValue();
		}

		if (ret == null && key.equals(CONF_KEY__DEFAULT_BLOCK_SIZE))
			ret = "8000";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_CIPHER))
			ret = "AES/CBC/PKCS5Padding";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_KEY_TYPE))
			ret = "AES";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_KEY_LENGTH))
			ret = "128";
		setConfigValue(key, ret);
		return ret;
	}

	public void setConfigValue(String key, String value) {
		UUID tenantId = new UUID(0L, 0L);
		ZyberUserSession zus = new ZyberUserSession(this, tenantId);
		ApplicationSetting appSettings = new ApplicationSetting(key, value);
		zus.mapper(ApplicationSetting.class).save(appSettings);
	}

	public Tenant getTenantBySubdomain(String subdomain) {
		TenantsAccessor tenantsAccessor = manager
				.createAccessor(TenantsAccessor.class);

		return tenantsAccessor.getTenantBySubdomain(subdomain);
	}
	
	public static String getSchemaFileHash(String cqlString) {

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(cqlString.getBytes("UTF-8"));
			byte[] digest = md.digest();
			return String.format("%064x", new java.math.BigInteger(1, digest));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
	}
	
	static String getZyberCQL() {
		String cqlSourceName = ZYBER_CQL;
		return getCQL(cqlSourceName);
	}
	
	static String getCQL(String cqlSourceName) {
		try (InputStream cql = ZyberSession.class
				.getResourceAsStream(cqlSourceName)) {
			if (cql == null)
				throw new IllegalStateException("Unable to find "
						+ cqlSourceName + " on the classpath.");
			return IOUtils.toString(cql);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	/*##############################################################*/
	//Tenants schema handling
	private static void checkTenantsSchema(String adminUser, String adminPass) {
		String runningHash = null;
		try {
			runningHash = global_session.getTenantsConfigValue(CONF_KEY__CQL_SCHEMA_HASH);
		} catch (Exception e) {
		}
		
		String fileHash = getSchemaFileHash(getTenantsCQL());

		if (runningHash == null || !runningHash.equals(fileHash)) {
			log
					.info("Tenants Schema file on disk different than running schema. Dropping and re-creating.");
			global_session.tenantsSchemaUpdate(true, adminUser, adminPass);
		}
	}
	
	static String getTenantsCQL() {
		String cqlSourceName = TENANTS_CQL;
		return getCQL(cqlSourceName);
	}
	
	
	public void tenantsSchemaUpdate(boolean dropAndCreateUsuallyForTesting, String adminUser, String adminPass) {
		if (dropAndCreateUsuallyForTesting) {
			csession.execute("DROP KEYSPACE IF EXISTS " + TENANTS_KEYSPACE + ";");
		}

		boolean createColFams = tenantsSchemaUpdateCreateKeySpaces();

		csession.execute("use " + TENANTS_KEYSPACE + ";");

		if (createColFams) {
			log.info("Creating column families");

			String cqlCreateTables = getTenantsCQL();
			for (String x : cqlCreateTables.split(";")) {
				String csql = (x.replaceAll("\r", "").replaceAll("\t", "")).trim();
				while (csql.startsWith("\n"))
					csql = csql.substring(1).trim();

				boolean allComments = true;
				for (String bit : csql.split("\n"))
					if (!bit.trim().startsWith("--"))
						allComments = false;

				if (csql.length() == 0)
					continue; // Skip empty lines (EOF in most cases).
				if (csql.startsWith("--") && csql.indexOf("\n") == -1)
					continue; // Skip single comment lines.
				csql += ";";
				try {
					if (!allComments)
						csession.execute(csql);

					if (allComments)
						log.info("Skipped Comments: " + x);
					else if (x.indexOf("(") > 0)
						log.info(csql.substring(0, csql.indexOf("(") - 1));
					else
						log.info("Executed CSQL: " + x);
				} catch (Exception e) {
					throw new IllegalStateException("While executing: [[" + csql + "]]", e);
				}
			}

			setTenantsConfigValue(CONF_KEY__CQL_SCHEMA_HASH, getSchemaFileHash(cqlCreateTables));

			if(null != adminUser && !adminUser.isEmpty() 
					&& null != adminPass && !adminPass.isEmpty())
				createAdminUser(adminUser, adminPass);
		}
	}

	private void createAdminUser(String adminUser, String adminPass) {		
		TenantAdmin user = new TenantAdmin(UUID.randomUUID(),adminUser, 
				BCrypt.hashpw(adminPass, BCrypt.gensalt()), 
				new Date(), adminUser, "en");
		
		Mapper<TenantAdmin> mapper = manager.mapper(TenantAdmin.class);
		
		mapper.save(user);
	}
	
	public void setTenantsConfigValue(String key, String value) {
		csession.execute("USE " + TENANTS_KEYSPACE + "");

		manager.mapper(TennantsSetting.class).save(new TennantsSetting(key, value));
	}
	
	
	private TenantsSettingsAccessor tsa;
	private Map<String, String> tenantsConfigTable = new HashMap<String, String>();

	public String getTenantsConfigValue(String key) {
		String ret = tenantsConfigTable.get(key);
		if (ret != null)
			return ret;

		if (tsa == null) {
			tsa = manager.createAccessor(TenantsSettingsAccessor.class);
		}
		TennantsSetting setting = tsa.getSetting(key);
		if (setting != null) {
			tenantsConfigTable.put(key, setting.getValue());
			return setting.getValue();
		}

		if (ret == null && key.equals(CONF_KEY__DEFAULT_BLOCK_SIZE))
			ret = "8000";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_CIPHER))
			ret = "AES/CBC/PKCS5Padding";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_KEY_TYPE))
			ret = "AES";
		if (ret == null && key.equals(CONF_KEY__DEFAULT_KEY_LENGTH))
			ret = "128";
		setTenantsConfigValue(key, ret);
		return ret;
	}
	
	private boolean tenantsSchemaUpdateCreateKeySpaces() {
		boolean createdTenants = schemaUpdateCreateKeySpaces(TENANTS_KEYSPACE);
		return createdTenants;
	}
	
	public MappingManager getMappingManagerForTenants() {
		return manager;
	}
}
