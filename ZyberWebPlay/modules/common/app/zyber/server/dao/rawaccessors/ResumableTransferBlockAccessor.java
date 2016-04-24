package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.ResumableTransferBlocks;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface ResumableTransferBlockAccessor {
	
	@Query("SELECT * from zyber.resumable_transfer_blocks WHERE tenant_id = :tenantId AND resumable_transfer_id = :resumable_transfer_id AND chunk_number = :chunk_number")
	ResumableTransferBlocks getBlock(@Param("tenantId") UUID tenantId, 
			@Param("resumable_transfer_id") UUID resumableTransferId, @Param("chunk_number")long chunkNumber);
	
	@Query("DELETE from zyber.resumable_transfer_blocks WHERE tenant_id = :tenantId AND resumable_transfer_id = :resumable_transfer_id")
	ResumableTransferBlocks deleteBlocks(@Param("tenantId") UUID tenantId, 
			@Param("resumable_transfer_id") UUID resumableTransferId);
}
