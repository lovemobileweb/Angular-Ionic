package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.Path;

@Accessor
public interface PathIncludingDeletedAccessor extends CommonPathAccessor {
	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id in :parent_path_id ALLOW filtering")
	Result<Path> getChildrenIn(@Param("tenantId") UUID tenantId, @Param("parent_path_id") List<UUID> parent_path_ids);
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id order by name asc ALLOW filtering")
	@Override
	Result<Path> getChildren(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	@Override
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and name = :childName ALLOW filtering")
	Result<Path> getChildNamed(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id, @Param("childName") String childName);

	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND path_id = :path_id ALLOW filtering")
	Path getPath(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id);
}