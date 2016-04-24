package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.SecurityTypeAccessor}
*/

import java.util.UUID;
import zyber.server.dao.SecurityType;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class SecurityTypeAccessor {

    public final zyber.server.dao.rawaccessors.SecurityTypeAccessor _accessor;
    public final UUID _tenant;

    public SecurityTypeAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.SecurityTypeAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.SecurityTypeAccessor} */
    public SecurityType getSecurityType(UUID securityId) { 
        return _accessor.getSecurityType(_tenant, securityId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.SecurityTypeAccessor} */
    public Result<SecurityType> getSecurityTypes() { 
        return _accessor.getSecurityTypes();
    }


}
