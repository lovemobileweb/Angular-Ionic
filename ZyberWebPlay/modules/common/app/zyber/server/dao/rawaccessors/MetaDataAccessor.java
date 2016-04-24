package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.MetaData;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface MetaDataAccessor {
	//FIXME does not contain path_id for partition key
//	@Query("SELECT * from metadata WHERE tenant_id = :tenantId AND value CONTAINS :value")
//	MetaData getMetaDataByValue(@Param("tenantId") UUID tenantId, @Param("value") String value);
	
	@Query("SELECT * from zyber.metadata WHERE tenant_id = :tenantId AND key = :key and path_id= :path_id")
	MetaData getValueByPathID(@Param("tenantId") UUID tenantId, @Param("key") String key,@Param("path_id") UUID path_id);

	@Query("SELECT * from zyber.metadata WHERE tenant_id = :tenantId AND path_id= :path_id")
	Result<MetaData> getPathMetadata(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id);
	
	@Query("DELETE from zyber.metadata WHERE tenant_id = :tenantId AND path_id= :path_id")
	void deletePathMetadata(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id);
	
	@Query("DELETE from zyber.metadata WHERE tenant_id = :tenantId AND path_id= :path_id and key = :key")
	void deletePathMetadataByKey(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id, @Param("key") String key);
}
