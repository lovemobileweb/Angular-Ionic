package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.UserKeys;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface UserKeysAccessor {

	@Query("SELECT * FROM zyber_secure.user_keys WHERE tenant_id = :tenantId AND user_id = :user_id")
	public UserKeys getUserKeysByUserId(@Param("tenantId") UUID tenantId, @Param("user_id") UUID userId);

//	@Query("SELECT * FROM zyber_secure.user_keys WHERE tenant_id = :tenantId")
//	public Result<UserKeys> getAll(@Param("tenantId") UUID tenantId);

//	@Query("SELECT * FROM users")
//	public ListenableFuture<Result<User>> getAllAsync();
}