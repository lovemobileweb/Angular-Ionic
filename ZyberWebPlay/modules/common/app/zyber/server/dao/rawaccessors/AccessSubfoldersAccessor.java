package zyber.server.dao.rawaccessors;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.AccessSubfolders;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface AccessSubfoldersAccessor {
	
	@Query("SELECT count(*) as access_count FROM zyber.access_subfolders WHERE tenant_id = :tenantId AND parent_path_id = :parent_id and principal_id in :principals")
	ResultSet countAccess(@Param("tenantId") UUID tenantId, @Param("parent_id") UUID parent_id, 
			@Param("principals") List<UUID> principals);
	
//	
//	@Query("UPDATE zyber.access_subfolders SET parent_path_id = :new_parent_path_id WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id AND path_id = :path_id")
//	void updateAccess(@Param("tenantId") UUID tenantId, @Param("new_parent_path_id") UUID new_parent_path_id, 
//			@Param("parent_path_id") UUID parent_path_id, @Param("path_id") UUID path_id);
	
	@Query("SELECT * FROM zyber.access_subfolders WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id AND path_id = :path_id")
	Result<AccessSubfolders> getAcess(@Param("tenantId") UUID tenantId, 
			@Param("parent_path_id") UUID parent_path_id, @Param("path_id") UUID path_id);
}
