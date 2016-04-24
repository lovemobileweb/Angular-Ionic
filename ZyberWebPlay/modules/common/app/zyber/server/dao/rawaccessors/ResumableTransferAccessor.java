package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.ResumableTransfer;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface ResumableTransferAccessor {
	
	@Query("SELECT * from zyber.resumable_transfer WHERE tenant_id = :tenantId AND resumable_transfer_id = :resumable_transfer_id")
	ResumableTransfer getResumable(@Param("tenantId") UUID tenantId, 
			@Param("resumable_transfer_id") UUID resumableTransferId);
}
