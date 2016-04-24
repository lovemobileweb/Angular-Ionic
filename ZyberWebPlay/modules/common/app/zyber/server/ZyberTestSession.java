package zyber.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import zyber.server.dao.Path;
import zyber.server.dao.User;
import zyber.server.dao.UserKeys;

import com.datastax.driver.core.Session;

public class ZyberTestSession {
	static ZyberSession zyberTestingSession;

	/** Ensures that the running scheme is the same as the file schema. */
	public synchronized static ZyberSession getSessionForTesting() {
		return getSessionForTesting(null, null);
	}
	public synchronized static ZyberSession getSessionForTesting(String adminTenantsUser, String adminTenantsPass) {
		if (zyberTestingSession == null) {
			zyberTestingSession = ZyberSession.getZyberSession(adminTenantsUser, adminTenantsPass);

			String runningHash = zyberTestingSession.getConfigValue(ZyberSession.CONF_KEY__CQL_SCHEMA_HASH);
			String fileHash = getSchemaFileHash();

			if (runningHash == null || !runningHash.equals(fileHash)) {
				ZyberSession.log.info("Schema file on disk different than running schema. Dropping and re-creating.");
				zyberTestingSession.zyberSchemaUpdate(true);
			}
		}
		return zyberTestingSession;
	}

	public static ZyberUserSession getTestUserSession(
			UUID tenantId, String prefix) {
		return getTestUserSession(tenantId, prefix,true);
	}
	
	public static ZyberUserSession getTestUserSession(
			UUID tenantId, String prefix, boolean drop) {
		return getTestUserSession(tenantId, prefix, drop, null, null);
	}

	public static ZyberUserSession getTestUserSession(
			UUID tenantId, String prefix, boolean drop,
			String adminTenantsUser, String adminTenantsPass) {

		//We cannot drop entire table because multitenancy. Instead use DBTest.deleteForTestingTenant meth
//		ZyberSession sessionForTesting = 
				getSessionForTesting();
//		if(drop) {
//			cleanDB(sessionForTesting.getSession());
//		}
		ZyberUserSession zyberUserSession = new ZyberUserSession(zyberTestingSession, tenantId, prefix);
		
		return zyberUserSession;
	}

	public static ZyberUserSession getTestUserSession(UUID tenantId) {
		return getTestUserSession(tenantId, null, null);
	}
	
	public static ZyberUserSession getTestUserSession(
			UUID tenantId, String adminTenantsUser, String adminTenantsPass) {
		return getTestUserSession(tenantId,"Test",true, adminTenantsUser, adminTenantsPass);
	}

	public static ZyberUserSession getTestUserSession(UUID tenantId, boolean drop) {
		return getTestUserSession(tenantId,"Test",drop);
	}

	public static void cleanDB(Session session)  {
		session.execute(truncate(Path.NAME));
		session.execute(truncate(UserKeys.KEYSPACE, UserKeys.NAME));
		session.execute(truncate(User.NAME));
	}

	public static String truncate(String s ){
		return "TRUNCATE "+s;
	}

	public static String truncate(String space, String name ){
		return "TRUNCATE "+space+"."+name;
	}
	
	public static String getSchemaFileHash() {

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(ZyberSession.getZyberCQL().getBytes("UTF-8"));
			byte[] digest = md.digest();
			return String.format("%064x", new java.math.BigInteger(1, digest));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException();
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
	}
}
