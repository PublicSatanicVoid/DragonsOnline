package mc.dragons.core.gameobject.user;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of user hooks.
 * 
 * @author Adam
 *
 */
public class UserHookRegistry {
	private List<UserHook> hooks = new ArrayList<>();

	public void registerHook(UserHook hook) {
		this.hooks.add(hook);
	}

	public List<UserHook> getHooks() {
		return this.hooks;
	}
}
