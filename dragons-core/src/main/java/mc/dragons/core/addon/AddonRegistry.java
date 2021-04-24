package mc.dragons.core.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import mc.dragons.core.Dragons;

/**
 * Central registry of game object add-ons.
 * 
 * @author Adam
 *
 */
public class AddonRegistry {
	private Logger LOGGER;

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
		LOGGER.info("Registering addon " + addon.getName() + " of type " + addon.getType());
		addons.add(addon);
	}

	/**
	 * Enable all add-ons
	 */
	public void enableAll() {
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
