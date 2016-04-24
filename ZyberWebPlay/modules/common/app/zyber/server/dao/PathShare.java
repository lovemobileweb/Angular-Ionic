package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import zyber.driver.mapping.annotations.Index;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(keyspace = "zyber", name = PathShare.NAME)
public class PathShare extends InTenant {
	public static final UUID SURROGATE_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");


	public static final String NAME = "shares";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
//	@PartitionKey(1)//if we use this,then some accessor queries will fail because we don't have the entire pk
	//FIXME improve maybe integrating with apache spark to do the select in a different cluster
	@ClusteringColumn(0)
	@Column(name = "share_id")
	private UUID shareId;
	@ClusteringColumn(1)
	@Column(name = "share_type")
	private String shareType;
	@ClusteringColumn(2)
	@Column(name = "user_id")
	private UUID userId;
	@Column(name = "path_id")
	private UUID pathId;
	@Column(name = "expiry")
	private Date expiry;
	@Column(name = "password")
	private String password;

	@Transient
	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public PathShare withUsername(String username) {
		setUsername(username);
		return this;
	}

	public PathShare() {
	}

	public PathShare(UUID share_id, String share_type, UUID user_id, UUID path_id, Date expiry, String password) {
		super();
		shareId = share_id;
		shareType = share_type;
		this.userId = user_id;
		this.pathId = path_id;
		this.expiry = expiry;
		this.password = password;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID user_id) {
		this.userId = user_id;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID pathId) {
		this.pathId = pathId;
	}

	public String getShareType() {
		return shareType;
	}

	public void setShareType(String shareType) {
		this.shareType = shareType;
	}

	public Date getExpiry() {
		return expiry;
	}

	public void setExpiry(Date expiry) {
		this.expiry = expiry;
	}

	public boolean isPublic() {
		return "public".equals(shareType);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean hasUser() {
		return !SURROGATE_USER.equals(shareId);
	}

	public UUID getShareId() {
		return shareId;
	}

	public void setShareId(UUID shareId) {
		this.shareId = shareId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PathShare pathShare = (PathShare) o;

		if (shareId != null ? !shareId.equals(pathShare.shareId) : pathShare.shareId != null) return false;
		if (shareType != null ? !shareType.equals(pathShare.shareType) : pathShare.shareType != null) return false;
		if (userId != null ? !userId.equals(pathShare.userId) : pathShare.userId != null) return false;
		if (pathId != null ? !pathId.equals(pathShare.pathId) : pathShare.pathId != null) return false;
		if (expiry != null ? !expiry.equals(pathShare.expiry) : pathShare.expiry != null) return false;
		return !(password != null ? !password.equals(pathShare.password) : pathShare.password != null);

	}

	@Override
	public int hashCode() {
		int result = shareId != null ? shareId.hashCode() : 0;
		result = 31 * result + (shareType != null ? shareType.hashCode() : 0);
		result = 31 * result + (userId != null ? userId.hashCode() : 0);
		result = 31 * result + (pathId != null ? pathId.hashCode() : 0);
		result = 31 * result + (expiry != null ? expiry.hashCode() : 0);
		result = 31 * result + (password != null ? password.hashCode() : 0);
		return result;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}


}
