package com.zyber.webdavserver3;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;

import zyber.server.ZyberSession;
import zyber.server.ZyberUserSession;
import zyber.server.dao.Path;

public class WD123ResourceFactory  implements DavResourceFactory {
	private final ZyberSession z;

    public WD123ResourceFactory(final ZyberSession z) {
    	this.z = z;
    }

    @Override
    public DavResource createResource(DavResourceLocator locator,
                                      DavServletRequest request,
                                      DavServletResponse response) throws DavException {
        return createResource(locator, request.getDavSession(), request);
    }

    @Override
    public DavResource createResource(DavResourceLocator locator, DavSession davSession) throws DavException {
        return createResource(locator, davSession, null);
    }

    private DavResource createResource(DavResourceLocator locator, DavSession davSession, DavServletRequest request) throws DavException {
        final String localPath = getPath(locator);
        
        Principal ru = request.getUserPrincipal();
//        ZyberUserSession zus = new ZyberUserSession(z, ru == null ? "guest" : ru.getName());//FIXME 
        ZyberUserSession zus = new ZyberUserSession(z, ru == null ? "guest" : ru.getName(), null);//FIXME 
        Path path = zus.getRootPath().findChild(localPath);
        
        if (path == null) {
            final boolean isCreateCollection;
            isCreateCollection = request != null && DavMethods.isCreateCollectionRequest(request);
            return new NullResource(this, locator, davSession, zus, isCreateCollection);
        } else {
        	return new PathResource(this, locator, davSession, path);
        }
    }

    private String getPath(DavResourceLocator locator) {
        String pathStr = StringUtils.chomp(locator.getResourcePath(), "/");

        if (StringUtils.isEmpty(pathStr)) {
            pathStr = "/";
        }
        try {
			return URLDecoder.decode(pathStr, "utf8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
    }
}