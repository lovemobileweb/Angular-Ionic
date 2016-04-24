package zyber.server.dao.mapping;

import java.util.Iterator;

import zyber.server.ZyberUserSession;

public class ZyberUserSessionFillingIterator<T extends IRequiresZyberUserSession> implements Iterator<T> {
	private final Iterator<T> wraps;
	private final ZyberUserSession zus;

	public ZyberUserSessionFillingIterator(Iterator<T> wraps, ZyberUserSession zus) {
		this.wraps = wraps;
		this.zus = zus;
	}

	@Override
	public boolean hasNext() {
		return wraps.hasNext();
	}

	@Override
	public T next() {
		T ret = wraps.next();
		ret.setZus(zus);
		return ret;
	}

}
