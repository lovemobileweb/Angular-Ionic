package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.Path;
import zyber.server.dao.PathType;

@Accessor
/**
 * N.B. Make sure every method you write here filters to exclude deleted.
 * For safety reasons, anything which needs to access deleted paths uses a separate accessor.
 */
public interface PathAccessor extends CommonPathAccessor {
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by name asc ALLOW filtering")
	Result<Path> getChildren(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	
	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false AND type = :type ALLOW filtering")
	Result<Path> getChildrenByType(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id, @Param("type") PathType type);

	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id in :parent_path_id and deleted = false ALLOW filtering")
	Result<Path> getChildrenIn(@Param("tenantId") UUID tenantId, @Param("parent_path_id") List<UUID> parent_path_ids);
	
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by name asc ALLOW filtering")
	Result<Path> getChildren_ByName_Asc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by name desc ALLOW filtering")
	Result<Path> getChildren_ByName_Desc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	
	@Query("SELECT * FROM zyber.path_orderby_created_date WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by created_date asc ALLOW filtering")
	Result<Path> getChildren_ByCreatedDate_Asc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	@Query("SELECT * FROM zyber.path_orderby_created_date WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by created_date desc ALLOW filtering")
	Result<Path> getChildren_ByCreatedDate_Desc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);

	@Query("SELECT * FROM zyber.path_orderby_modified_date WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id  and deleted = false order by modified_date asc ALLOW filtering")
	Result<Path> getChildren_ByModifiedDate_Asc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	@Query("SELECT * FROM zyber.path_orderby_modified_date WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id  and deleted = false order by modified_date desc ALLOW filtering")
	Result<Path> getChildren_ByModifiedDate_Desc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	
	@Query("SELECT * FROM zyber.path_orderby_size WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by size asc ALLOW filtering")
	Result<Path> getChildren_BySize_Asc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	@Query("SELECT * FROM zyber.path_orderby_size WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false order by size desc ALLOW filtering" )
	Result<Path> getChildren_BySize_Desc(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	
	
	
	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false ALLOW filtering")
	Result<Path> getChildrenByCreated(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
	
	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false ALLOW filtering")
	Result<Path> getChildrenByModified(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);

	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and deleted = false ALLOW filtering")
	Result<Path> getChildrenBySize(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id );
	
	@Query("SELECT * FROM zyber.path_orderby_name WHERE tenant_id = :tenantId AND parent_path_id = :parent_path_id and name = :childName and deleted = false ALLOW filtering")
	Result<Path> getChildNamed(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id, @Param("childName") String childName);

//	@Query("SELECT * FROM paths WHERE path_id = :path_id and deleted = false ALLOW filtering")
//	Path getPath(UUID tenantId, /*@Param("path_id")*/ UUID path_id);
	
	@Query("SELECT * FROM zyber.paths WHERE path_id = :path_id and deleted = false ALLOW filtering")
	Path getPath(@Param("path_id") UUID path_id);
//	Path __getPath(@Param("path_id") UUID path_id);
//	Path getPath(@Param("tenantId") UUID tenantId, UUID path_id) {
//		Path ret = __getPath(path_id);
//		if (ret == null) return null;
//		if (ret.getTenantId().equals(tenantId)) {
//			Logger.logHorribleExceptionHere!!!
//			return null;
//		}
//	}
// TODO: ASSERT THAT THE TENANT ID IS THE EXPECTED TENANT ID HERE!!
	
	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND path_id = :path_id and parent_path_id = :parent_path_id and deleted = false ALLOW filtering")
	Path getPathByParent(@Param("tenantId") UUID tenantId, @Param("path_id") UUID path_id,@Param("parent_path_id") UUID parent_path_id);

	@Query("SELECT * FROM zyber.paths WHERE tenant_id = :tenantId AND linked_id = :linked_id and deleted = false ALLOW filtering")
	Result<Path> getPathsLinked(@Param("tenantId") UUID tenantId, @Param("linked_id") UUID path_id);

	//Below here is probably safe not to filter down

	@Query("DELETE from zyber.paths WHERE tenant_id = :tenantId AND path_id= :path_id and parent_path_id = :parent_path_id")
	void deletePath(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id, @Param("path_id") UUID path_id);
	
	//@Query("DELETE from paths WHERE tenant_id = :tenantId AND parent_path_id= :parent_path_id")
//	void deleteParentPath(@Param("parent_path_id") UUID parent_path_id);

//	@Query("SELECT * FROM paths WHERE tenant_id = :tenantId")
//	public Result<Path> getAll(@Param("tenantId") UUID tenantId);

}
