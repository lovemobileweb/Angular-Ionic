package zyber.server;

import java.util.UUID;

import zyber.server.dao.UserRole;

public class Abilities {

	public enum Ability {
		Access_files_folders, 
		Create_users, 
		Create_admin, 
		Create_groups, 
		Reset_passwords, 
		View_activy, 
		Manage_termstore, 
		Manage_abilities;
		public int value() {
			return 1 << ordinal();
		}
	}

	public final int abilities;

	public Abilities(int abilities) {
		this.abilities = abilities;
	}

	public boolean can(Ability h) {
		int mask = 1 << h.ordinal();
		return (abilities & mask) == mask;
	}

	public boolean canAccessFilesFolders() {
		return can(Ability.Access_files_folders);
	}

	public boolean canCreateUsers() {
		return can(Ability.Create_users);
	}

	public boolean canCreateAdmin() {
		return can(Ability.Create_admin);
	}

	public boolean canCreateGroups() {
		return can(Ability.Create_groups);
	}

	public boolean canResetPasswords() {
		return can(Ability.Reset_passwords);
	}

	public boolean canViewActivy() {
		return can(Ability.View_activy);
	}

	public boolean canManageTermstore() {
		return can(Ability.Manage_termstore);
	}

	public boolean canManageAbilities() {
		return can(Ability.Manage_abilities);
	}
	
	public boolean isPowerUser(){
		return abilities == DefaultUserRoles.powerUser.getAbilities();
	}
	
	public boolean isUseer(){
		return abilities == DefaultUserRoles.user.getAbilities();
	}
	public static Abilities abilitiesFor(Ability... hab) {
		int res = 0;
		for (Ability h : hab) {
			res = res | h.value();
		}
		return new Abilities(res);
	}

	public static class DefaultUserRoles {

		public static final String USER_NAME = "user";
		public static final String POWSER_USER_NAME = "power_user";
//		public static final String ADMINISTRATOR_NAME = "administrator";

		private static int abilitiesValue(Ability... hab) {
			return abilitiesFor(hab).abilities;
		}

		public static UserRole user = new UserRole(USER_NAME,
				new UUID(0, 0), abilitiesValue(Ability.Access_files_folders));

		public static UserRole powerUser = new UserRole(POWSER_USER_NAME,
				new UUID(1, 1), abilitiesValue(Ability.Access_files_folders,
						Ability.Create_users, Ability.Reset_passwords,
						Ability.Manage_termstore));

//		public static UserRole administrator = new UserRole(ADMINISTRATOR_NAME,
//				UUID.randomUUID(), 
//				abilitiesValue(Ability.Access_files_folders,
//						Ability.Create_users,
//						Ability.Create_admin,
//						Ability.Create_groups));
	}
}
