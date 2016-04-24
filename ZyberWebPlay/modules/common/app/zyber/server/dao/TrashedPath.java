package zyber.server.dao;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;

@Table(keyspace = "zyber", name = TrashedPath.NAME)
public class TrashedPath extends InTenant {
	public static final String NAME = "trash";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	@PartitionKey(1)
	@Column(name = "user_id")
	private UUID userId;
//	@PartitionKey(2)
	@ClusteringColumn
	@Column(name = "path_id")
	private UUID pathId;

	//Below not really needed, but allows access to be faster. Can't store parent name as it is mutable even after delete, although we could update
	@Column(name = "parent_id")
	private UUID parentId;
	@Column(name = "name")
	private String name;
	@Column(name = "directory")
	private boolean directory;

	public TrashedPath() {
	}

	public TrashedPath(UUID pathId, UUID userId, UUID parentId, String name, boolean directory) {
		this.pathId = pathId;
		this.userId = userId;
		this.parentId = parentId;
		this.name = name;
		this.directory = directory;
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID pathId) {
		this.pathId = pathId;
	}

	public UUID getParentId() {
		return parentId;
	}

	public void setParentId(UUID parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}
}
