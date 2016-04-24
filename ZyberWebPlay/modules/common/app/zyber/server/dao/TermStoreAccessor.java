package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.TermStoreAccessor}
*/

import java.util.UUID;
import zyber.server.dao.TermStore;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class TermStoreAccessor {

    public final zyber.server.dao.rawaccessors.TermStoreAccessor _accessor;
    public final UUID _tenant;

    public TermStoreAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.TermStoreAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreAccessor} */
    public Result<TermStore> getTermStore() { 
        return _accessor.getTermStore(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreAccessor} */
    public TermStore getTermStoreById(UUID term_store_id) { 
        return _accessor.getTermStoreById(_tenant, term_store_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreAccessor} */
    public TermStore getTermStoreByName(String name) { 
        return _accessor.getTermStoreByName(_tenant, name);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TermStoreAccessor} */
    public void deleteTermStoreTerms(UUID term_store_id) { 
        _accessor.deleteTermStoreTerms(_tenant, term_store_id);
    }


}
