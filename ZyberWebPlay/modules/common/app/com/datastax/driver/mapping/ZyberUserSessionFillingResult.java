package com.datastax.driver.mapping;

import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ExecutionInfo;

import zyber.server.ZyberUserSession;
import zyber.server.dao.mapping.IRequiresZyberUserSession;
import zyber.server.dao.mapping.ZyberUserSessionFillingIterator;

public class ZyberUserSessionFillingResult<T extends IRequiresZyberUserSession> implements Iterable<T> {
	private final Result<T> wraps;
	private final ZyberUserSession zus;

	public ZyberUserSessionFillingResult(Result<T> wraps, ZyberUserSession zus) {
		this.wraps = wraps;
		this.zus = zus;
	}

	public int hashCode() {
		return wraps.hashCode();
	}

	public boolean isExhausted() {
		return wraps.isExhausted();
	}

	public T one() {
		T ret = wraps.one();
		ret.setZus(zus);
		return ret;
	}

	public List<T> all() {
		List<T> ret = wraps.all();
		for (T e : ret) {
			e.setZus(zus);
		}
		return ret;
	}

	public boolean equals(Object obj) {
		return wraps.equals(obj);
	}

	public Iterator<T> iterator() {
		return new ZyberUserSessionFillingIterator<>(wraps.iterator(), zus);
	}

	public ExecutionInfo getExecutionInfo() {
		return wraps.getExecutionInfo();
	}

	public String toString() {
		return wraps.toString();
	}

}
