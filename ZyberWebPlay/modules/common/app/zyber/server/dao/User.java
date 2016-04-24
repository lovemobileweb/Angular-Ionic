package zyber.server.dao;

import java.util.Date;
import java.util.UUID;

import org.joda.time.DateTime;
import zyber.driver.mapping.annotations.Index;
import zyber.server.CassandraMapperDelegate;
import zyber.server.ZyberUserSession;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "zyber", name = User.NAME)
public class User extends InTenant {
	public static final String NAME = "users";
	@PartitionKey(0)
	@Column(name = "tenant_id")
	UUID tenantId;

	@Index
	@ClusteringColumn
	@Column(name = "user_id")
	private UUID userId;

	@Index
	private String email;

	@Column(name = "associated_principal_id")
	private String associatedPrincipalId;

	@Column(name = "home_folder")
	private UUID homeFolder;

	@Column(name = "created_date")
	private Date createdDate;
	private String name;
	private String language;

	@Column(name = "user_role")
	private UUID userRole;
	@Column(name = "phone_number")
	private String phoneNumber;
	@Column(name = "country_code")
	private String countryCode;
	@Column(name = "two_factor")
	private boolean requireTwoFactor;
	@Column(name = "number_confirmed")
	private boolean numberConfirmed = false;
	//TODO figure out if we need this
	@Column(name = "nonce")
	private UUID nonce;

	@Column(name = "authy_id")
	private Integer authyID;

	public Integer getAuthyID() {
		return authyID;
	}

	public void setAuthyID(Integer authyID) {
		this.authyID = authyID;
	}

	public UUID getNonce() {
		return nonce;
	}

	public void setNonce(UUID nonce) {
		this.nonce = nonce;
	}

	public boolean isNumberConfirmed() {
		return numberConfirmed;
	}

	public void setNumberConfirmed(boolean numberConfirmed) {
		this.numberConfirmed = numberConfirmed;
	}

	public boolean isRequireTwoFactor() {
		return requireTwoFactor;
	}

	public void setRequireTwoFactor(boolean requireTwoFactor) {
		this.requireTwoFactor = requireTwoFactor;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	@Index
	private Boolean active;

	@Column(name = "passwordExpiry")
	private Date passwordExpiry;

	public User() {
	}

	public User(UUID user_id, String email, String associated_principal_id, UUID home_folder, Date created_date,
			String name) {
		super();
		this.userId = user_id;
		this.email = email;
		this.associatedPrincipalId = associated_principal_id;
		this.homeFolder = home_folder;
		this.createdDate = created_date;
		this.name = name;
		this.nonce = UUID.randomUUID();
	}

	public User(UUID user_id, String name, String email) {
		this.userId = user_id;
		this.name = name;
		this.email = email;
		this.nonce = UUID.randomUUID();
	}

	public Date getPasswordExpiry() {
		return passwordExpiry;
	}

	public void setPasswordExpiry(Date passwordExpiry) {
		this.passwordExpiry = passwordExpiry;
	}

	public Boolean expiredPassword() {
		return new DateTime().isAfter(new DateTime(passwordExpiry));
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID user_id) {
		this.userId = user_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAssociatedPrincipalId() {
		return associatedPrincipalId;
	}

	public void setAssociatedPrincipalId(String associated_principal_id) {
		this.associatedPrincipalId = associated_principal_id;
	}

	public UUID getHomeFolder() {
		return homeFolder;
	}

	public void setHomeFolder(UUID home_folder) {
		this.homeFolder = home_folder;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date created_date) {
		this.createdDate = created_date;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	/**
	 * The the users root path. If the root path is null, create it. Though this
	 * should probably be done with user creation.
	 */
	public Path getRootPath(ZyberUserSession zus) {
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		Path usersPath = getOrCreateUsersPath(zus);
		if (homeFolder == null) {
			
			Path newpath = new Path(homeFolder = UUID.randomUUID(), email, PathType.Directory,
					usersPath.getPathId(), new Date(), new Date(),new Date(), UUID.randomUUID(), null, zus);
			newpath.setZus(zus);
			zus.mapper(Path.class).save(newpath);
			if(this.getUserId()!=null){
			zus.mapper(User.class).save(this);
			}
			return newpath;
		} else {
			// find the Path object related to the home_folder and return
//			Path retPath = zus.mapper(Path.class).get(Path.ROOT_PATH_PARENT, homeFolder);
			Path retPath = pathAccessor.getPathByParent(homeFolder, usersPath.getPathId());
			retPath.setZus(zus);
			return retPath;
		}
	}
	
	private Path getOrCreateUsersPath(ZyberUserSession zus) {
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		Path usersPath = pathAccessor.getChildNamed(Path.ROOT_PATH_PARENT, Path.USERS_FOLDER).one();
		if(null == usersPath){
			Path rootPath = new Path(Path.ROOT_PATH_PARENT, "/", PathType.Directory,
					Path.ROOT_PATH_PARENT, new Date(), new Date(),new Date(), UUID.randomUUID(), null, zus);
			CassandraMapperDelegate<Path> mapper = zus.mapper(Path.class);
			mapper.save(rootPath);
			
			usersPath = new Path(UUID.randomUUID(), Path.USERS_FOLDER, PathType.Directory,
					Path.ROOT_PATH_PARENT, new Date(), new Date(),new Date(), UUID.randomUUID(), null, zus);
			mapper.save(usersPath);
			
			Path sharesPath = new Path(UUID.randomUUID(), Path.SHARES_FOLDER, PathType.Directory,
					Path.ROOT_PATH_PARENT, new Date(), new Date(),new Date(), UUID.randomUUID(), null, zus);
			mapper.save(sharesPath);
		}
		return usersPath;
	}

	public Path findPath(ZyberUserSession zus, UUID home_folder2) {
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		return pathAccessor.getPath(home_folder2);

	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public UUID getUserRole() {
		return userRole;
	}

	public void setUserRole(UUID userRole) {
		this.userRole = userRole;
	}

}
