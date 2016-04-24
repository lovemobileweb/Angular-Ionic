package zyber.server.dao.rawaccessors;

import java.util.Date;
import java.util.UUID;

import zyber.server.dao.FileVersion;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface FileVersionAccessor {
	@Query("SELECT * FROM zyber_secure.file_version WHERE tenant_id = :tenantId AND path_id = :pathId AND version = :ver")
	FileVersion getVersion(@Param("tenantId") UUID tenantId, @Param("pathId") UUID pathId, @Param("ver") Date versionTimestamp);

	@Query("SELECT * FROM zyber_secure.file_version WHERE tenant_id = :tenantId AND path_id = :pathId")
	Result<FileVersion> getAllVersions(@Param("tenantId") UUID tenantId, @Param("pathId") UUID pathId);
}
