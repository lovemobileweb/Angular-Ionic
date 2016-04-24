package zyber.server.dao;

import java.util.UUID;

import zyber.server.ZyberUserSession;
import zyber.server.dao.Principal.PrincipalType;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = PathSecurity.NAME)
public class PathSecurity extends InTenant {
	public static final String NAME = "path_security";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "parent_path_id")
	private UUID parentPathId;

	@ClusteringColumn(0)
	@Column(name = "path_id")
	private UUID pathId;

	@ClusteringColumn(1)
	@Column(name = "principal_id")
	private UUID principalId;
	
	@ClusteringColumn(2)
	@Column(name = "principal_type")
	private Principal.PrincipalType principalType;

	@Column(name = "security_type")
	private UUID securityType;

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
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

	public Principal.PrincipalType getPrincipalType() {
		return principalType;
	}

	public void setPrincipalType(Principal.PrincipalType principalType) {
		this.principalType = principalType;
	}

	public int getPermission(ZyberUserSession zus) {
		SecurityType st = zus.accessor(SecurityTypeAccessor.class).getSecurityType(securityType);
		return st.getPermission();
	}

	public UUID getSecurityType() {
		return securityType;
	}

	public void setSecurityType(UUID securityType) {
		this.securityType = securityType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pathId == null) ? 0 : pathId.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((principalType == null) ? 0 : principalType.hashCode());
		result = prime * result
				+ ((tenantId == null) ? 0 : tenantId.hashCode());
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
		PathSecurity other = (PathSecurity) obj;
		if (pathId == null) {
			if (other.pathId != null)
				return false;
		} else if (!pathId.equals(other.pathId))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (principalType == null) {
			if (other.principalType != null)
				return false;
		} else if (!principalType.equals(other.principalType))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		return true;
	}

	public PathSecurity() {
	}

	public PathSecurity(UUID tenantId, UUID parentPathId, UUID pathId, UUID principalId,
			PrincipalType principalType) {
		this.parentPathId = parentPathId;
		this.tenantId = tenantId;
		this.pathId = pathId;
		this.principalId = principalId;
		this.principalType = principalType;
	}

	public PathSecurity(UUID pathId, UUID parentPathId, UUID principalId,
			PrincipalType principalType) {
		this.parentPathId = parentPathId;
		this.pathId = pathId;
		this.principalId = principalId;
		this.principalType = principalType;
	}

	public UUID getParentPathId() {
		return parentPathId;
	}

	public void setParentPathId(UUID parentPathId) {
		this.parentPathId = parentPathId;
	}

}
