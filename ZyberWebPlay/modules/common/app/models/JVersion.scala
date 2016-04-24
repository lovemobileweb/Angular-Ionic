package models

import zyber.server.dao.{ActivityTimeline, FileVersion}

case class JVersion (number:Int,
                    pathId:String,
                    name:String,
                    version:Long,
                    modifiedPretty:String,
                    size:String,
                    action:String,
                    actionIcon:String,
                    actionUser:String
                    ) {

}

object JVersion {
  def from(version:FileVersion, activity:ActivityTimeline,name:String, number:Int):JVersion = {
    JVersion(number,version.getPathId.toString,name, version.getVersion.getTime, version.getVersion.toString, version.getSize.toString, activity.getActivity, activity.getActionIcon, activity.getUsername)
  }
}
