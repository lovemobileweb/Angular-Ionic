package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import zyber.driver.mapping.annotations.Index;
import zyber.driver.mapping.annotations.View;
import zyber.server.dao.mapping.RequiresZyberUserSession;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(keyspace = GroupMember.KEYSPACE, name = GroupMember.NAME)
@View("CREATE MATERIALIZED VIEW group_member_by_principal AS\n" + 
		"		  SELECT tenant_id, group_id, member_principal_id, joined_date \n" + 
		"		  FROM group_member\n" + 
		"		  WHERE tenant_id IS NOT NULL AND group_id IS NOT NULL AND member_principal_id IS NOT NULL AND \n" + 
		"		  joined_date IS NOT NULL    \n" + 
		"		  PRIMARY KEY ((tenant_id, member_principal_id), group_id) \n" + 
		"         WITH CLUSTERING ORDER BY (group_id desc);\n;")
public class GroupMember extends RequiresZyberUserSession {

	public static final String NAME = "group_member";
	public static final String KEYSPACE = "zyber";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	
	@Index
	@PartitionKey(1)
	@Column(name = "group_id")
	private UUID groupId;

	@ClusteringColumn(0)
	@Column(name = "member_principal_id")
	private UUID memberPrincipalId;

//	@ClusteringColumn(1)
//	@Index
//	@Column(name = "member_principal_type")
//	private Principal.PrincipalType memberPrincipalType;
	
	@Column(name = "joined_date")
	private Date joinedDate;
	
	@Transient
	private String name;
	
	public GroupMember() {
	}
	
	

	public GroupMember(UUID groupId, UUID memberPrincipalId, Date joinedDate) {
		this.groupId = groupId;
		this.memberPrincipalId = memberPrincipalId;
		this.joinedDate = joinedDate;
	}

	public UUID getGroupId() {
		return groupId;
	}

	public void setGroupId(UUID groupId) {
		this.groupId = groupId;
	}

	public UUID getMemberPrincipalId() {
		return memberPrincipalId;
	}

	public void setMemberPrincipalId(UUID memberPrincipalId) {
		this.memberPrincipalId = memberPrincipalId;
	}

//	public Principal.PrincipalType getMemberPrincipalType() {
//		return memberPrincipalType;
//	}
//
//	public void setMemberPrincipalType(Principal.PrincipalType memberPrincipalType) {
//		this.memberPrincipalType = memberPrincipalType;
//	}



	public Date getJoinedDate() {
		return joinedDate;
	}



	public void setJoinedDate(Date joinedDate) {
		this.joinedDate = joinedDate;
	}



//	public Principal.PrincipalType getMemberPrincipalType() {
//		return memberPrincipalType;
//	}
//
//
//
//	public void setMemberPrincipalType(Principal.PrincipalType memberPrincipalType) {
//		this.memberPrincipalType = memberPrincipalType;
//	}



	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}
	
	
//	private void updateFlatMemberGroups(HashSet<UUID> groupsSeen, UUID groupUUID, UUID sourceId, CassandraMapperDelegate<GroupMemberFlat> fm, GroupMembersAccessor ga, boolean deleteMode) {
//		if (!groupsSeen.add(sourceId)) {
//			return; // Avoid infinite recursion when there are cycles of groups.
//		}
//		
//		for (GroupMember x:ga.getGroupMembers(sourceId)) {
//			if (x.getMemberPrincipalType().equals(PrincipalType.User)) {
//				GroupMemberFlat f = new GroupMemberFlat();
//				f.setTenantId(tenantId);
//				f.setGroupId(groupUUID);
//				f.setSourceGroupId(x.getGroupId());
//				f.setMemberPrincipalId(x.getMemberPrincipalId());
//				f.setMemberPrincipalType(x.getMemberPrincipalType());
//				if (deleteMode) {
//					fm.delete(f);
//				} else {
//					fm.save(f);
//				}
//			}else if(x.getMemberPrincipalType().equals(PrincipalType.Group)){
//				updateFlatMemberGroups(groupsSeen, groupUUID, x.memberPrincipalId, fm, ga, deleteMode);
//			}
//		}
//	}
	
