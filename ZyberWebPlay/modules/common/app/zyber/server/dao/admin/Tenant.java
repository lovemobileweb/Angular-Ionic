package zyber.server.dao.admin;

import java.util.Date;
import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = Tenant.KEYSPACE, name = Tenant.NAME)
public class Tenant {
	public static final String NAME = "tenants";
	public static final String KEYSPACE = "zyber_tenants";

	@PartitionKey
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	@Column(name = "tenant_name")
	private String tenantName;
	
	@Column(name = "contact_name")
	private String contactName;
	
	@Column(name = "contact_phone")
	private String contactPhone;
	
	@Column(name = "contact_email")
	private String contactEmail; 
	
	@Column(name = "created_date")
	private Date createdDate;
	
	private String subdomain;

	
	public Tenant() {
	}
	
	

	public Tenant(UUID tenantId, String subdomain) {
		super();
		this.tenantId = tenantId;
		this.subdomain = subdomain;
	}



	public Tenant(UUID tenantId, String tenantName, String contactName, String contactPhone, String contactEmail,
			Date createdDate) {
		this.tenantId = tenantId;
		this.tenantName = tenantName;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.contactEmail = contactEmail;
		this.createdDate = createdDate;
	}
	
	public Tenant(UUID tenantId, String tenantName, String contactName, String contactPhone, String contactEmail,
			Date createdDate, String subdomain) {
		this.tenantId = tenantId;
		this.tenantName = tenantName;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.contactEmail = contactEmail;
		this.createdDate = createdDate;
		this.subdomain = subdomain;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getSubdomain() {
		return subdomain;
	}

	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}
}
