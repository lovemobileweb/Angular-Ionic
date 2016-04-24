package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.User;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface UserAccessor {
	
	@Query("SELECT * FROM zyber.users WHERE tenant_id = :tenantId AND email = :email")
	public User getUserByEmail(@Param("tenantId") UUID tenantId, @Param("email") String email);

	@Query("SELECT * FROM zyber.users WHERE tenant_id = :tenantId AND user_id = :userId")
	public User getOnePosition(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("SELECT * FROM zyber.users WHERE tenant_id = :tenantId AND user_id = :user_id") 
    public User getById(@Param("tenantId") UUID tenantId, @Param("user_id") UUID user_id);


	@Query("UPDATE zyber.users SET name = :name, email = :email WHERE tenant_id = :tenantId AND user_id = :id")
	public void updateUser(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("name") String name, @Param("email") String email);

	@Query("UPDATE zyber.users SET active = false WHERE tenant_id = :tenantId AND user_id = :id")
	public void deleteUser(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
	/*
	 * @Query("UPDATE users SET addresses[:name]=:address WHERE tenant_id = :tenantId AND id = :id")
	 * ResultSet addAddress(@Param("id") UUID id, @Param("name") String
	 * addressName, @Param("address") Address address);
	 */

	@Query("SELECT * FROM zyber.users where tenant_id = :tenantId")
	public Result<User> getAll(@Param("tenantId") UUID tenantId);
	
	@Query("SELECT * FROM zyber.users WHERE tenant_id = :tenantId AND active = true;")
	public Result<User> getActiveUsers(@Param("tenantId") UUID tenantId);

	// @Query("SELECT * FROM users")
	// public ListenableFuture<Result<User>> getAllAsync();
}