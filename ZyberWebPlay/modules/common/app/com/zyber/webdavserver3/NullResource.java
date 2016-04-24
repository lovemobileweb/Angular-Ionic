package com.zyber.webdavserver3;

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;

import zyber.server.ZyberUserSession;

public class NullResource extends PathResource {
    private final boolean collection;

    public NullResource(WD123ResourceFactory factory, DavResourceLocator locator, DavSession davSession,
			ZyberUserSession zus, boolean isCreateCollection) {
    	super(factory, locator, davSession, null);
        this.collection = isCreateCollection;
	}

	@Override
    public boolean isCollection() {
        return collection;
    }
    
	@Override
	public boolean exists() {
		return false;
	}
}
