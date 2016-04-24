package zyber.server;

import java.security.InvalidParameterException;

public enum Permissions {

	File_Preview(2),
	File_View(3),
	File_ViewHistory(4),
	File_Modify(5),
	File_Delete(7),
	File_Rename(8), //Applies also to folders
	File_Move(9),

	Folder_View(16),// - List the contents of a folder. (+ On Behalf Of)
	Folder_Add(17),// - Add a new folder or file. May be relevant for creating drop boxes or queues. (Future?)
	Folder_Remove(18),// - Remove a file or folder. (+ On Behalf Of)
	Folder_Move(19),
	Folder_Allow_Access(20),
	Folder_Remove_Access(21),
	
	// Applies to folders and files:
	View_Permissions(25),
	Restore_files_folders(26),
	View_activity(27),
	;
	public final int bit;
	Permissions(int bit) {
		this.bit = bit;
	}
	
	public int value(){
		return 1 << bit;
	}
	
	public static Permissions fromValue(int value){
		for(Permissions p : Permissions.values()){
			if(p.bit == value)
				return p;
		}
		throw new InvalidParameterException();
	}
	
}
