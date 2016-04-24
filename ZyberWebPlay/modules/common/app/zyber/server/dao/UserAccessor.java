package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.UserAccessor}
*/

import java.util.UUID;
import zyber.server.dao.User;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class UserAccessor {

    public final zyber.server.dao.rawaccessors.UserAccessor _accessor;
    public final UUID _tenant;

    public UserAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.UserAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public User getUserByEmail(String email) { 
        return _accessor.getUserByEmail(_tenant, email);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public User getOnePosition(UUID userId) { 
        return _accessor.getOnePosition(_tenant, userId);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public User getById(UUID user_id) { 
        return _accessor.getById(_tenant, user_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public void updateUser(UUID id, String name, String email) { 
        _accessor.updateUser(_tenant, id, name, email);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public void deleteUser(UUID id) { 
        _accessor.deleteUser(_tenant, id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public Result<User> getAll() { 
        return _accessor.getAll(_tenant);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.UserAccessor} */
    public Result<User> getActiveUsers() { 
        return _accessor.getActiveUsers(_tenant);
    }


}
