package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.ApplicationSettingsAccessor}
*/

import java.util.UUID;
import zyber.server.dao.ApplicationSetting;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;
import com.datastax.driver.mapping.MappingManager;

@SuppressWarnings("all")
public class ApplicationSettingsAccessor {

    public final zyber.server.dao.rawaccessors.ApplicationSettingsAccessor _accessor;
    public final UUID _tenant;

    public ApplicationSettingsAccessor(MappingManager manager, UUID _tenant) {
        this._accessor = manager.createAccessor(zyber.server.dao.rawaccessors.ApplicationSettingsAccessor.class);
        this._tenant = _tenant;
    }
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ApplicationSettingsAccessor} */
    public ApplicationSetting getGlobalSetting(String key) { 
        return _accessor.getGlobalSetting(_tenant, key);
    }

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.ApplicationSettingsAccessor} */
    public ApplicationSetting getTenantSetting(String key) { 
        return _accessor.getTenantSetting(_tenant, key);
    }


}
