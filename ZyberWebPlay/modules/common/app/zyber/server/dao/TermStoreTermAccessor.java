package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.TermStoreTermAccessor}
*/

import java.util.UUID;
import zyber.server.dao.TermStoreTerm;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class TermStoreTermAccessor {

    public final zyber.server.dao.rawaccessors.TermStoreTermAccessor _accessor;
    public final UUID _tenant;

    public TermStoreTermAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.TermStoreTermAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreTermAccessor} */
    public Result<TermStoreTerm> getTerms(UUID term_store_id) { 
        return _accessor.getTerms(_tenant, term_store_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreTermAccessor} */
    public void deleteTermStoreTerms(UUID term_store_id) { 
        _accessor.deleteTermStoreTerms(_tenant, term_store_id);
    }


}
