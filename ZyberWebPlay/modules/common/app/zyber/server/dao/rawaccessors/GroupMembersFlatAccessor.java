package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.GroupMemberFlat;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface GroupMembersFlatAccessor {
	
	@Query("SELECT * from "+GroupMemberFlat.KEYSPACE+ "."+GroupMemberFlat.NAME+" WHERE tenant_id = :tenantId AND group_id = :group_id")
	Result<GroupMemberFlat> getGroupMembers(@Param("tenantId") UUID tenantId, @Param("group_id") UUID groupId);
	
	@Query("SELECT * from "+GroupMemberFlat.KEYSPACE+ "."+GroupMemberFlat.NAME+" WHERE tenant_id = :tenantId AND group_id = :group_id and member_principal_id = :member_principal_id")
	GroupMemberFlat getGroupMember(@Param("tenantId") UUID tenantId, @Param("group_id") UUID groupId,
			@Param("member_principal_id") UUID member_principal_id);
	
	@Query("DELETE from group_member_flat WHERE tenant_id = :tenantId AND group_id = :group_id and member_principal_id = :member_principal_id")
	void deleteGroupMember(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id,
			@Param("member_principal_id") UUID member_principal_id);
	
	@Query("DELETE from "+GroupMemberFlat.KEYSPACE+ "."+GroupMemberFlat.NAME+" WHERE tenant_id = :tenantId AND group_id = :group_id")
	void deleteGroupMembers(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id);
	
	@Query("SELECT * from group_member_flat_user_groups WHERE tenant_id = :tenantId AND member_principal_id = :member_principal_id")
	Result<GroupMemberFlat> getUserGroups(@Param("tenantId") UUID tenantId, @Param("member_principal_id") UUID userId);
	
	@Query("SELECT * FROM group_member_flat_by_source WHERE tenant_id = :tenantId AND source_group_id = :source_group_id AND member_principal_id = :member_principal_id")
	Result<GroupMemberFlat> getGroupsBySource(@Param("tenantId") UUID tenantId, @Param("source_group_id") UUID sourceGroupId, 
			@Param("member_principal_id") UUID memberPrincipalId);
}

