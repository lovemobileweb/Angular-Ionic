package zyber.server;

import java.util.UUID;

import zyber.server.dao.InTenant;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.Mapper.Option;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * TODO: Contribute an interface to cassandra, so that we can implement the
 * interface rather than create our own class.
 */
public class CassandraMapperDelegate<T extends InTenant> {
	final Mapper<T> mapper;
	final UUID tenantId;

	public TableMetadata getTableMetadata() {
		return mapper.getTableMetadata();
	}

	public MappingManager getManager() {
		return mapper.getManager();
	}

	public Statement saveQuery(T entity) {
		return mapper.saveQuery(entity);
	}

	public Statement saveQuery(T entity, Option... options) {
		return mapper.saveQuery(entity, options);
	}

	public void save(T entity) {
		entity.setTenantId(tenantId);
		mapper.save(entity);
	}

	public void save(T entity, Option... options) {
		entity.setTenantId(tenantId);
		mapper.save(entity, options);
	}

	public ListenableFuture<Void> saveAsync(T entity) {
		if (entity instanceof InTenant)
			entity.setTenantId(tenantId);
		return mapper.saveAsync(entity);
	}

	public ListenableFuture<Void> saveAsync(T entity, Option... options) {
		if (entity instanceof InTenant)
			entity.setTenantId(tenantId);
		return mapper.saveAsync(entity, options);
	}

	public Statement getQuery(Object... objects) {
		return mapper.getQuery(objects);
	}

	public T get(Object... objects) {
		return mapper.get(objects);
	}

	public ListenableFuture<T> getAsync(Object... objects) {
		return mapper.getAsync(objects);
	}

	public Statement deleteQuery(T entity, Option... options) {
		return mapper.deleteQuery(entity, options);
	}

	public Statement deleteQuery(T entity) {
		return mapper.deleteQuery(entity);
	}

	public Statement deleteQuery(Object... objects) {
		return mapper.deleteQuery(objects);
	}

	public void delete(T entity) {
		// todo: assert tenant?
		entity.setTenantId(tenantId);
		mapper.delete(entity);
	}

	public void delete(T entity, Option... options) {
		mapper.delete(entity, options);
	}

	public ListenableFuture<Void> deleteAsync(T entity) {
		return mapper.deleteAsync(entity);
	}

	public ListenableFuture<Void> deleteAsync(T entity, Option... options) {
		return mapper.deleteAsync(entity, options);
	}

	public void delete(Object... objects) {
		mapper.delete(objects);
	}

	public ListenableFuture<Void> deleteAsync(Object... objects) {
		return mapper.deleteAsync(objects);
	}

	public boolean equals(Object obj) {
		return mapper.equals(obj);
	}

	public int hashCode() {
		return mapper.hashCode();
	}

	public Result<T> map(ResultSet resultSet) {
		return mapper.map(resultSet);
	}

	public Result<T> mapAliased(ResultSet resultSet) {
		return mapper.mapAliased(resultSet);
	}

	public void setDefaultSaveOptions(Option... options) {
		mapper.setDefaultSaveOptions(options);
	}

	public void resetDefaultSaveOptions() {
		mapper.resetDefaultSaveOptions();
	}

	public void setDefaultGetOptions(Option... options) {
		mapper.setDefaultGetOptions(options);
	}

	public void resetDefaultGetOptions() {
		mapper.resetDefaultGetOptions();
	}

	public void setDefaultDeleteOptions(Option... options) {
		mapper.setDefaultDeleteOptions(options);
	}

	public void resetDefaultDeleteOptions() {
		mapper.resetDefaultDeleteOptions();
	}

	public String toString() {
		return mapper.toString();
	}

	public CassandraMapperDelegate(Mapper<T> mapper, UUID tenantId) {
		this.mapper = mapper;
		this.tenantId = tenantId;
	}

}