package mc.dragons.anticheat.check;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.core.gameobject.user.User;

public abstract class Check {
	protected DragonsAntiCheat plugin;
	protected boolean enabled;
	
	protected Check(DragonsAntiCheat plugin) {
		this.plugin = plugin;
		this.enabled = true;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		plugin.getLogger().info("Check " + getName() + " " + (enabled ? "enabled" : "disabled"));
	}
	
	public abstract CheckType getType();
	public abstract String getName();
	public abstract void setup();
	
	/**
	 * 
	 * @param user The user to check
	 * 
	 * @return Whether the check was passed or not.
	 */
	public abstract boolean check(User user);
}
