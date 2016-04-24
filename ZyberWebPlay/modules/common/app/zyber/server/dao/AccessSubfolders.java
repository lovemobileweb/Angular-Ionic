package zyber.server.dao;

import java.util.UUID;

import zyber.driver.mapping.annotations.Index;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = AccessSubfolders.NAME)
public class AccessSubfolders extends InTenant {
	public static final String NAME = "access_subfolders";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "parent_path_id")
	private UUID parentPathId;

	@Index
	@ClusteringColumn(0)
	@Column(name = "principal_id")
	private UUID principalId;
	
	@Index
	@ClusteringColumn(1)
	@Column(name = "path_id")
	private UUID pathId;
	
	public AccessSubfolders() {
	}
	
	public AccessSubfolders(UUID parentPathId, UUID pathId, UUID principalId) {
		this.parentPathId = parentPathId;
		this.pathId = pathId;
		this.principalId = principalId;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getParentPathId() {
		return parentPathId;
	}

	public void setParentPathId(UUID parentPathId) {
		this.parentPathId = parentPathId;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID pathId) {
		this.pathId = pathId;
	}

	public UUID getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(UUID principalId) {
		this.principalId = principalId;
	}
	
	
}
