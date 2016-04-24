package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import zyber.server.ZyberUserSession;

@Table(keyspace = Principal.KEYSPACE, name = Principal.NAME)
public class Principal extends InTenant {
	public static final String NAME = "principal";
	public static final String KEYSPACE = "zyber";

	public enum PrincipalType{
		User, Group
	}
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@ClusteringColumn
	@Column(name = "principal_id")
	private UUID principalId;
	
	@Column(name = "type")
	private PrincipalType type;
	
	@Column(name = "root_folder")
	private UUID rootFolder;
	
	@Column(name = "created_date")
	private Date createdDate;
	
	@Column(name = "active")
	private Boolean active = true;
	
	@Column(name = "displayName")
	private String displayName;
	
	public Principal(){
	}

	public Principal(UUID principalId, PrincipalType type, UUID rootFolder, Date createdDate) {
		this.principalId = principalId;
		this.type = type;
		this.rootFolder = rootFolder;
		this.createdDate = createdDate;
	}

	public UUID getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(UUID principalId) {
		this.principalId = principalId;
	}

	public PrincipalType getType() {
		return type;
	}

	public void setType(PrincipalType type) {
		this.type = type;
	}

	public UUID getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(UUID rootFolder) {
		this.rootFolder = rootFolder;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public Path getRootPath(ZyberUserSession zus) {
		if (rootFolder == null) {
			Path newpath = new Path(rootFolder = UUID.randomUUID(), "", PathType.Directory,
					Path.ROOT_PATH_PARENT, new Date(), new Date(),new Date(), UUID.randomUUID(), null, zus);
			newpath.setZus(zus);
			zus.mapper(Path.class).save(newpath);
			zus.mapper(Principal.class).save(this);
			return newpath;
		} else {
			// find the Path object related to the home_folder and return
			Path retPath = zus.mapper(Path.class).get(Path.ROOT_PATH_PARENT, rootFolder);
			retPath.setZus(zus);
			return retPath;
		}
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
