package mc.dragons.core.addon;

import java.util.ArrayList;
import java.util.List;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

/**
 * Central registry of game object add-ons.
 * 
 * @author Adam
 *
 */
public class AddonRegistry {
	private DragonsLogger LOGGER;

	private List<Addon> addons;

	public AddonRegistry(Dragons plugin) {
		addons = new ArrayList<>();
		LOGGER = plugin.getLogger();
	}

	/**
	 * Register the specified add-on.
	 * 
	 * @param addon
	 */
	public void register(Addon addon) {
		LOGGER.debug("Registering addon " + addon.getName() + " of type " + addon.getType());
		addons.add(addon);
	}

	/**
	 * Enable all add-ons
	 */
	public void enableAll() {
		LOGGER.debug("Enabling all addons");
		addons.forEach(addon -> addon.onEnable());
	}

	/**
	 * 
	 * @param name
	 * @return The add-on with the specified name
	 */
	public Addon getAddonByName(String name) {
		for (Addon addon : addons) {
			if (addon.getName().equalsIgnoreCase(name)) {
				return addon;
			}
		}
		return null;
	}

	/**
	 * 
	 * @return All registered add-ons
	 */
	public List<Addon> getAllAddons() {
		return addons;
	}
}
