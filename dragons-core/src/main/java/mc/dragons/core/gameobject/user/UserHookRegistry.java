package mc.dragons.core.gameobject.user;

import java.util.ArrayList;
import java.util.List;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

/**
 * Central registry of user hooks.
 * 
 * @author Adam
 *
 */
public class UserHookRegistry {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	private List<UserHook> hooks = new ArrayList<>();

	public void registerHook(UserHook hook) {
		LOGGER.debug("Registered user hook " + hook.getClass().getName());
		hooks.add(hook);
	}

	public List<UserHook> getHooks() {
		return hooks;
	}
}
