package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = UserKeys.KEYSPACE, name = UserKeys.NAME)
public class UserKeys extends InTenant {
	public static final String KEYSPACE = "zyber_secure";
	public static final String NAME = "user_keys";
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "user_id")
	UUID userId;
	
	@Column(name = "password_hash")
	String passwordHash;
	
	@Column(name = "password_hash_type")
	String passwordHashType;
	
	@Column(name = "public_key")
	String publicKey;
	
	@Column(name = "private_key")
	String privateKey;
	
	@Column(name = "key_type")
	String keyType;

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

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getKeyType() {
		return keyType;
	}

	public void setKeyType(String keyType) {
		this.keyType = keyType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserKeys other = (UserKeys) obj;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}
}
