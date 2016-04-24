package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.Principal;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface PrincipalRoleAccessor {

	@Query("SELECT * from zyber.principal_roles WHERE tenant_id = :tenantId")
	Result<Principal> getPrincipalRoles(@Param("tenantId") UUID tenantId);
	
	@Query("SELECT * from zyber.principal_roles WHERE tenant_id = :tenantId AND principal_id = :principal_id")
	Principal getPrincipalRoles(@Param("tenantId") UUID tenantId, @Param("principal_id") UUID principal_id);
}
