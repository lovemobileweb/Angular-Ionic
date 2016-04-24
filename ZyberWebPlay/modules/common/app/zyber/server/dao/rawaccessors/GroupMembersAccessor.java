package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.GroupMember;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface GroupMembersAccessor {

	@Query("SELECT * from zyber.group_member WHERE tenant_id = :tenantId AND group_id = :group_id")
	Result<GroupMember> getGroupMembers(@Param("tenantId") UUID tenantId, @Param("group_id") UUID groupId);
	
	@Query("SELECT * from zyber.group_member WHERE tenant_id = :tenantId AND group_id = :group_id and member_principal_id = :member_principal_id")
	GroupMember getGroupMember(@Param("tenantId") UUID tenantId, @Param("group_id") UUID groupId,
			@Param("member_principal_id") UUID member_principal_id);

// DON'T DO THIS. See code in GroupMember. We also need to delete the flattened items.
//	@Query("DELETE from group_member WHERE tenant_id = :tenantId AND group_id = :group_id and member_principal_id = :member_principal_id")
//	void deleteGroupMember(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id,
//			@Param("member_principal_id") UUID member_principal_id);
	
	@Query("DELETE from group_member WHERE tenant_id = :tenantId AND group_id = :group_id")
	void deleteGroupMembers(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id);
	
	@Query("SELECT * from zyber.group_member_by_principal WHERE tenant_id = :tenantId AND member_principal_id = :member_principal_id")
	Result<GroupMember> getGroupMembersByPrincipal(@Param("tenantId") UUID tenantId, @Param("member_principal_id") UUID memberPrincipalId);
	
	@Query("SELECT count(*) as members_count FROM zyber.group_member WHERE tenant_id = :tenantId AND group_id = :group_id")
	ResultSet countMembers(@Param("tenantId") UUID tenantId, @Param("group_id") UUID groupId);
	
}
