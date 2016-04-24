package zyber.server.dao;

import java.util.Set;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

import zyber.server.ZyberUserSession;

@Table(keyspace = "zyber", name = "metadata")
public class MetaData extends InTenant {
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
    UUID path_id;

	@ClusteringColumn
	String key;

	Set<String> value;
    @Transient
	private ZyberUserSession zus;
    public ZyberUserSession getZus() {
		return zus;
	}

	public void setZus(ZyberUserSession zus) {
		this.zus = zus;
	}
	public MetaData() {
	}
	
	public MetaData(String key, Set<String> value, UUID path_id) {
		this.key = key;
		this.value = value;
		this.path_id = path_id;
	}

	public MetaData(String key, Set<String> value, UUID path_id, ZyberUserSession zus) {
		super();
		this.key = key;
		this.value = value;
		this.path_id = path_id;
		this.zus = zus;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Set<String> getValue() {
		return value;
	}

	public void setValue(Set<String> value) {
		this.value = value;
	}

	public UUID getPath_id() {
		return path_id;
	}

	public void setPath_id(UUID path_id) {
		this.path_id = path_id;
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
		MetaData other = (MetaData) obj;
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
