package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

import zyber.server.dao.PathShare;

@Accessor
public interface PathShareAccessor {

	@Query("SELECT * FROM zyber.shares WHERE tenant_id = :tenantId AND share_id = :share_id")
	public Result<PathShare> getShareForId(@Param("tenantId") UUID tenantId, @Param("share_id") UUID share_id);
	//Possibly add some more fine grained access later. For now, we work based on retrieving everything and filtering in memory

	//TODO eventually we'll want to restructure this to be more efficient
	@Query("SELECT * from zyber.shares WHERE tenant_id = :tenantId")
	public Result<PathShare> listAllShares(@Param("tenantId") UUID tenantId);

	@Query("delete FROM zyber.shares WHERE tenant_id = :tenantId AND share_id = :share_id")
	public void deleteForPath(@Param("tenantId") UUID tenantId, @Param("share_id") UUID share_id);
	
	// WHERE tenant_id = :tenantId
	//@Param("tenantId") UUID tenantId
	@Query("TRUNCATE shares")
	public void wipeShares();
}