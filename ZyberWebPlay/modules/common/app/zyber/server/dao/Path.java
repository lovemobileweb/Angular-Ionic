package zyber.server.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import zyber.driver.mapping.annotations.Index;
import zyber.driver.mapping.annotations.View;
import zyber.server.ZyberUserSession;
import zyber.server.dao.mapping.IRequiresZyberUserSession;

import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.ZyberUserSessionFillingResult;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@SuppressWarnings("unused")
@Table(keyspace = Path.KEYSPACE, name = Path.NAME)
@View("CREATE MATERIALIZED VIEW path_orderby_created_date AS\n"+
	"  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType\n"+
	"  FROM paths\n"+
	"  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND created_date IS NOT NULL\n"+    
	"  PRIMARY KEY ((tenant_id,parent_path_id),created_date,path_id)\n" +
	"  WITH CLUSTERING ORDER BY (created_date desc); \n")
	
@View("CREATE MATERIALIZED VIEW path_orderby_modified_date AS\n"+
	"  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType\n"+
	"  FROM paths\n"+
	"  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND modified_date IS NOT NULL\n"+
	"  PRIMARY KEY ((tenant_id,parent_path_id),modified_date,path_id)\n"+
	"  WITH CLUSTERING ORDER BY (modified_date desc);\n")

@View("CREATE MATERIALIZED VIEW path_orderby_size AS\n"+     
	"  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType\n"+
	"  FROM paths\n"+
	"  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL AND size IS NOT NULL\n" +
	"  PRIMARY KEY ((tenant_id,parent_path_id),size,path_id)\n" +
	"  WITH CLUSTERING ORDER BY (size desc);\n")

@View("CREATE MATERIALIZED VIEW path_orderby_name AS\n"+
	"  SELECT tenant_id, parent_path_id,path_id,name,type,deleted,created_date,modified_date,current_version,share_id,linked_id,size,mimeType\n"+
	"  FROM paths\n"+
	"  WHERE tenant_id IS NOT NULL AND parent_path_id IS NOT NULL AND path_id IS NOT NULL AND name IS NOT NULL\n"+
	"  PRIMARY KEY ((tenant_id,parent_path_id),name,path_id)\n"+
	"  WITH CLUSTERING ORDER BY (name desc);\n")

public class Path extends InTenant implements IRequiresZyberUserSession{
	public static final Comparator<Path> BY_NAME = (o1, o2) -> o1.getName().compareTo(o2.getName());

	public static final UUID ROOT_PATH_PARENT = UUID.fromString("00000000-0000-0000-0000-000000000000");
	public static final String SHARES_FOLDER = "shares";
	public static final String USERS_FOLDER = "users";
	public static final String NAME = "paths";
	public static final String KEYSPACE = "zyber";
	
	@PartitionKey(0)
	@Column(name = "tenant_id")
	private UUID tenantId;
	
	/** Note that this is part of the key for the path table. In a relational database this would not be so. */
	@PartitionKey(1)
	@Column(name = "parent_path_id")
	private UUID parentPathId;

	@Index
	@ClusteringColumn(0)
	@Column(name = "path_id")
	private UUID pathId;
	private String name;

	//@Enumerated(EnumType.STRING)
	@Index
	private PathType type;


	@Column(name = "created_date")
	private Date createdDate;

	@Column(name = "modified_date")
	private Date modifiedDate;

	@Column(name = "share_id")
	private UUID shareId;

	@Index
	@Column (name = "linked_id")
	private UUID linkedId;

	@Column (name = "deleted")
	private boolean deleted;
	
	@Column (name = "mimeType")
	private String mimeType;

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Transient
	private FileVersion cachedCurrentFileVersion;
	
	/**
	 * When the PathType is a file, it will have a blocksize and length.
	 * Internally file is split up into blocks of block size length.
	 */
	/*@Column(name = "block_size")
	private int blockSize;*/
	/** When the PathType is a file, it will have a blocksize and length. */
	private long size;
	@Column(name = "current_version")
	private Date currentVersion;

	@Transient
	private ZyberUserSession zus;

	public ZyberUserSession getZus() {
		return zus;
	}

	public void setZus(ZyberUserSession zus) {
		this.zus = zus;
	}

	public Path() {
		super();
	}

	public Path(UUID path_id, String name, PathType type, UUID parent_path_id,
			Date createdDate, Date modifiedDate,Date currentVersion, UUID share_id, UUID linked_id, ZyberUserSession zus) {
		super();
		this.pathId = path_id;
		this.name = name;
		this.type = type;
	//	this.metadata = metadata;
		this.parentPathId = parent_path_id;
		this.createdDate = createdDate;
		this.modifiedDate = modifiedDate;
		this.zus = zus;
		this.shareId = share_id;
		this.currentVersion = currentVersion;
		this.linkedId = linked_id;
	}

	public UUID getLinkedId() {
		return linkedId;
	}

	public void setLinkedId(UUID linkedId) {
		this.linkedId = linkedId;
	}

