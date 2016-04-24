package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.SecurityType;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface SecurityTypeAccessor {

	@Query("SELECT * FROM zyber.security_type WHERE tenant_id = :tenantId AND security_id = :security_id")
	SecurityType getSecurityType(@Param("tenantId") UUID tenantId, @Param("security_id") UUID securityId);
	
	@Query("SELECT * FROM zyber.security_type")
	Result<SecurityType> getSecurityTypes();
}
