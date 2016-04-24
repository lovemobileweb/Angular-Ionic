package zyber.server.dao.admin;

import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
@Accessor
public interface TenantsSettingsAccessor {
	@Query("SELECT * FROM zyber_tenants.application_settings WHERE key = :key")
	TennantsSetting getSetting(@Param("key") String key);

}
