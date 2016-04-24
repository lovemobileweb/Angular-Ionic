package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.ActivityTimelineAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import java.util.List;
import java.util.UUID;
import zyber.server.dao.ActivityTimeline;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class ActivityTimelineAccessor {

    public final zyber.server.dao.rawaccessors.ActivityTimelineAccessor _accessor;
    public final UUID _tenant;

    public ActivityTimelineAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.ActivityTimelineAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ActivityTimelineAccessor} */
    public Result<ActivityTimeline> getActivityByFile(UUID pathId) { 
        return _accessor.getActivityByFile(_tenant, pathId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ActivityTimelineAccessor} */
    public Result<ActivityTimeline> getActivityByFileIn(List<UUID> pathId) { 
        return _accessor.getActivityByFileIn(_tenant, pathId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ActivityTimelineAccessor} */
    public Result<ActivityTimeline> getActivityByUser(UUID userId) { 
        return _accessor.getActivityByUser(_tenant, userId);
    }


}
