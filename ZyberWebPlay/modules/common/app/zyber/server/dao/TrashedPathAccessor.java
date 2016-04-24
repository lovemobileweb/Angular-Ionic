package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.TrashedPathAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import java.util.UUID;
import zyber.server.dao.TrashedPath;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class TrashedPathAccessor {

    public final zyber.server.dao.rawaccessors.TrashedPathAccessor _accessor;
    public final UUID _tenant;

    public TrashedPathAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.TrashedPathAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TrashedPathAccessor} */
    public Result<TrashedPath> getTrashedPathsForUser(UUID user_id) { 
        return _accessor.getTrashedPathsForUser(_tenant, user_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TrashedPathAccessor} */
    public TrashedPath getTrashedPath(UUID user_id, UUID path_id) { 
        return _accessor.getTrashedPath(_tenant, user_id, path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.TrashedPathAccessor} */
    public void deleteTrashedPath(UUID user_id, UUID path_id) { 
        _accessor.deleteTrashedPath(_tenant, user_id, path_id);
    }


}
