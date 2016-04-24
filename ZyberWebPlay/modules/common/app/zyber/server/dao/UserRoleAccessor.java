package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.UserRoleAccessor}
*/

import java.util.UUID;
import zyber.server.dao.UserRole;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class UserRoleAccessor {

    public final zyber.server.dao.rawaccessors.UserRoleAccessor _accessor;
    public final UUID _tenant;

    public UserRoleAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.UserRoleAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserRoleAccessor} */
    public UserRole getUserRole(UUID roleId) { 
        return _accessor.getUserRole(_tenant, roleId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserRoleAccessor} */
    public Result<UserRole> getUserRoles() { 
        return _accessor.getUserRoles();
    }


}
