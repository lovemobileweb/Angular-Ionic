package zyber.server.dao;

import java.util.UUID;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;

public class UserDetails extends InTenant {
	public static final String NAME = "users";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;
//	@PartitionKey(1)
	@Column(name = "user_id")
	private UUID userId;

	byte[] photo;
	String address;
	String phone;

	public UserDetails(UUID user_id, byte[] photo, String address, String phone) {
		super();
		this.userId = user_id;
		this.photo = photo;
		this.address = address;
		this.phone = phone;
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

	public void setUserId(UUID user_id) {
		this.userId = user_id;
	}

	public byte[] getPhoto() {
		return photo;
	}

	public void setPhoto(byte[] photo) {
		this.photo = photo;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}
