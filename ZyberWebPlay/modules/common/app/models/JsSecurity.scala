package models

import java.util.Date
import zyber.server.dao.Principal
import zyber.server.Permission
import zyber.server.Permissions
import play.api.i18n.Messages
import java.util.UUID
import zyber.server.dao.UserRole

case class JPermissions(
  name: String,
  //  value: Int,
  uuid: UUID,
  selected: Boolean = false,
  label: String = "",
  description: Seq[String] = Nil)

case class JPrincipal(
  principalId: String,
  princType: String,
  createdDate: Date,
  name: String,
  permissions: Option[JPermissions] = None)

object JPrincipal {
  def fromPrincipal(p: Principal)(implicit m: Messages) = {
    JPrincipal(
      p.getPrincipalId.toString(),
      Messages(p.getType.name()),
      p.getCreatedDate,
      p.getDisplayName)
  }
  def withPermissions(p: Principal, permissions: Option[JPermissions])(implicit m: Messages) = {
    JPrincipal(
      p.getPrincipalId.toString(),
      Messages(p.getType.name()),
      p.getCreatedDate,
      p.getDisplayName,
      permissions)
  }
}

object PermissionSets {

  def permissionValue(p: Permissions*): Int = {
    Permission.permisionFor(p: _*).permission
  }

  val viewer = Seq(
    Permissions.File_View,
    Permissions.Folder_View)

  val viewerValue = permissionValue(viewer: _*)

  val editor = Seq(
    Permissions.File_View,
    Permissions.Folder_View,
    Permissions.File_Rename,
    Permissions.Folder_Add,
    Permissions.File_Delete,
    Permissions.View_Permissions)

  val editorValue = permissionValue(editor: _*)

  val employee = Seq(
    Permissions.File_View,
    Permissions.Folder_View,
    Permissions.File_Rename,
    Permissions.Folder_Add,
    Permissions.File_Delete,
    Permissions.Folder_Remove,
    Permissions.View_Permissions,
    Permissions.Folder_Allow_Access,
    Permissions.Folder_Remove_Access)

  val employeeValue = permissionValue(employee: _*)

  val owner = Seq(Permissions.values(): _*)

  val ownerValue = permissionValue(owner :_*)

}

case class JsUserRole(
  name: String,
  uuid: String)

object JsUserRole {
  def fromUserRole(ur: UserRole)(implicit messages: Messages): JsUserRole = {
    JsUserRole(
      Messages(ur.getName),
      ur.getRoleId.toString())
  }
}