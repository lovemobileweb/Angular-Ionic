package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = ViewerInfo.NAME)
public class ViewerInfo {

	public static final String NAME = "viewer_info";
	
	@PartitionKey
	@Column(name = "token_id")
	private UUID tokenId;
	
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@Column(name = "user_id")
	private UUID userId;
	
	@Column(name = "file_id")
	private UUID fileId;
	
	@Column(name = "created_time")
	private Date createdTime;
	
	public ViewerInfo() {
	}

	public ViewerInfo(UUID tokenId, UUID tenantId, UUID userId, UUID fileId,
			Date createdTime) {
		this.tokenId = tokenId;
		this.tenantId = tenantId;
		this.userId = userId;
		this.fileId = fileId;
		this.createdTime = createdTime;
	}

	public UUID getTokenId() {
		return tokenId;
	}

	public void setTokenId(UUID tokenId) {
		this.tokenId = tokenId;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getFileId() {
		return fileId;
	}

	public void setFileId(UUID fileId) {
		this.fileId = fileId;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}
	
	
}
