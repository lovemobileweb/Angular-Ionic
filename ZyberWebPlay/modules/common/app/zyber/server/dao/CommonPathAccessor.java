package zyber.server.dao;


/** Use this accessor, but do not modify it, it was genereated by zyber.GenerateTenantedAccesssors$GenerateTenantedAccesssorsInterface.
*
* This accessor filters for the tenant.
*
* Modify the accessor:  {@link class:zyber.server.dao.rawaccessors.CommonPathAccessor}
*/

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Param;
import java.util.List;
import java.util.UUID;
import zyber.server.dao.Path;
public interface CommonPathAccessor {

    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.CommonPathAccessor} */
    public Result<Path> getChildren(UUID parent_path_id); 
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.CommonPathAccessor} */
    public Result<Path> getChildNamed(UUID parent_path_id, String childName); 
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.CommonPathAccessor} */
    public Path getPath(UUID path_id); 
    /** Originally declared by {@link class:zyber.server.dao.rawaccessors.CommonPathAccessor} */
    public Result<Path> getChildrenIn(List<UUID> parent_path_ids); 

}
