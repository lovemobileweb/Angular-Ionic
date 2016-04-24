package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import zyber.server.dao.mapping.RequiresZyberUserSession;


@Table(keyspace = "zyber", name = "application_settings")
public class ApplicationSetting extends RequiresZyberUserSession {
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "key")
	String key;

	@Column(name = "value")
	String value;
	public ApplicationSetting() {
		
	}
	public ApplicationSetting(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationSetting other = (ApplicationSetting) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	public UUID getTenantId() {
		return tenantId;
	}
	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

}
