package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PrincipalRoleAccessor}
*/

import java.util.UUID;
import zyber.server.dao.Principal;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PrincipalRoleAccessor {

    public final zyber.server.dao.rawaccessors.PrincipalRoleAccessor _accessor;
    public final UUID _tenant;

    public PrincipalRoleAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PrincipalRoleAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PrincipalRoleAccessor} */
    public Result<Principal> getPrincipalRoles() { 
        return _accessor.getPrincipalRoles(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PrincipalRoleAccessor} */
    public Principal getPrincipalRoles(UUID principal_id) { 
        return _accessor.getPrincipalRoles(_tenant, principal_id);
    }


}
