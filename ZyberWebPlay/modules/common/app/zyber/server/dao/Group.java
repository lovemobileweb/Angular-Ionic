package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import zyber.driver.mapping.annotations.Index;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = Group.KEYSPACE, name = Group.NAME)
public class Group extends InTenant {

	public static final String NAME = "group";
	public static final String KEYSPACE = "zyber";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@ClusteringColumn
	@Index
	@Column(name = "group_id")
	private UUID groupId;

//	@Index
//	@Column(name = "owner_principal_id")
//	private UUID ownerPrincipalId;
	
	@Index
	@Column(name = "name")
	private String name;
	
	@Column(name = "created_date")
	private Date createdDate;
	
	private Integer members;
	
//	@Transient
//	private User owner;
	
	public Group(){
	}

	public Group(UUID groupId, /*UUID ownerPrincipalId, */ String name, Date createdDate) {
		this.groupId = groupId;
//		this.ownerPrincipalId = ownerPrincipalId;
		this.name = name;
		this.createdDate = createdDate;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public void setGroupId(UUID groupId) {
		this.groupId = groupId;
	}

//	public UUID getOwnerPrincipalId() {
//		return ownerPrincipalId;
//	}
//
//	public void setOwnerPrincipalId(UUID ownerPrincipalId) {
//		this.ownerPrincipalId = ownerPrincipalId;
//	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

//	public Group withOwner(User owner){
//		setOwner(owner);
//		return this;
//	}

//	public User getOwner() {
//		return owner;
//	}
//
//	public void setOwner(User owner) {
//		this.owner = owner;
//	}

	public Integer getMembers() {
		return members;
	}

	public void setMembers(Integer members) {
		this.members = members;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result
				+ ((tenantId == null) ? 0 : tenantId.hashCode());
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
		Group other = (Group) obj;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		return true;
	}
	
	
	
}
