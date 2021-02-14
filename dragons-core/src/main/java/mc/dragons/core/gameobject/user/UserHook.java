package mc.dragons.core.gameobject.user;

import org.bson.Document;
import org.bukkit.Location;

/**
 * Event handlers that can be added from other plugins or modules
 * to hook into the default user functionality.
 * 
 * @author Adam
 *
 */
public interface UserHook {
	
	/**
	 * Called when the user object is constructed.
	 * 
	 * @param user
	 */
	default void onInitialize(User user) {}
	
	/**
	 * Called when the user joins with verification,
	 * or after skipping verification.
	 * 
	 * @param user
	 */
	default void onVerifiedJoin(User user) {}
	
	/**
	 * Called when we need to update the user's
	 * tab list name tag.
	 * 
	 * @param user
	 * @return
	 */
	default String getListNameSuffix(User user) { return ""; }
	
	/**
	 * Called whenever the user's state is updated.
	 * 
	 * @param user
	 * @param location
	 */
	default void onUpdateState(User user, Location location) {};
	
	/**
	 * Called whenever the user's data is auto-saved to
	 * the persistent data store.
	 * 
	 * @param user
	 * @param document
	 */
	default void onAutoSave(User user, Document document) {}
	
	/**
	 * Called whenever the user leaves the game.
	 * 
	 * @param user
	 */
	default void onQuit(User user) {}
}
