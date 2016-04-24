package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.PathAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import java.util.List;
import java.util.UUID;
import zyber.server.dao.Path;
import zyber.server.dao.PathType;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class PathAccessor implements CommonPathAccessor {

    public final zyber.server.dao.rawaccessors.PathAccessor _accessor;
    public final UUID _tenant;

    public PathAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.PathAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren(UUID parent_path_id) { 
        return _accessor.getChildren(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildrenByType(UUID parent_path_id, PathType type) { 
        return _accessor.getChildrenByType(_tenant, parent_path_id, type);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildrenIn(List<UUID> parent_path_ids) { 
        return _accessor.getChildrenIn(_tenant, parent_path_ids);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByName_Asc(UUID parent_path_id) { 
        return _accessor.getChildren_ByName_Asc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByName_Desc(UUID parent_path_id) { 
        return _accessor.getChildren_ByName_Desc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByCreatedDate_Asc(UUID parent_path_id) { 
        return _accessor.getChildren_ByCreatedDate_Asc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByCreatedDate_Desc(UUID parent_path_id) { 
        return _accessor.getChildren_ByCreatedDate_Desc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByModifiedDate_Asc(UUID parent_path_id) { 
        return _accessor.getChildren_ByModifiedDate_Asc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_ByModifiedDate_Desc(UUID parent_path_id) { 
        return _accessor.getChildren_ByModifiedDate_Desc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_BySize_Asc(UUID parent_path_id) { 
        return _accessor.getChildren_BySize_Asc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildren_BySize_Desc(UUID parent_path_id) { 
        return _accessor.getChildren_BySize_Desc(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildrenByCreated(UUID parent_path_id) { 
        return _accessor.getChildrenByCreated(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildrenByModified(UUID parent_path_id) { 
        return _accessor.getChildrenByModified(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildrenBySize(UUID parent_path_id) { 
        return _accessor.getChildrenBySize(_tenant, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getChildNamed(UUID parent_path_id, String childName) { 
        return _accessor.getChildNamed(_tenant, parent_path_id, childName);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Path getPath(UUID path_id) { 
        return _accessor.getPath(path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Path getPathByParent(UUID path_id, UUID parent_path_id) { 
        return _accessor.getPathByParent(_tenant, path_id, parent_path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public Result<Path> getPathsLinked(UUID path_id) { 
        return _accessor.getPathsLinked(_tenant, path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.PathAccessor} */
    public void deletePath(UUID parent_path_id, UUID path_id) { 
        _accessor.deletePath(_tenant, parent_path_id, path_id);
    }


}
