package com.zyber.webdavserver3;

import org.apache.jackrabbit.webdav.simple.LocatorFactoryImpl;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zyber.server.ZyberSession;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import java.lang.invoke.MethodHandles;

@SuppressWarnings("serial")
public class WD123Servlet extends SimpleWebdavServlet {
	public final static Logger log	= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

	@Override
	public Repository getRepository() {
		/** Not requried?? */
		return null;
	}

	
	
	final ZyberSession zs;
    public WD123Servlet(ZyberSession zs) {
    	this.zs = zs;
    }
    public WD123Servlet() {
    	this(ZyberSession.getZyberSession());
    }
    
    @Override
    public void init() throws ServletException {
    	super.init();
        setLocatorFactory(new LocatorFactoryImpl(getPathPrefix()));
        setDavSessionProvider(new EmptyDavSessionProvider()); // Might need to fix this up?
        setResourceFactory(new WD123ResourceFactory(zs));

    }
    
    
    
    


}
