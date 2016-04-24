package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor}
*/

import java.util.UUID;
import zyber.server.dao.GroupMember;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class GroupMembersAccessor {

    public final zyber.server.dao.rawaccessors.GroupMembersAccessor _accessor;
    public final UUID _tenant;

    public GroupMembersAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.GroupMembersAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor} */
    public Result<GroupMember> getGroupMembers(UUID groupId) { 
        return _accessor.getGroupMembers(_tenant, groupId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor} */
    public GroupMember getGroupMember(UUID groupId, UUID member_principal_id) { 
        return _accessor.getGroupMember(_tenant, groupId, member_principal_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor} */
    public void deleteGroupMembers(UUID group_id) { 
        _accessor.deleteGroupMembers(_tenant, group_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor} */
    public Result<GroupMember> getGroupMembersByPrincipal(UUID memberPrincipalId) { 
        return _accessor.getGroupMembersByPrincipal(_tenant, memberPrincipalId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.GroupMembersAccessor} */
    public ResultSet countMembers(UUID groupId) { 
        return _accessor.countMembers(_tenant, groupId);
    }


}
