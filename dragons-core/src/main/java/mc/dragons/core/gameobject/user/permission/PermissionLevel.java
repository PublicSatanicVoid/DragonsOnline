package mc.dragons.core.gameobject.user.permission;

/**
 * Levels of access to sensitive system and administrative functionality.
 * Permission levels are tied to system profiles, which staff members must
 * log into upon joining in order to verify their identity before accessing
 * permissions.
 * 
 * @author Adam
 *
 */
public enum PermissionLevel {
	USER,
	TESTER,
	BUILDER,
	HELPER,
	MODERATOR,
	GM,
	DEVELOPER,
	ADMIN,
	SYSOP;
}
