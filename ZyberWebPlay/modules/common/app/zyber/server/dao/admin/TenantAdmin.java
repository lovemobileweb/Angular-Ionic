package zyber.server.dao.admin;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(keyspace= TenantAdmin.KEYSPACE, name = TenantAdmin.NAME)
public class TenantAdmin {
	public static final String NAME = "administrators";
	public static final String KEYSPACE = "zyber_tenants";

	@PartitionKey
	@Column(name = "user_id")
	private UUID userId;
	
	private String username;
	
	@Column(name = "password_hash")
	private String password;
	
	@Column(name = "created_date")
	private Date createdDate;
	
	private String name;
	
	private String language;

	private Boolean active = true;
	
	@Transient
	private Boolean reset;
	
	public TenantAdmin() {
	}
	
	public TenantAdmin(String username, String password) {
		this.username = username;
		this.password = password;
	}

	

	public TenantAdmin(UUID userId, String username, String password, Date createdDate, String name, String language) {
		super();
		this.userId = userId;
		this.username = username;
		this.password = password;
		this.createdDate = createdDate;
		this.name = name;
		this.language = language;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean getReset() {
		return reset;
	}

	public void setReset(Boolean reset) {
		this.reset = reset;
	}
	
	

}
