package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.ResumableTransferAccessor}
*/

import java.util.UUID;
import zyber.server.dao.ResumableTransfer;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class ResumableTransferAccessor {

    public final zyber.server.dao.rawaccessors.ResumableTransferAccessor _accessor;
    public final UUID _tenant;

    public ResumableTransferAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.ResumableTransferAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ResumableTransferAccessor} */
    public ResumableTransfer getResumable(UUID resumableTransferId) { 
        return _accessor.getResumable(_tenant, resumableTransferId);
    }


}
