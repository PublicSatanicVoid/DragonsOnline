package mc.dragons.core.addon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import mc.dragons.core.Dragons;

public class AddonRegistry {
	private Logger LOGGER;

	private List<Addon> addons;

	public AddonRegistry(Dragons plugin) {
		addons = new ArrayList<>();
		LOGGER = plugin.getLogger();
	}

	public void register(Addon addon) {
		LOGGER.info("Registering addon " + addon.getName() + " of type " + addon.getType());
		addons.add(addon);
	}

	public void enableAll() {
		addons.forEach(addon -> addon.onEnable());
	}

	public Addon getAddonByName(String name) {
		for (Addon addon : addons) {
			if (addon.getName().equalsIgnoreCase(name)) {
				return addon;
			}
		}
		return null;
	}

	public List<Addon> getAllAddons() {
		return addons;
	}
}
