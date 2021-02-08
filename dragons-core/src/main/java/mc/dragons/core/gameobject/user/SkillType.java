package mc.dragons.core.gameobject.user;

/**
 * Various skills that the player can advance to unlock additional features.
 * 
 * @author Adam
 *
 */
public enum SkillType {
	MELEE("Melee"), 
	ARCHERY("Archery"), 
	MINING("Mining"), 
	FISHING("Fishing"), 
	COOKING("Cooking"), 
	DUAL_WIELD("Dual Wield"), 
	RIDING("Riding"), 
	BLACKSMITHING("Blacksmithing"), 
	DEFENSE("Defense");

	private String friendlyName;

	SkillType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return this.friendlyName;
	}
}
