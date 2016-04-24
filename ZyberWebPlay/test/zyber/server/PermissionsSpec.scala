package zyber.server

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PermissionsSpec extends Specification {

  "Permissions" should {
    "Validate correct permissions" in {
      var p = new Permission(Permissions.File_Delete.value());
      p.canFile_Delete() must beTrue

      p = new Permission(Permissions.File_Modify.value());
      p.canFile_Modify() must beTrue

      p = new Permission(Permissions.File_Preview.value());
      p.canFile_Preview() must beTrue

      p = new Permission(Permissions.File_Rename.value());
      p.canFile_Rename() must beTrue

      p = new Permission(Permissions.File_View.value());
      p.canFile_View() must beTrue

      p = new Permission(Permissions.File_ViewHistory.value());
      p.canFile_ViewHistory() must beTrue

      p = new Permission(Permissions.View_Permissions.value());
      p.can_ViewPermissions() must beTrue
      p.canFile_Delete() must beFalse
      p.canFile_Rename() must beFalse
      p.canFile_Modify() must beFalse
      p.canFile_Preview() must beFalse
    }

    "Assign multiple permissions" in {
      var p = Permission.permisionFor(Permissions.File_ViewHistory, Permissions.File_Delete, Permissions.File_Rename)
      p.canFile_Delete() must beTrue
      p.canFile_ViewHistory() must beTrue
      p.canFile_Rename() must beTrue
      p.canFile_Modify() must beFalse
      p.canFile_Preview() must beFalse
      p.canFile_View() must beFalse
      p.can_ViewPermissions() must beFalse
      p.canFolder_Add() must beFalse
    }

    "Get permissions from value" in {
      Permissions.fromValue(Permissions.File_Delete.bit) mustEqual Permissions.File_Delete
      Permissions.fromValue(Permissions.Folder_Add.bit) mustEqual Permissions.Folder_Add
      
      Permissions.fromValue(Permissions.Folder_Add.bit) mustNotEqual Permissions.File_Delete
    }
  }
}