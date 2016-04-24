package models.extra

import models.{JActivityTimeline, JPath}

case class JAdminActivity(paths:List[JPath],activities:List[JFileActivity])
case class JAdminActivityByTime(paths:List[JPath],activities:List[JActivityTimeline])
case class JFileActivity(path:JPath, activities:List[JActivityTimeline])
