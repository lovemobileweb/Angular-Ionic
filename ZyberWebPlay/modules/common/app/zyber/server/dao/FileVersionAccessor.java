package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.FileVersionAccessor}
*/

import java.util.Date;
import java.util.UUID;
import zyber.server.dao.FileVersion;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class FileVersionAccessor {

    public final zyber.server.dao.rawaccessors.FileVersionAccessor _accessor;
    public final UUID _tenant;

    public FileVersionAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.FileVersionAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.FileVersionAccessor} */
    public FileVersion getVersion(UUID pathId, Date versionTimestamp) { 
        return _accessor.getVersion(_tenant, pathId, versionTimestamp);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.FileVersionAccessor} */
    public Result<FileVersion> getAllVersions(UUID pathId) { 
        return _accessor.getAllVersions(_tenant, pathId);
    }


}
