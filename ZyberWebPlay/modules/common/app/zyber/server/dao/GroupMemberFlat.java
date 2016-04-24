package zyber.server.dao;

import java.util.UUID;

import zyber.driver.mapping.annotations.View;
import zyber.server.dao.Principal.PrincipalType;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = GroupMemberFlat.KEYSPACE, name = GroupMemberFlat.NAME)
@View("CREATE MATERIALIZED VIEW group_member_flat_user_groups AS\n" + 
		"		  SELECT tenant_id, group_id, member_principal_id, member_principal_type, source_group_id \n" + 
		"		  FROM group_member_flat\n" + 
		"		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND \n" + 
		"		  member_principal_type IS NOT NULL AND source_group_id IS NOT NULL    \n" + 
		"		  PRIMARY KEY ((tenant_id,member_principal_id),source_group_id, group_id) \n" + 
		"         WITH CLUSTERING ORDER BY (group_id desc);\n;")
@View("CREATE MATERIALIZED VIEW group_member_flat_by_source AS\n" + 
		"		  SELECT tenant_id, group_id, member_principal_id, member_principal_type, source_group_id \n" + 
		"		  FROM group_member_flat\n" + 
		"		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND \n" + 
		"		  member_principal_type IS NOT NULL AND source_group_id IS NOT NULL    \n" + 
		"		  PRIMARY KEY ((tenant_id, source_group_id, member_principal_id), group_id) \n" + 
		"         WITH CLUSTERING ORDER BY (group_id desc);\n;")
public class GroupMemberFlat extends InTenant {

	public static final String NAME = "group_member_flat";
	
	public static final String KEYSPACE = "zyber";
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	protected UUID tenantId;
		
	@PartitionKey(1)
	@Column(name = "group_id")
	private UUID groupId;
	
	@ClusteringColumn(0)
	@Column(name = "member_principal_id")
	private UUID memberPrincipalId;
	
	@ClusteringColumn(1)
	@Column(name = "source_group_id")
	private UUID sourceGroupId;
	
	@Column(name = "member_principal_type")
	private Principal.PrincipalType memberPrincipalType;
	

	public GroupMemberFlat() {
	}
	
	public GroupMemberFlat(UUID groupId, UUID memberPrincipalId,
			PrincipalType memberPrincipalType, UUID sourceGroupId) {
		this.groupId = groupId;
		this.memberPrincipalId = memberPrincipalId;
		this.memberPrincipalType = memberPrincipalType;
		this.sourceGroupId = sourceGroupId;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public void setGroupId(UUID groupId) {
		this.groupId = groupId;
	}

	public UUID getMemberPrincipalId() {
		return memberPrincipalId;
	}

	public void setMemberPrincipalId(UUID memberPrincipalId) {
		this.memberPrincipalId = memberPrincipalId;
	}

	public Principal.PrincipalType getMemberPrincipalType() {
		return memberPrincipalType;
	}

	public void setMemberPrincipalType(Principal.PrincipalType memberPrincipalType) {
		this.memberPrincipalType = memberPrincipalType;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getSourceGroupId() {
		return sourceGroupId;
	}

	public void setSourceGroupId(UUID source_groupId) {
		this.sourceGroupId = source_groupId;
	}

}

