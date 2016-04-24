package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.FileDataAccessor}
*/

import java.util.UUID;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class FileDataAccessor {

    public final zyber.server.dao.rawaccessors.FileDataAccessor _accessor;
    public final UUID _tenant;

    public FileDataAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.FileDataAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.FileDataAccessor} */
    public ResultSet getBlocks(UUID path_id, long version, long startBlock, long endBlock) { 
        return _accessor.getBlocks(_tenant, path_id, version, startBlock, endBlock);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.FileDataAccessor} */
    public void deleteFileData(UUID path_id, long version) { 
        _accessor.deleteFileData(_tenant, path_id, version);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.FileDataAccessor} */
    public void deleteAll() { 
        _accessor.deleteAll();
    }


}
