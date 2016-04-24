package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import java.util.List;
import java.util.UUID;
import zyber.server.dao.Path;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PathIncludingDeletedAccessor implements CommonPathAccessor {

    public final zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor _accessor;
    public final UUID _tenant;

    public PathIncludingDeletedAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor} */
    public Result<Path> getChildrenIn(List<UUID> parent_path_ids) { 
        return _accessor.getChildrenIn(_tenant, parent_path_ids);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor} */
    public Result<Path> getChildren(UUID parent_path_id) { 
        return _accessor.getChildren(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor} */
    public Result<Path> getChildNamed(UUID parent_path_id, String childName) { 
        return _accessor.getChildNamed(_tenant, parent_path_id, childName);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathIncludingDeletedAccessor} */
    public Path getPath(UUID path_id) { 
        return _accessor.getPath(_tenant, path_id);
    }


}
