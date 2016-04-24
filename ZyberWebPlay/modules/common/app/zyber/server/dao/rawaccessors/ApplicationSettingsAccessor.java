package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.ApplicationSetting;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
@Accessor
public interface ApplicationSettingsAccessor {
	@Query("SELECT * FROM zyber.application_settings WHERE tenant_id = :tenantId AND key = :key")
	ApplicationSetting getGlobalSetting(@Param("tenantId") UUID tenantId, @Param("key") String key);

	@Query("SELECT * FROM zyber.application_settings WHERE tenant_id = :tenantId AND key = :key")
	ApplicationSetting getTenantSetting(@Param("tenantId") UUID tenantId, @Param("key") String key);
	
}
