package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PrincipalAccessor}
*/

import java.util.List;
import java.util.UUID;
import zyber.server.dao.Principal;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PrincipalAccessor {

    public final zyber.server.dao.rawaccessors.PrincipalAccessor _accessor;
    public final UUID _tenant;

    public PrincipalAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PrincipalAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PrincipalAccessor} */
    public Result<Principal> getPrincipals() { 
        return _accessor.getPrincipals(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PrincipalAccessor} */
    public Principal getPrincipaById(UUID principal_id) { 
        return _accessor.getPrincipaById(_tenant, principal_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PrincipalAccessor} */
    public Result<Principal> getPrincipaById(List<UUID> principal_id) { 
        return _accessor.getPrincipaById(_tenant, principal_id);
    }


}
