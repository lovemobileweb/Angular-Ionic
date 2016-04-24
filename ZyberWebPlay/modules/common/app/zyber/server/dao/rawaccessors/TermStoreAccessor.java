package zyber.server.dao.rawaccessors;

import java.util.UUID;

import zyber.server.dao.TermStore;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface TermStoreAccessor {

	@Query("SELECT * from zyber.term_store WHERE tenant_id = :tenantId")
	Result<TermStore> getTermStore(@Param("tenantId") UUID tenantId);
	
	@Query("SELECT * from zyber.term_store WHERE tenant_id = :tenantId AND term_store_id = :term_store_id")
	TermStore getTermStoreById(@Param("tenantId") UUID tenantId, @Param("term_store_id") UUID term_store_id);
	
	@Query("SELECT * from zyber.term_store WHERE tenant_id = :tenantId AND name= :name")
	TermStore getTermStoreByName(@Param("tenantId") UUID tenantId, @Param("name") String name);
	
	@Query("DELETE from zyber.term_store WHERE tenant_id = :tenantId AND term_store_id= :term_store_id")
	void deleteTermStoreTerms(@Param("tenantId") UUID tenantId, @Param("term_store_id") UUID term_store_id);
}
