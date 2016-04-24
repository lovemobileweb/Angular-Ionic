package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

import zyber.server.dao.TrashedPath;

@Accessor
public interface TrashedPathAccessor {
	@Query("SELECT * FROM zyber.trash WHERE tenant_id = :tenantId AND user_id = :user_id")
	public Result<TrashedPath> getTrashedPathsForUser(@Param("tenantId") UUID tenantId, @Param("user_id") UUID user_id);
	@Query("SELECT * FROM zyber.trash WHERE tenant_id = :tenantId AND user_id = :user_id and path_id = :path_id")
	public TrashedPath getTrashedPath(@Param("tenantId") UUID tenantId, @Param("user_id") UUID user_id,@Param("path_id") UUID path_id);
	@Query("DELETE FROM zyber.trash WHERE tenant_id = :tenantId AND user_id = :user_id and path_id = :path_id")
	public void deleteTrashedPath(@Param("tenantId") UUID tenantId, @Param("user_id") UUID user_id,@Param("path_id") UUID path_id);
}