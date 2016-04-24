package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.GroupAccessor}
*/

import java.util.UUID;
import zyber.server.dao.Group;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class GroupAccessor {

    public final zyber.server.dao.rawaccessors.GroupAccessor _accessor;
    public final UUID _tenant;

    public GroupAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.GroupAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupAccessor} */
    public Result<Group> getGroups() { 
        return _accessor.getGroups(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupAccessor} */
    public Group getGroupByName(String name) { 
        return _accessor.getGroupByName(_tenant, name);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupAccessor} */
    public Group getGroupById(UUID group_id) { 
        return _accessor.getGroupById(_tenant, group_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupAccessor} */
    public void deleteGroup(UUID group_id) { 
        _accessor.deleteGroup(_tenant, group_id);
    }


}
