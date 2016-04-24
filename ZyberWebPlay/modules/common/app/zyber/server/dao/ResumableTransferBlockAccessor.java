package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.ResumableTransferBlockAccessor}
*/

import java.util.UUID;
import zyber.server.dao.ResumableTransferBlocks;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class ResumableTransferBlockAccessor {

    public final zyber.server.dao.rawaccessors.ResumableTransferBlockAccessor _accessor;
    public final UUID _tenant;

    public ResumableTransferBlockAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.ResumableTransferBlockAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ResumableTransferBlockAccessor} */
    public ResumableTransferBlocks getBlock(UUID resumableTransferId, long chunkNumber) { 
        return _accessor.getBlock(_tenant, resumableTransferId, chunkNumber);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ResumableTransferBlockAccessor} */
    public ResumableTransferBlocks deleteBlocks(UUID resumableTransferId) { 
        return _accessor.deleteBlocks(_tenant, resumableTransferId);
    }


}
