package zyber.server.dao;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(keyspace = PasswordHistory.KEYSPACE, name = PasswordHistory.NAME)
public class PasswordHistory extends InTenant {
	public static final String KEYSPACE = "zyber_secure";
	public static final String NAME = "password_history";
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "user_id")
	UUID userId;
	
	@Column(name = "password_hash")
	@ClusteringColumn
	String passwordHash;
	
	@Column(name = "password_hash_type")
	String passwordHashType;
	
	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID user_id) {
		this.userId = user_id;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPasswordHashType() {
		return passwordHashType;
	}

	public void setPasswordHashType(String passwordHashType) {
		this.passwordHashType = passwordHashType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PasswordHistory that = (PasswordHistory) o;

		if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;
		if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
		if (passwordHash != null ? !passwordHash.equals(that.passwordHash) : that.passwordHash != null) return false;
		return passwordHashType != null ? passwordHashType.equals(that.passwordHashType) : that.passwordHashType == null;

	}

	@Override
	public int hashCode() {
		int result = tenantId != null ? tenantId.hashCode() : 0;
		result = 31 * result + (userId != null ? userId.hashCode() : 0);
		result = 31 * result + (passwordHash != null ? passwordHash.hashCode() : 0);
		result = 31 * result + (passwordHashType != null ? passwordHashType.hashCode() : 0);
		return result;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}
}
