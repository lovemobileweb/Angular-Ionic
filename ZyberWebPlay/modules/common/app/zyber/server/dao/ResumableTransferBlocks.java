package zyber.server.dao;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = "resumable_transfer_blocks")
public class ResumableTransferBlocks  extends InTenant{
	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@PartitionKey(1)
	@Column(name = "resumable_transfer_id")
	private UUID resumableTransferId;
	
	@ClusteringColumn
	@Column(name = "chunk_number")
	private long chunkNumber;
	
	@Column(name = "chunk_data")
	private ByteBuffer chunkData;
	
	@Column(name = "chunk_size")
	private long chunkSize;
	
	public ResumableTransferBlocks() {
	}

	public ResumableTransferBlocks(UUID resumableTransferId, long chunkNumber,
			byte[] chunkData, long chunkSize) {
		this.resumableTransferId = resumableTransferId;
		this.chunkNumber = chunkNumber;
		this.chunkData = ByteBuffer.wrap(chunkData);
		this.chunkSize = chunkSize;
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

	public long getChunkNumber() {
		return chunkNumber;
	}

	public void setChunkNumber(long chunkNumber) {
		this.chunkNumber = chunkNumber;
	}

	public ByteBuffer getChunkData() {
		return chunkData;
	}

	public void setChunkData(ByteBuffer chunkData) {
		this.chunkData = chunkData;
	}

	public long getChunkSize() {
		return chunkSize;
	}

	public void setChunkSize(long chunkSize) {
		this.chunkSize = chunkSize;
	}
}
