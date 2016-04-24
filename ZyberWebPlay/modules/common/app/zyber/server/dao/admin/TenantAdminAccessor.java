package zyber.server.dao.admin;

import java.util.UUID;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface TenantAdminAccessor {

	@Query("SELECT * FROM zyber_tenants.administrators WHERE username = :username")
	public TenantAdmin getUserByUsername(@Param("username") String username);
	
	@Query("SELECT * FROM zyber_tenants.administrators WHERE user_id = :user_id")
	public TenantAdmin getUserById(@Param("user_id") UUID user_id);
	
	@Query("SELECT * FROM zyber_tenants.administrators")
	public Result<TenantAdmin> getUsers();
	
	@Query("DELETE from zyber_tenants.administrators where user_id= :user_id")
	void deleteTenant(@Param("user_id") UUID user_id);
}
