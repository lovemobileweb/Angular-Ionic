package models

import java.text.SimpleDateFormat
import java.util.Date

import scala.util.Try

import models.extra.FolderTree
import models.extra.JAdminActivity
import models.extra.JAdminActivityByTime
import models.extra.JFileActivity
import models.extra.SharingSubmission
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes
import zyber.server.dao.TermStoreTerm

trait JsonFormats {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm ss")

  implicit val wrs: Writes[Date] = new Writes[Date] {
    def writes(t: Date) = {
      JsString(dateFormatter.format(t.getTime))
    }
  }

  implicit val shareForm = Json.format[JShare]
  implicit val pathFormat = Json.format[JPath]
  implicit val versionFormat = Json.writes[JVersion]
  implicit val userWrites = Json.format[JUser]
  implicit val activityWrites = Json.format[JActivityTimeline]

  implicit val shareSubmitFormat = Json.reads[SharingSubmission]

  implicit val termStoreFormat = Json.format[JTermStore]

  implicit val metadataReads = Json.format[JMetadata]

  implicit val fileActivityFormat = Json.format[JFileActivity]
  implicit val adminActivityFormat = Json.format[JAdminActivity]
  implicit val adminActivityTimeFormat = Json.format[JAdminActivityByTime]

  implicit val groupFormat = Json.format[JGroup]
  implicit val groupMemberFormat = Json.format[JGroupMember]

  implicit val termWrites = new Writes[TermStoreTerm] {
    def writes(tst: TermStoreTerm) = Json.obj(
      "name" -> tst.getName)
  }

  def toJsonObj(metadata: Seq[JMetadata]): JsObject =
    metadata.foldLeft(Json.obj()) { (a, b) => a + (b.key, JsArray(b.value.map { x => JsString(x) })) }

  def metadataFromJson(value: JsValue): Try[Seq[JMetadata]] = Try {
    value match {
      case jsob: JsObject => jsob.fields.map {
        case (a, s) => JMetadata(a, jsArrayToSeq(s))
      }
      case _ => throw new RuntimeException("Invalid metadata Json")
    }
  }

  def jsArrayToSeq(value: JsValue): Seq[String] = {
    value match {
      case JsArray(seq) => seq.map(jstringToString)
      case _            => throw new RuntimeException("Invalid metadata Json")
    }
  }

  def jstringToString(value: JsValue): String = value match {
    case JsString(str) => str
    case _             => throw new RuntimeException("Invalid metadata Json")
  }

  implicit val permissionsFormat = Json.format[JPermissions]

  implicit val jprincipalFormat = Json.format[JPrincipal]
  
  implicit val jsuserRolFormat  = Json.format[JsUserRole]
  
  
  implicit val folderTreeWriter = new Writes[FolderTree] {
    def writes(ft: FolderTree): JsValue = toJsValue(ft)
        
    def toJsValue(ft: FolderTree): JsValue = JsObject(Seq(
        "name" -> JsString(ft.path.getName),
        "uuid" -> JsString(ft.path.getPathId.toString()),
        "children" -> JsArray(ft.folders.map(toJsValue))
        ))
  }

}

object JsonFormats extends JsonFormats