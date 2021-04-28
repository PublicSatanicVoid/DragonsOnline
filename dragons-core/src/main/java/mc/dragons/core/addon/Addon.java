package mc.dragons.core.addon;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObject;

/**
 * Addons allow the functionality of certain game objects
 * to be programmatically extended in other modules/plugins,
 * providing a standardized API to interface with DragonsCore.
 * 
 * @author Adam
 *
 */
public interface Addon {
	
	/**
	 * 
	 * @return The name of this add-on
	 */
	default String getName() { return ""; }
	
	/**
	 * 
	 * @return The type of this add-on
	 */
	default AddonType getType() { return null; }
	
	/**
	 * Initializes the given game object when it is loaded
	 * from the database or registry.
	 * 
	 * <p>Called every time the game object is constructed.
	 * 
	 * @param gameObject
	 */
	default void initialize(GameObject gameObject) { /* default */ }
	
	/**
	 * Called when the add-on is enabled at server start.
	 */
	default void onEnable() { /* default */ }
	
	/**
	 * Initializes the given game object with any custom
	 * data or features when it is first registered.
	 * 
	 * <p>Only called once per game object.
	 * 
	 * @param initialData
	 */
	default void onCreateStorageAccess(Document initialData) { /* default */ }
}
