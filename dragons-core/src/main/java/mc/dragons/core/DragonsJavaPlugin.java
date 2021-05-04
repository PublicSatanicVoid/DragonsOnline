package mc.dragons.core;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.logging.CustomLoggingProvider;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.singletons.Singleton;
import mc.dragons.core.util.singletons.SingletonReInstantiationException;
import mc.dragons.core.util.singletons.Singletons;

public abstract class DragonsJavaPlugin extends JavaPlugin implements Singleton {
	private static List<DragonsJavaPlugin> dragonsPlugins = new ArrayList<>();
	private static Dragons dragons;
	protected static CustomLoggingProvider customLoggingProvider;
	
	private DragonsLogger LOGGER = new DragonsLogger(getName());
	
	public static List<DragonsJavaPlugin> getDragonsPlugins() {
		return dragonsPlugins;
	}

	protected static void setCustomLoggingProvider(CustomLoggingProvider provider) {
		customLoggingProvider = provider;
	}
	
	protected DragonsJavaPlugin() {
		if(Singletons.getInstance(getClass().asSubclass(DragonsJavaPlugin.class), () -> this) != this) {
			throw new SingletonReInstantiationException(getClass());
		}
		dragonsPlugins.add(this);
		if(this instanceof Dragons) {
			dragons = (Dragons) this;
		}
	}
	
	/**
	 * Enable custom debug output for this plugin logger.
	 */
	protected void enableDebugLogging() {
		customLoggingProvider.enableDebugLogging(getLogger());
	}
	
	/**
	 * 
	 * @return A custom logger unique to this plugin which properly
	 * handles debug logging and adds custom log levels
	 */
	public DragonsLogger getLogger() {
		return LOGGER;
	}
	
	/**
	 * 
	 * @return The instance of Dragons Core, if registered.
	 */
	public Dragons getDragonsInstance() {
		return dragons;
	}
}
