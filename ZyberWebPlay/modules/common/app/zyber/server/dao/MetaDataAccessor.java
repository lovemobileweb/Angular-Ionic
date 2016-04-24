package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.MetaDataAccessor}
*/

import java.util.UUID;
import zyber.server.dao.MetaData;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class MetaDataAccessor {

    public final zyber.server.dao.rawaccessors.MetaDataAccessor _accessor;
    public final UUID _tenant;

    public MetaDataAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.MetaDataAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.MetaDataAccessor} */
    public MetaData getValueByPathID(String key, UUID path_id) { 
        return _accessor.getValueByPathID(_tenant, key, path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.MetaDataAccessor} */
    public Result<MetaData> getPathMetadata(UUID path_id) { 
        return _accessor.getPathMetadata(_tenant, path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.MetaDataAccessor} */
    public void deletePathMetadata(UUID path_id) { 
        _accessor.deletePathMetadata(_tenant, path_id);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.MetaDataAccessor} */
    public void deletePathMetadataByKey(UUID path_id, String key) { 
        _accessor.deletePathMetadataByKey(_tenant, path_id, key);
    }


}
