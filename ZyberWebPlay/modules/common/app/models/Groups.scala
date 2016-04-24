package models

import zyber.server.dao.User
import zyber.server.dao.Group
import java.util.UUID
import java.util.Date
import zyber.server.dao.GroupMember

case class JGroup(
  uuid: String,
//  owner: JUser,
  name: String,
  members: Int,
  createdDate: Date)

object JGroup {
  def from(group: Group): JGroup = {
    JGroup(
      group.getGroupId.toString,
//      JUser.fromUser(group.getOwner),
      group.getName,
      group.getMembers,
      group.getCreatedDate)
  }
}

case class JGroupMember(
  groupId: String,
  memberId: String,
//  memberType: String,
  joinedDate: Date,
  name: String)

object JGroupMember {
  def from(gm: GroupMember): JGroupMember =
    JGroupMember(
      gm.getGroupId.toString(),
      gm.getMemberPrincipalId.toString(),
//      gm.getMemberPrincipalType.toString(),
      gm.getJoinedDate,
      gm.getName)

}
    