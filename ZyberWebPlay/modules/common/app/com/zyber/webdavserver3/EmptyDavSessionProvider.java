package com.zyber.webdavserver3;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

public class EmptyDavSessionProvider implements DavSessionProvider {

	@Override
	public boolean attachSession(WebdavRequest request) throws DavException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void releaseSession(WebdavRequest request) {
		// TODO Auto-generated method stub

	}

}
