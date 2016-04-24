package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor}
*/

import java.util.UUID;
import zyber.server.dao.GroupMemberFlat;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class GroupMembersFlatAccessor {

    public final zyber.server.dao.rawaccessors.GroupMembersFlatAccessor _accessor;
    public final UUID _tenant;

    public GroupMembersFlatAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.GroupMembersFlatAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public Result<GroupMemberFlat> getGroupMembers(UUID groupId) { 
        return _accessor.getGroupMembers(_tenant, groupId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public GroupMemberFlat getGroupMember(UUID groupId, UUID member_principal_id) { 
        return _accessor.getGroupMember(_tenant, groupId, member_principal_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public void deleteGroupMember(UUID group_id, UUID member_principal_id) { 
        _accessor.deleteGroupMember(_tenant, group_id, member_principal_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public void deleteGroupMembers(UUID group_id) { 
        _accessor.deleteGroupMembers(_tenant, group_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public Result<GroupMemberFlat> getUserGroups(UUID userId) { 
        return _accessor.getUserGroups(_tenant, userId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersFlatAccessor} */
    public Result<GroupMemberFlat> getGroupsBySource(UUID sourceGroupId, UUID memberPrincipalId) { 
        return _accessor.getGroupsBySource(_tenant, sourceGroupId, memberPrincipalId);
    }


}
