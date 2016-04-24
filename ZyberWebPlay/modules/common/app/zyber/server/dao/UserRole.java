package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = UserRole.NAME)
public class UserRole extends InTenant {
	public static final String NAME = "user_role";

	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@Column(name ="name")
	private String name;
	
//	@ClusteringColumn
	@PartitionKey(1)
	@Column(name = "role_id")
	private UUID roleId;
	
	@Column(name ="abilities")
	private int abilities;
	
	public UserRole() {
	}

	public UserRole(String name, UUID roleId, int abilities) {
		this.name = name;
		this.roleId = roleId;
		this.abilities = abilities;
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

	public int getAbilities() {
		return abilities;
	}

	public void setAbilities(int abilities) {
		this.abilities = abilities;
	}

	public UUID getRoleId() {
		return roleId;
	}

	public void setRoleId(UUID roleId) {
		this.roleId = roleId;
	}
}