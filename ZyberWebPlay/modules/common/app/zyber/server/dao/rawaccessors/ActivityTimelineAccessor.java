package zyber.server.dao.rawaccessors;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

import java.util.List;
import java.util.UUID;

import zyber.server.dao.ActivityTimeline;

@Accessor
public interface ActivityTimelineAccessor {
  @Query("SELECT * FROM zyber.activity_timeline WHERE  tenant_id = :tenantId AND path_id = :pathId allow filtering")
  Result<ActivityTimeline> getActivityByFile(@Param("tenantId") UUID tenantId, @Param("pathId") UUID pathId);

  @Query("SELECT * FROM zyber.activity_timeline WHERE tenant_id = :tenantId AND path_id in :pathId allow filtering")
  Result<ActivityTimeline> getActivityByFileIn(@Param("tenantId") UUID tenantId, @Param("pathId") List<UUID> pathId);

  @Query("SELECT * FROM zyber.activity_timeline WHERE tenant_id = :tenantId AND user_id = :userId allow filtering")
  Result<ActivityTimeline> getActivityByUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
  
//  @Query("SELECT * FROM activity_timeline WHERE tenant_id = :tenantId AND path_id = :pathId and activity_timestamp = :timestamp")
//  ActivityTimeline getActivityByFile(@Param("pathId") UUID pathId, @Param("activity_timestamp") Date timestamp);
}
