package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.UserRole;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface UserRoleAccessor {
	@Query("SELECT * FROM zyber.user_role WHERE tenant_id = :tenantId AND role_id = :role_id")
	UserRole getUserRole(@Param("tenantId") UUID tenantId, @Param("role_id") UUID roleId);
	
	@Query("SELECT * FROM zyber.user_role")
	Result<UserRole> getUserRoles();
}
