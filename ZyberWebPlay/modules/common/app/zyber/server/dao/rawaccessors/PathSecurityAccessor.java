package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.PathSecurity;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface PathSecurityAccessor {

	@Query("SELECT * FROM zyber.path_security WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id AND path_id = :pathId")
	public Result<PathSecurity> getSecurityForPath(@Param("tenantId") UUID tenantId, 
			@Param("parent_path_id") UUID parentPathId, @Param("pathId") UUID pathId);

	@Query("SELECT * FROM zyber.path_security WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id AND path_id = :pathId AND principal_id = :principal_id")
	public PathSecurity getSecurityForPrincipal(@Param("tenantId") UUID tenantId, 
			@Param("parent_path_id") UUID parentPathId,  @Param("pathId") UUID pathId, @Param("principal_id")UUID princId);
	
	@Query("SELECT * FROM zyber.path_security WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id")
	public Result<PathSecurity> getSecurityForParentPath(@Param("tenantId") UUID tenantId, 
			@Param("parent_path_id") UUID parentPathId);
}