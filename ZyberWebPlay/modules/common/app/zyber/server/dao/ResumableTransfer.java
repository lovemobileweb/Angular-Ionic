package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = "resumable_transfer")
public class ResumableTransfer extends InTenant {
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "resumable_transfer_id")
	private UUID resumableTransferId;
	
//	@ClusteringColumn
//	@Column(name = "parent_path_id")
//	private UUID parentPathId;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "total_chunks")
	private int totalChunks;
	
	@Column(name = "total_size")
	private long totalSize;
	
	public ResumableTransfer() {
	}

	public ResumableTransfer(UUID resumableTransferId, /*UUID parentPathId,*/
			String name, int totalChunks, long totalSize) {
		this.resumableTransferId = resumableTransferId;
//		this.parentPathId = parentPathId;
		this.name = name;
		this.totalChunks = totalChunks;
		this.totalSize = totalSize;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getResumableTransferId() {
		return resumableTransferId;
	}

	public void setResumableTransferId(UUID resumableTransferId) {
		this.resumableTransferId = resumableTransferId;
	}

//	public UUID getParentPathId() {
//		return parentPathId;
//	}
//
//	public void setParentPathId(UUID parentPathId) {
//		this.parentPathId = parentPathId;
//	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTotalChunks() {
		return totalChunks;
	}

	public void setTotalChunks(int totalChunks) {
		this.totalChunks = totalChunks;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}
}
