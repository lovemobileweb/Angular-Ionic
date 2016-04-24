package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PathShareAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import java.util.UUID;
import zyber.server.dao.PathShare;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PathShareAccessor {

    public final zyber.server.dao.rawaccessors.PathShareAccessor _accessor;
    public final UUID _tenant;

    public PathShareAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PathShareAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathShareAccessor} */
    public Result<PathShare> getShareForId(UUID share_id) { 
        return _accessor.getShareForId(_tenant, share_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathShareAccessor} */
    public Result<PathShare> listAllShares() { 
        return _accessor.listAllShares(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathShareAccessor} */
    public void deleteForPath(UUID share_id) { 
        _accessor.deleteForPath(_tenant, share_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathShareAccessor} */
    public void wipeShares() { 
        _accessor.wipeShares();
    }


}