	public UUID getPathId() {
		return pathId;
	}

	public void setPathId(UUID path_id) {
		this.pathId = path_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public boolean isRoot() {
		return pathId.equals(ROOT_PATH_PARENT);
	}

	public boolean parentIsRoot() {
		return ROOT_PATH_PARENT.equals(getParentPathId());
	}


	public PathType getType() {
		return type;
	}

	public void setType(PathType type) {
		this.type = type;
	}

	public UUID getParentPathId() {
		return parentPathId;
	}

	public void setParentPathId(UUID parent_path_id) {
		this.parentPathId = parent_path_id;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public Date getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(Date currentVersion) {
		this.currentVersion = currentVersion;
	}
	
	/** Set's current version from the passed in current version, and keeps this as a cache of the current version. */
	public void setCurrentVersion(FileVersion currentVersion) {
		this.currentVersion = currentVersion.getVersion();
		this.cachedCurrentFileVersion = currentVersion;
	}
	public boolean isDirectory() {
		return this.getType() == PathType.Directory;
	}

	public boolean isFile() {
		return this.getType() == PathType.File;
	}

	public boolean isLinked() {
		return linkedId != null;
	}

	public Result<Path> getChild(String name) {
		checkZus();
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		return pathAccessor.getChildNamed(pathId, name);
	}

	/** Searches for */
	public Path findChild(String path) {
		String paths[] = path.split("/");
		return findChild(paths, 0);
	}

	private Path findChild(String paths[], int index) {
		if (index == paths.length)
			return this;
		String thisSegment = paths[index];
		while ((thisSegment == null || thisSegment.length() == 0) && index+1 < paths.length)
			thisSegment = paths[++index];
		if (index == paths.length)
			return this;
		if (thisSegment.length() == 0) {
			return this.findChild(paths, index + 1);
		} else {
			Path ret = getFirstChild(thisSegment);
			if (ret != null)
				return ret.findChild(paths, index + 1);
			else return ret;
		}
	}

	public Path getFirstChild(String name) {
		checkZus();
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		Result<Path> children = pathAccessor.getChildNamed(pathId, name);
		Path ret = children.isExhausted() ? null : children.one();
		if (ret != null)
			ret.setZus(zus);
		return ret;
	}

	public Path createChild(String name, PathType fileOrDirectory) {
		checkZus();
		Date createdDate = new Date();
		Path newPath = new Path(UUID.randomUUID(), name, fileOrDirectory, getPathId(), createdDate, createdDate, createdDate, UUID.randomUUID(), null,
				zus);
		zus.mapper(Path.class).save(newPath);
		return newPath;
	}


	/** When there is no current version, this will create one. */
	public HasPathOutputStream getOutputStream() {
		return getOutputStream(new Date());
	}

	public HasPathOutputStream getOutputStream(Date time) {
		FileVersion cfv = getCurrentFileVersion();
		if (cfv == null) {
			cfv = new FileVersion(zus, pathId, time);
		}

		return cfv.getOutputStream(this);
	}
	
	public InputStream getInputStream() {
		FileVersion cfv = getCurrentFileVersion();
		if (cfv == null)
			return new ByteArrayInputStream(new byte[0]);
		else 
			return cfv.getInputStream();
	}

	/**
	 * Note: This will return null when there is no version of the file.
	 */
	public FileVersion getCurrentFileVersion() {
		if (cachedCurrentFileVersion != null && cachedCurrentFileVersion.getVersion().equals(currentVersion))
			return cachedCurrentFileVersion;

		checkZus();
		FileVersionAccessor fileVersionAccessor = zus.accessor(FileVersionAccessor.class);

		if (this.getCurrentVersion() == null) return null;

		FileVersion ret = fileVersionAccessor.getVersion(this.getPathId(), this.getCurrentVersion());
		if (ret != null) ret.setZus(zus);
		return ret;
	}

	public Path createFile(String name) {
		return createChild(name, PathType.File);
	}

	public Path createDirectory(String name) {
		return createChild(name, PathType.Directory);
	}

	public ZyberUserSessionFillingResult<Path> getChildren() {
		checkZus();
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		return new ZyberUserSessionFillingResult<>(pathAccessor.getChildren(this.getPathId()),
				zus);
	}
	

	/** Marks the file as deleted. */
	public boolean delete() {
		return markDeletion(true);
	}

	public boolean restore() {
		return markDeletion(false);
	}

	public boolean purge() {
		checkZus();
		if(this.isFile()){
			PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
			pathAccessor.deletePath(getParentPathId(),getPathId());
		}
		else if(this.isDirectory()){
			checkZus();
			PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
			Result<Path> children = pathAccessor.getChildren(this.getPathId());
			for(Path child : children){
				child.setZus(zus);
				child.purge();
			}
		}
		return false;
	}

	private boolean markDeletion(boolean deleted) {
		checkZus();
		if(this.isFile()){
			setDeleted(deleted);
			zus.mapper(Path.class).save(this);
		}
		// At the moment we recursively mark as deleted which will suffer for high level deletes
		// Perhaps the better approach is to check the parent(s) always
		else if(this.isDirectory()){
			checkZus();
			PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
			Result<Path> children = pathAccessor.getChildren(this.getPathId());
			for(Path child : children){
				child.setZus(zus);
				child.markDeletion(deleted);
			}
			setDeleted(deleted);
			zus.mapper(Path.class).save(this);
		}
		return false;
	}

	private void checkZus() {
		if (zus == null)
            throw new IllegalStateException(
                    "ZyberUserSession required to call this method. Call x.setZus() on the object first.");
	}


	/*public int getBlockSize() {
		if (blockSize <= 0) {
			blockSize = Integer.valueOf(zus.session.getConfigValue(ZyberSession.CONF_KEY__DEFAULT_BLOCK_SIZE));
		}
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}*/

	public Path withZus(ZyberUserSession zus) {
		setZus(zus);
		return this;
	}

	public UUID getShareId() {
		return shareId;
	}

	public void setShareId(UUID shareId) {
		this.shareId = shareId;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long length) {
		this.size = length;
	}
	public Set<String> getMetaData(String key) {
		checkZus();
		MetaDataAccessor metadataAccessor = zus.accessor(MetaDataAccessor.class);
		System.out.println(key);
		System.out.println(this.getPathId().toString());
		MetaData getMetaData = metadataAccessor.getValueByPathID(key, this.getPathId());

		return getMetaData.getValue();
	}

	public void setMetaData(HashMap<String, String> metadata) {
		checkZus();
		if(!metadata.isEmpty()) {
			for (Map.Entry<String, String> entry : metadata.entrySet()) {
			    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			    setMetaData(entry.getKey(),entry.getValue());
			}
			
		}
		
	}

	public void setMetaData(String key, String value) {
		checkZus();
		Set<String> val = new HashSet<>();
		val.add(value);
		setMetaData(key,val);
	}
	
	public void setMetaData(String key, Set<String> value) {
		checkZus();
		MetaDataAccessor metadataAccessor = zus.accessor(MetaDataAccessor.class);
		MetaData md = metadataAccessor.getValueByPathID(key, this.getPathId());
		if(md != null){
			md.getValue().addAll(value);
		}
		MetaData mdata = new MetaData(key,value, this.getPathId(), zus);
		zus.mapper(MetaData.class).save(mdata);
	}

	public void rename(String name) {
		checkZus();
		setName(name);
		zus.mapper(Path.class).save(this);
	}

	public void move(UUID directory) {
		PathAccessor pathAccessor = zus.accessor(PathAccessor.class);
		pathAccessor.deletePath(getParentPathId(),getPathId());
//		zus.mapper(Path.class).delete(this);
		setParentPathId(directory);
		zus.mapper(Path.class).save(this);
	}
	
	public Path copy(Path dstDirectory) throws IOException {
		Path copiedFile = dstDirectory.createFile(name);
		checkZus();
		copiedFile.setMimeType(mimeType);
		zus.mapper(Path.class).save(this);
		
		HasPathOutputStream os = copiedFile.getOutputStream(copiedFile.getCurrentVersion());
		InputStream is = getInputStream();
		IOUtils.copy(is, os);
		is.close();
		os.close();
		return copiedFile;
	}
	
	public Path copy(Path dstDirectory, ZyberUserSession session) throws IOException {
		setZus(session);
		return copy(dstDirectory);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Path path = (Path) o;

		if (size != path.size) return false;
		if (pathId != null ? !pathId.equals(path.pathId) : path.pathId != null) return false;
		if (name != null ? !name.equals(path.name) : path.name != null) return false;
		if (type != path.type) return false;
		if (parentPathId != null ? !parentPathId.equals(path.parentPathId) : path.parentPathId != null) return false;
		if (createdDate != null ? !createdDate.equals(path.createdDate) : path.createdDate != null) return false;
		if (modifiedDate != null ? !modifiedDate.equals(path.modifiedDate) : path.modifiedDate != null) return false;
		if (shareId != null ? !shareId.equals(path.shareId) : path.shareId != null) return false;
		if (linkedId != null ? !linkedId.equals(path.linkedId) : path.linkedId != null) return false;
		return !(currentVersion != null ? !currentVersion.equals(path.currentVersion) : path.currentVersion != null);

	}

	@Override
	public int hashCode() {
		int result = pathId != null ? pathId.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (parentPathId != null ? parentPathId.hashCode() : 0);
		result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
		result = 31 * result + (modifiedDate != null ? modifiedDate.hashCode() : 0);
		result = 31 * result + (shareId != null ? shareId.hashCode() : 0);
		result = 31 * result + (linkedId != null ? linkedId.hashCode() : 0);
		result = 31 * result + (int) (size ^ (size >>> 32));
		result = 31 * result + (currentVersion != null ? currentVersion.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "'" + name + '\'' + " (" + pathId + ')';
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}
}
