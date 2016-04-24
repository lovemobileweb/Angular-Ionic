package zyber.server.dao.rawaccessors;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.Principal;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface PrincipalAccessor {

	@Query("SELECT * from zyber.principal WHERE tenant_id = :tenantId")
	Result<Principal> getPrincipals(@Param("tenantId") UUID tenantId);
	
	@Query("SELECT * from zyber.principal WHERE tenant_id = :tenantId AND principal_id = :principal_id")
	Principal getPrincipaById(@Param("tenantId") UUID tenantId, @Param("principal_id") UUID principal_id);
	
	@Query("SELECT * from zyber.principal WHERE tenant_id = :tenantId AND principal_id in :principal_id")
	Result<Principal> getPrincipaById(@Param("tenantId") UUID tenantId, @Param("principal_id") List<UUID> principal_id);
}
