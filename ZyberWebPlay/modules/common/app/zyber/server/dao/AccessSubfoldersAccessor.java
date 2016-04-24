package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.AccessSubfoldersAccessor}
*/

import java.util.List;
import java.util.UUID;
import zyber.server.dao.AccessSubfolders;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class AccessSubfoldersAccessor {

    public final zyber.server.dao.rawaccessors.AccessSubfoldersAccessor _accessor;
    public final UUID _tenant;

    public AccessSubfoldersAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.AccessSubfoldersAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.AccessSubfoldersAccessor} */
    public ResultSet countAccess(UUID parent_id, List<UUID> principals) { 
        return _accessor.countAccess(_tenant, parent_id, principals);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.AccessSubfoldersAccessor} */
    public Result<AccessSubfolders> getAcess(UUID parent_path_id, UUID path_id) { 
        return _accessor.getAcess(_tenant, parent_path_id, path_id);
    }


}
