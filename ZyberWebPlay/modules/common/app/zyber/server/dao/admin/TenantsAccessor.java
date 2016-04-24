package zyber.server.dao.admin;

import java.util.UUID;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface TenantsAccessor {
	
	@Query("SELECT * FROM zyber_tenants.tenants")
	Result<Tenant> getTenants();
	
	@Query("SELECT * FROM zyber_tenants.tenants where tenant_id = :tenant_id")
	Tenant getTenantById(@Param("tenant_id") UUID tenantId);
	
	@Query("SELECT * FROM zyber_tenants.tenants where subdomain = :subdomain")
	Tenant getTenantBySubdomain(@Param("subdomain") String subdomain);
	
	@Query("DELETE from zyber_tenants.tenants where tenant_id= :tenant_id")
	void deleteTenant(@Param("tenant_id") UUID tenant_id);
	
}
