package zyber.server.dao.rawaccessors;

import java.util.UUID;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface FileDataAccessor {
	//TODO support both the path and a version. Currently not possible due to ORDER BY with 2ndary indexes is not supported
//	@Query("SELECT * FROM file_data WHERE tenant_id = :tenantId AND path_id = :path_id and block_number >= :start_block and block_number <= :end_block and version = :version order by block_number")
	@Query("SELECT * FROM zyber.file_data WHERE tenant_id = :tenantId AND path_id = :path_id and version = :version and block_number >= :start_block and block_number <= :end_block order by block_number")
	ResultSet getBlocks(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id, @Param("version") long version, @Param("start_block") long startBlock,
			@Param("end_block") long endBlock
//											,@Param("version") long version
	);
	
	@Query("DELETE from zyber.file_data WHERE tenant_id = :tenantId AND path_id= :path_id and version = :version")
	void deleteFileData(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id, @Param("version") long version);

	//FIXME deletes all rows for all tenants
	@Query("TRUNCATE zyber.file_data")// WHERE tenant_id = :tenantId
	void deleteAll(/*@Param("tenantId") UUID tenantId*/);

	//DELETE from file_data WHERE tenant_id = :tenantId AND path_id=

}
