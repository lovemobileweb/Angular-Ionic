package com.zyber.webdavserver3;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;

import zyber.server.dao.Path;

import com.datastax.driver.mapping.ZyberUserSessionFillingResult;

public class PathResource implements DavResource {
	private static final String COMPLIANCE_CLASS =
			DavCompliance.concatComplianceClasses(new String[]{DavCompliance._2_});

	private static final String SUPPORTED_METHODS
	= "OPTIONS, GET, HEAD, POST, TRACE, MKCOL, COPY, PUT, DELETE, MOVE, PROPFIND";

	private final WD123ResourceFactory factory;
	private final DavResourceLocator locator;
	private final DavSession davSession;
	private final Path zPath;

	private final String path;

	private LockManager lockManager = new SimpleLockManager();

	private DavPropertySet properties = null;

	public PathResource(
			WD123ResourceFactory factory,
			DavResourceLocator locator,
			DavSession davSession,
			Path zPath) {
		this.factory = factory;
		this.locator = locator;
		this.davSession = davSession;
		this.zPath = zPath;

		String pathStr = StringUtils.chomp(locator.getResourcePath(), "/");

		if (StringUtils.isEmpty(pathStr)) {
			pathStr = "/";
		}
		try {
			this.path = URLDecoder.decode(pathStr, "utf8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}


	@Override
	public String getComplianceClass() {
		return COMPLIANCE_CLASS;
	}

	@Override
	public String getSupportedMethods() {
		return SUPPORTED_METHODS;
	}

	@Override
	public boolean exists() {
		return true; 
		//return cassandraService.resourceExists(getPath());
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public DavResourceLocator getLocator() {
		return locator;
	}

	@Override
	public String getResourcePath() {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String getHref() {
		return getPath();
	}

	@Override
	public long getModificationTime() {
		return zPath.getModifiedDate().getTime();
	}

	@Override
	public DavPropertyName[] getPropertyNames() {
		initProperties();
		return properties.getPropertyNames();
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		initProperties();
		return properties.get(name);
	}

	@Override
	public DavPropertySet getProperties() {
		initProperties();
		return properties;
	}

	@Override
	public void setProperty(DavProperty<?> davProperty) throws DavException {
		initProperties();
	}

	@Override
	public void removeProperty(DavPropertyName name) throws DavException {
		initProperties();
		properties.remove(name);
	}

	@Override
	public MultiStatusResponse alterProperties(List<? extends PropEntry> propEntries) throws DavException {
		return null;
	}

	@Override
	public DavResource getCollection() { 
		//final DavResourceLocator newLocator = locator.getFactory()
		//		.createResourceLocator(locator.getPrefix(), getParentDirectory(getPath()));
		throw new IllegalStateException("required to write files.");
//		try {
//			return factory.createResource(newLocator, getSession());
//		} catch (DavException ex) {
//			throw new RuntimeException(ex);
//		}
	}

	@Override
	public void move(DavResource davResource) throws DavException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void copy(DavResource davResource, boolean b) throws DavException {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public boolean isLockable(Type type, Scope scope) {
		return false;
	}

	@Override
	public boolean hasLock(Type type, Scope scope) {
		return false;
	}

	@Override
	public ActiveLock getLock(Type type, Scope scope) {
		return lockManager.getLock(type, scope, this);
	}

	@Override
	public ActiveLock[] getLocks() {
		return new ActiveLock[0];
	}

	@Override
	public ActiveLock lock(LockInfo lockInfo) throws DavException {
		return lockManager.createLock(lockInfo, this);
	}

	@Override
	public ActiveLock refreshLock(LockInfo lockInfo, String s) throws DavException {
		return lockManager.refreshLock(lockInfo, s, this);
	}

	@Override
	public void unlock(String s) throws DavException {
	}

	@Override
	public void addLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	@Override
	public DavResourceFactory getFactory() {
		return factory;
	}

	@Override
	public DavSession getSession() {
		return davSession;
	}

	public String getPath() {
		return path;
	}


	private void initProperties() {
		if (properties != null) {
			return;
		}
		this.properties = createProperties();
	}

	protected DavPropertySet createProperties() {
		final DavPropertySet localProperties = new DavPropertySet();
		SimpleDateFormat simpleFormat = (SimpleDateFormat) DavConstants.modificationDateFormat.clone();
		simpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		localProperties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, simpleFormat.format(new Date())));

		if (getDisplayName() != null) {
			localProperties.add(new DefaultDavProperty<String>(DavPropertyName.DISPLAYNAME, getDisplayName()));
		}

		return localProperties;
	}


	@Override
	public boolean isCollection() {
		return zPath.isDirectory();
	}


	@Override
	public void spool(OutputContext outputContext) throws IOException {
		try (OutputStream out = outputContext.getOutputStream()) {
			try (InputStream in=new BufferedInputStream(zPath.getInputStream(),32000)) {
				IOUtils.copy(in,out);
			}
		} 
	}


	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		throw new IllegalStateException("what to do???");
	}


	@Override
	public DavResourceIterator getMembers() {
		if (!isCollection()) {
			return new DavResourceIteratorImpl(Collections.<DavResource>emptyList());
		}

		final ZyberUserSessionFillingResult<Path> pr = zPath.getChildren();
		return new DavResourceIterator() {

			@Override
			public DavResource next() {
				Path nextPath = pr.one();

				DavResourceLocator locator2 = getLocator();
				DavResourceLocator newLocator = locator2.getFactory().createResourceLocator(locator2.getPrefix(),
						locator2.getWorkspacePath(),
						StringUtils.chomp(getPath(), "/") + "/" + nextPath.getName(),
						false);

				return new PathResource(factory, newLocator, davSession, nextPath);
			}

			@Override
			public boolean hasNext() {
				return !pr.isExhausted();
			}

			@Override
			public int size() {
				throw new IllegalStateException("Necessary?");
			}

			@Override
			public DavResource nextResource() {
				return next();
			}
		};
	}


	@Override
	public void removeMember(DavResource member) throws DavException {
		// TODO Auto-generated method stub

	}
}
