package models

import java.util.Date
import services.LoginService
import zyber.server.dao.{Path, ActivityTimeline, User}

case class JActivityTimeline(
    action: String,
    icon: String,
    note: String, 
    activityTimestamp: Date,
    prettyTimestamp: String,
    user: JUser,
    pathName:Option[String]
    )
    
object JActivityTimeline {
  def from(activity: ActivityTimeline, user: User, pathName:Option[String]=None): JActivityTimeline = {
    JActivityTimeline(
        activity.getActivity, 
        activity.getActionIcon, 
        activity.getNote,
        activity.getActivityTimestamp,
        activity.getActivityTimestamp.toString,
        JUser.fromUser(user),
        pathName
        )
  }

  def fromActivity(activity: ActivityTimeline, loginService:LoginService,pathName:Option[String]=None)(implicit u: User): JActivityTimeline = {
    JActivityTimeline.from(
      activity,
      loginService.findById(activity.getUserId),
      pathName
    )
  }
}