package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.Group;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface GroupAccessor {

	@Query("SELECT * from zyber.group WHERE tenant_id = :tenantId ")
	Result<Group> getGroups(@Param("tenantId") UUID tenantId);

//	@Query("SELECT * from zyber.group WHERE tenant_id = :tenantId AND owner_principal_id = :owner_principal_id")
//	Result<Group> getUserGroups(@Param("tenantId") UUID tenantId, @Param("owner_principal_id") UUID owner_id);

	@Query("SELECT * from zyber.group WHERE tenant_id = :tenantId AND name = :name")
	Group getGroupByName(@Param("tenantId") UUID tenantId, @Param("name") String name);

	@Query("SELECT * from zyber.group WHERE tenant_id = :tenantId AND group_id = :group_id")
	Group getGroupById(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id);

	@Query("DELETE from zyber.group WHERE tenant_id = :tenantId AND group_id = :group_id")
	void deleteGroup(@Param("tenantId") UUID tenantId, @Param("group_id") UUID group_id);
	
//	@Query("UPDATE group SET name = :name WHERE tenant_id = :tenantId AND group_id = :group_id")
//	ResultSet updateName(@Param("name") String name, @Param("group_id") UUID group_id);
}
