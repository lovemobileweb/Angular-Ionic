package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PathSecurityAccessor}
*/

import java.util.UUID;
import zyber.server.dao.PathSecurity;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PathSecurityAccessor {

    public final zyber.server.dao.rawaccessors.PathSecurityAccessor _accessor;
    public final UUID _tenant;

    public PathSecurityAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PathSecurityAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathSecurityAccessor} */
    public Result<PathSecurity> getSecurityForPath(UUID parentPathId, UUID pathId) { 
        return _accessor.getSecurityForPath(_tenant, parentPathId, pathId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathSecurityAccessor} */
    public PathSecurity getSecurityForPrincipal(UUID parentPathId, UUID pathId, UUID princId) { 
        return _accessor.getSecurityForPrincipal(_tenant, parentPathId, pathId, princId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathSecurityAccessor} */
    public Result<PathSecurity> getSecurityForParentPath(UUID parentPathId) { 
        return _accessor.getSecurityForParentPath(_tenant, parentPathId);
    }


}
