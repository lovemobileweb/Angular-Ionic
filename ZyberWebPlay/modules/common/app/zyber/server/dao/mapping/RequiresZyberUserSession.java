package zyber.server.dao.mapping;

import zyber.server.ZyberUserSession;
import zyber.server.dao.InTenant;

public abstract class RequiresZyberUserSession extends InTenant implements IRequiresZyberUserSession {
	protected ZyberUserSession zus;

	@Override
	public ZyberUserSession getZus() {
		return zus;
	}

	@Override
	public void setZus(ZyberUserSession zus) {
		this.zus = zus;		
	}

}
