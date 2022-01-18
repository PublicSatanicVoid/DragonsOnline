package mc.dragons.anticheat.check;

import mc.dragons.core.gameobject.user.User;

public interface Check {
	public CheckType getType();
	public String getName();
	public void setup();
	
	/**
	 * 
	 * @param user The user to check
	 * 
	 * @return Whether the check was passed or not.
	 */
	public boolean check(User user);
}
