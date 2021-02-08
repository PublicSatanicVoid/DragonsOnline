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
	void onInitialize(User user);
	
	/**
	 * Called when the user joins with verification,
	 * or after skipping verification.
	 * 
	 * @param user
	 */
	void onVerifiedJoin(User user);
	
	/**
	 * Called whenever the user's state is updated.
	 * 
	 * @param user
	 * @param location
	 */
	void onUpdateState(User user, Location location);
	
	/**
	 * Called whenever the user's data is auto-saved to
	 * the persistent data store.
	 * 
	 * @param user
	 * @param document
	 */
	void onAutoSave(User user, Document document);
	
	/**
	 * Called whenever the user leaves the game.
	 * 
	 * @param user
	 */
	void onQuit(User user);
}
