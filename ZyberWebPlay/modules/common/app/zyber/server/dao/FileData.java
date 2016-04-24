package zyber.server.dao;

import java.nio.ByteBuffer;
import java.util.UUID;

import zyber.server.ZyberUserSession;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(keyspace = "zyber", name = "file_data")
public class FileData extends InTenant { // extends BaseDAO {
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@PartitionKey(1)
	UUID path_id;

	@PartitionKey(2)
	long version;
	
	@ClusteringColumn(0)
	long block_number;
	
	ByteBuffer bytes;
	@Transient
	private ZyberUserSession zus;
	@Transient
	private int block_size;

	public ZyberUserSession getZus() {
		return zus;
	}

	public void setZus(ZyberUserSession zus) {
		this.zus = zus;
	}

	public FileData(UUID path_id, int block_number, ByteBuffer bytes, ZyberUserSession zus, int block_size, long version) {
		super();
		this.path_id = path_id;
		this.block_number = block_number;
		this.bytes = bytes;
		this.zus = zus;
		this.block_size = block_size;
		this.version = version;
	}

	public FileData(UUID path_id, long block_number, ByteBuffer bytes, ZyberUserSession zus, long version) {
		super();
		this.path_id = path_id;
		this.block_number = block_number;
		this.bytes = bytes;
		this.zus = zus;
		this.block_size = Integer.valueOf(zus.session.getConfigValue("BLOCK_SIZE"));
		this.version = version;
		// this.block_size = block_size ;
	}

	public UUID getPath_id() {
		return path_id;
	}

	public void setPath_id(UUID path_id) {
		this.path_id = path_id;
	}

	public long getBlock_number() {
		return block_number;
	}

	public void setBlock_number(long block_number) {
		this.block_number = block_number;
	}

	public ByteBuffer getBytes() {
		return bytes;
	}

	public void setBytes(ByteBuffer bytes) {
		this.bytes = bytes;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path_id == null) ? 0 : path_id.hashCode());
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
		FileData other = (FileData) obj;
		if (path_id == null) {
			if (other.path_id != null)
				return false;
		} else if (!path_id.equals(other.path_id))
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
