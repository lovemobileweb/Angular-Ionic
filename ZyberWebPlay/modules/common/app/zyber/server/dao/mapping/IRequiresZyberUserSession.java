package zyber.server.dao.mapping;

import zyber.server.ZyberUserSession;

/**
 * See also the related class located in a different package so it can be
 * extended. ZyberUserSessionFillingResult.java
 */
public interface IRequiresZyberUserSession {
	
	public ZyberUserSession getZus();

	public void setZus(ZyberUserSession zus);
	
}
