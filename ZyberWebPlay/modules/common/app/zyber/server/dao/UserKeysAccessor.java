package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.UserKeysAccessor}
*/

import java.util.UUID;
import zyber.server.dao.UserKeys;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class UserKeysAccessor {

    public final zyber.server.dao.rawaccessors.UserKeysAccessor _accessor;
    public final UUID _tenant;

    public UserKeysAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.UserKeysAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserKeysAccessor} */
    public UserKeys getUserKeysByUserId(UUID userId) { 
        return _accessor.getUserKeysByUserId(_tenant, userId);
    }


}
