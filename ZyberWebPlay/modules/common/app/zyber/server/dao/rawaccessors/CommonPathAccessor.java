package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Param;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.Path;

public interface CommonPathAccessor {
  Result<Path> getChildren(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id);
  Result<Path> getChildNamed(@Param("tenantId") UUID tenantId, @Param("parent_path_id") UUID parent_path_id, @Param("childName") String childName);
  Path getPath(/*@Param("tenantId") UUID tenantId,*/ @Param("path_id") UUID path_id);
  Result<Path> getChildrenIn(@Param("tenantId") UUID tenantId, List<UUID> parent_path_ids);
}
