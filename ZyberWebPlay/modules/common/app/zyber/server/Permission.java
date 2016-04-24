package zyber.server;

public class Permission {
	public final int permission;
	public Permission(int permission) {
		this.permission = permission;
	}
	
	public boolean can(Permissions canDo) {
		int mask = 1 << canDo.bit;
		return (permission & mask) == mask;
	}
	
	public boolean can_ViewPermissions() { return can(Permissions.View_Permissions); }
	public boolean can_Restore() { return can(Permissions.Restore_files_folders); }
	public boolean can_ViewActivity() { return can(Permissions.Restore_files_folders); }

	public boolean canFile_Preview() { return can(Permissions.File_Preview); }
	public boolean canFile_View() { return can(Permissions.File_View); }
	public boolean canFile_ViewHistory() { return can(Permissions.File_ViewHistory); }
	public boolean canFile_Modify() { return can(Permissions.File_Modify); }
	public boolean canFile_Delete() { return can(Permissions.File_Delete); }
	public boolean canFile_Rename() { return can(Permissions.File_Rename); }
	public boolean canFile_Move() { return can(Permissions.File_Move); }

	public boolean canFolder_View() { return can(Permissions.Folder_View); }
	public boolean canFolder_Add() { return can(Permissions.Folder_Add); }
	public boolean canFolder_Remove() { return can(Permissions.Folder_Remove); }
	public boolean canFolder_Move() { return can(Permissions.Folder_Move); }
	public boolean canFolder_AllowAccess() { return can(Permissions.Folder_Allow_Access); }
	public boolean canFolder_RemoveAccess() { return can(Permissions.Folder_Remove_Access); }


	public static Permission permisionFor(Permissions... permisions){
		int res = 0;
		for(Permissions p : permisions){
			res = res | p.value();
		}
		return new Permission(res);
	}
	
//	public static final void main(String[] args){
//
//		Permission p = new Permission(0 | Permissions.File_Rename.value());
//		System.out.println(p.canFile_Preview());
//		System.out.println(p.canFile_View());
//		System.out.println(p.canFile_ViewHistory());
//		System.out.println(p.canFile_ViewPermissions());
//		System.out.println(p.canFile_Rename());
//		System.out.println(p.canFile_Modify());
//	}
}
