package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = SecurityType.NAME)
public class SecurityType extends InTenant {
	public static final String NAME = "security_type";

	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@Column(name ="name")
	private String name;
	
//	@ClusteringColumn
	@PartitionKey(1)
	@Column(name = "security_id")
	private UUID securityTypeId;
	
	@Column(name ="permission")
	private int permission;
	
	public SecurityType() {
	}

	public SecurityType(String name, UUID securityTypeId, int permission) {
		this.name = name;
		this.securityTypeId = securityTypeId;
		this.permission = permission;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getSecurityTypeId() {
		return securityTypeId;
	}

	public void setSecurityTypeId(UUID securityTypeId) {
		this.securityTypeId = securityTypeId;
	}

	public int getPermission() {
		return permission;
	}

	public void setPermission(int permission) {
		this.permission = permission;
	}

	
}
