package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.TermStoreTerm;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface TermStoreTermAccessor {

	@Query("SELECT * from zyber.term_store_term WHERE tenant_id = :tenantId AND term_store_id = :term_store_id")
	Result<TermStoreTerm> getTerms(@Param("tenantId") UUID tenantId, @Param("term_store_id") UUID term_store_id);
	
	@Query("DELETE from zyber.term_store_term WHERE tenant_id = :tenantId AND term_store_id= :term_store_id")
	void deleteTermStoreTerms(@Param("tenantId") UUID tenantId, @Param("term_store_id") UUID term_store_id);
}
