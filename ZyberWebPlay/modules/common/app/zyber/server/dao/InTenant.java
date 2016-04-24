package zyber.server.dao;

import java.util.UUID;


public abstract class InTenant {
	
	abstract public UUID getTenantId();
	abstract public void setTenantId(UUID tenantId);

}