//	public void saveAndUpdateFlatMembership(CassandraMapperDelegate<GroupMember> gm) {
//		CassandraMapperDelegate<GroupMemberFlat> fm = getZus().mapper(GroupMemberFlat.class);
//		GroupMembersAccessor ga = getZus().accessor(GroupMembersAccessor.class);
//		
//		for (GroupMember x:ga.getGroupMembers(this.getMemberPrincipalId())) {
//			if (x.getMemberPrincipalType().equals(PrincipalType.Group)) {
//				updateFlatMemberGroups(new HashSet<UUID>(), this.getGroupId(), x.getMemberPrincipalId(), fm, ga, false);
//			}else if(x.getMemberPrincipalType().equals(PrincipalType.User)){
//				GroupMemberFlat f = new GroupMemberFlat();
//				f.setTenantId(tenantId);
//				f.setGroupId(this.getGroupId());
//				f.setSourceGroupId(x.getGroupId());
//				f.setMemberPrincipalId(x.getMemberPrincipalId());
//				f.setMemberPrincipalType(x.getMemberPrincipalType());
//				fm.save(f);
//			}
//		}
//		gm.save(this);
//	}

//	public void deleteAndUpdateFlatMembership() {
//		deleteAndUpdateFlatMembership(getZus().mapper(GroupMember.class));
//	}
	
//	public void deleteAndUpdateFlatMembership(CassandraMapperDelegate<GroupMember> gm) {
//		CassandraMapperDelegate<GroupMemberFlat> fm = getZus().mapper(GroupMemberFlat.class);
//		GroupMembersAccessor ga = getZus().accessor(GroupMembersAccessor.class);
//		
//		GroupMembersFlatAccessor gmfa = getZus().accessor(GroupMembersFlatAccessor.class);
//		
//		for (GroupMember x:ga.getGroupMembers(this.getMemberPrincipalId())) {
//			if (x.getMemberPrincipalType().equals(PrincipalType.Group)) {
//				updateFlatMemberGroups(new HashSet<UUID>(), this.getGroupId(), x.getMemberPrincipalId(), fm, ga, true);
//			}else if(x.getMemberPrincipalType().equals(PrincipalType.User)){
//				gmfa.deleteGroupMember(groupId, x.getMemberPrincipalId());
//			}
//		}
//		
//		deleteGroupMembersBySource(groupId, memberPrincipalId, gmfa);
//
//		gm.delete(this);
//	}

//	private void deleteGroupMembersBySource(UUID groupId,
//			UUID memberPrincipalId, GroupMembersFlatAccessor gmfa) {
//		List<GroupMemberFlat> all = gmfa.getGroupsBySource(groupId, memberPrincipalId).all();
//		for(GroupMemberFlat gmf : all){
//			gmfa.deleteGroupMember(gmf.getGroupId(), memberPrincipalId);
//		}
//	}



	public UUID getTenantId() {
		return tenantId;
	}



	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String toString() {
		return "GroupMember [memberPrincipalId=" + memberPrincipalId + "]";
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result
				+ ((joinedDate == null) ? 0 : joinedDate.hashCode());
		result = prime
				* result
				+ ((memberPrincipalId == null) ? 0 : memberPrincipalId
						.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		GroupMember other = (GroupMember) obj;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (joinedDate == null) {
			if (other.joinedDate != null)
				return false;
		} else if (!joinedDate.equals(other.joinedDate))
			return false;
		if (memberPrincipalId == null) {
			if (other.memberPrincipalId != null)
				return false;
		} else if (!memberPrincipalId.equals(other.memberPrincipalId))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		return true;
	}
	
	
}
