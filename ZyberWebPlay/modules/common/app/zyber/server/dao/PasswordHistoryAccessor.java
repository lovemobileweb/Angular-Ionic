package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PasswordHistoryAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import zyber.server.dao.PasswordHistory;
import zyber.server.dao.UserKeys;
import java.util.UUID;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PasswordHistoryAccessor {

    public final zyber.server.dao.rawaccessors.PasswordHistoryAccessor _accessor;
    public final UUID _tenant;

    public PasswordHistoryAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PasswordHistoryAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PasswordHistoryAccessor} */
    public Result<PasswordHistory> getPasswordHistoryForUser(UUID userId) { 
        return _accessor.getPasswordHistoryForUser(_tenant, userId);
    }


}
