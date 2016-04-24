package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import zyber.server.dao.PasswordHistory;
import zyber.server.dao.UserKeys;

import java.util.UUID;

@Accessor
public interface PasswordHistoryAccessor {

	@Query("SELECT * FROM zyber_secure.password_history WHERE tenant_id = :tenantId AND user_id = :user_id")
	public Result<PasswordHistory> getPasswordHistoryForUser(@Param("tenantId") UUID tenantId, @Param("user_id") UUID userId);

//	@Query("SELECT * FROM zyber_secure.user_keys WHERE tenant_id = :tenantId")
//	public Result<UserKeys> getAll(@Param("tenantId") UUID tenantId);

//	@Query("SELECT * FROM users")
//	public ListenableFuture<Result<User>> getAllAsync();
}