package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

import zyber.driver.mapping.annotations.Index;
import zyber.server.ZyberUserSession;

@Table(keyspace = "zyber", name = "activity_timeline")
public class ActivityTimeline extends InTenant {
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
	

	@PartitionKey(1)
	@Index
	@Column(name = "path_id")
	UUID pathId;
	
//	@PartitionKey(2)if we use this,then some accessor queries will fail because we don't have the entire pk
	//FIXME improve maybe integrating with apache spark to do the select in a different cluster
	@Index
	@Column(name = "user_id")
	UUID userId;

	@ClusteringColumn(0)
	@Column(name = "activity_timestamp")
	Date activityTimestamp;

	String activity; //One of create, view, edit, change permissions
	String note; // Description of the action such as added user permissions: a,b,c for change permissions or others. No note necessary for create/view/edit. For an edit we write in the version number.

	public enum Action {
		Created("plus"),
		Edited("edit"),
		Restored("transfer"),//TODO What to do here
		Viewed("download"),
		Delete("trash"),
		Rename("retweet"), //TODO What to do here
		Unknown("question-sign"),
		Share("share"),
		Revoke("stop"),
		Login("login"),
		Moved("moved"),
		Copied("copied");

		private String icon;

		Action(String icon) {
			this.icon = icon;
		}

		public String getIcon() {
			return icon;
		}
	}

	@Transient
	private ZyberUserSession zus;
	@Transient
	private String username;

	public ZyberUserSession getZus() {
		return zus;
	}

	public void setZus(ZyberUserSession zus) {
		this.zus = zus;
	}

	public ActivityTimeline() {
	}

	public ActivityTimeline(UUID userId, UUID pathId, Date activityTimestamp, String activity, String note,
			ZyberUserSession zus) {
		super();
		this.userId = userId;
		this.pathId = pathId;
		this.activityTimestamp = activityTimestamp;
		this.activity = activity;
		this.note = note;
		this.zus = zus;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID pathId) {
		this.pathId = pathId;
	}

	public Date getActivityTimestamp() {
		return activityTimestamp;
	}

	public void setActivityTimestamp(Date activityTimestamp) {
		this.activityTimestamp = activityTimestamp;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public ActivityTimeline withUsername(String username){
		this.username = username;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public static ActivityTimeline empty(){
		ActivityTimeline empty = new ActivityTimeline(null,null,null,"Unknown","Unknown history",null);
		return empty.withUsername("Unknown User");
	}

	public String getActionIcon() {
		return Action.valueOf(activity).getIcon();
	}
	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

}