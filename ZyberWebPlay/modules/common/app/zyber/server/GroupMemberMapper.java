package zyber.server;

import java.util.UUID;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Mapper.Option;
import com.google.common.util.concurrent.ListenableFuture;

import zyber.server.dao.GroupMember;

public class GroupMemberMapper<T extends GroupMember> extends CassandraMapperDelegate<T> {

	public GroupMemberMapper(Mapper<T> mapper, UUID tenantId) {
		super(mapper, tenantId);
	}

	@Override
	public Statement deleteQuery(T entity, Option... options) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteQuery(entity, options);
	}

	@Override
	public Statement deleteQuery(T entity) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteQuery(entity);
	}

	@Override
	public Statement deleteQuery(Object... objects) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteQuery(objects);
	}

	@Override
	public void delete(T entity) {
		//delete the group members And flattened ones.
		super.delete(entity);
		throw new UnsupportedOperationException("Implement me if you want me.");
	}

	@Override
	public void delete(T entity, Option... options) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		super.delete(entity, options);
	}

	@Override
	public ListenableFuture<Void> deleteAsync(T entity) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteAsync(entity);
	}

	@Override
	public ListenableFuture<Void> deleteAsync(T entity, Option... options) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteAsync(entity, options);
	}

	@Override
	public void delete(Object... objects) {
		super.delete(objects);
	}

	@Override
	public ListenableFuture<Void> deleteAsync(Object... objects) {
		throw new UnsupportedOperationException("Implement me if you want me.");
//		return super.deleteAsync(objects);
	}
	
	
}
