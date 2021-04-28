package mc.dragons.core.storage.loader;

import java.util.HashMap;
import java.util.Map;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

/**
 * A registry of all lightweight loaders, enabling them
 * to be fetched from all Dragons components.
 * 
 * @author Adam
 *
 */
public class LightweightLoaderRegistry {
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	@SuppressWarnings("rawtypes")
	private Map<Class<? extends AbstractLightweightLoader>, AbstractLightweightLoader<?>> loaders = new HashMap<>();

	public void register(AbstractLightweightLoader<?> loader) {
		LOGGER.debug("Registering lightweight loader " + loader.getClass().getName());
		loaders.put(loader.getClass(), loader);
	}

	@SuppressWarnings("unchecked")
	public <C extends AbstractLightweightLoader<?>> C getLoader(Class<C> clazz) {
		return (C) loaders.get(clazz);
	}
}
