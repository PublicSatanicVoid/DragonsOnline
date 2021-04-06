package mc.dragons.core.gameobject.user;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.gameobject.user.chat.ChatChannel;

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
	default void onInitialize(User user) { /* default */ }
	
	/**
	 * Called when the user joins with verification,
	 * or after skipping verification.
	 * 
	 * @param user
	 */
	default void onVerifiedJoin(User user) { /* default */ }
	
	/**
	 * Called when we need to update the user's
	 * tab list name tag.
	 * 
	 * @param user
	 * @return
	 */
	default String getListNameSuffix(User user) { return ""; }
	
	/**
	 * Called when the user sends a chat message.
	 * 
	 * @param speaking
	 * @param message
	 */
	default void onChat(ChatChannel speaking, String message) { /* default */ }
	
	/**
	 * Called whenever the user's state is updated.
	 * 
	 * @param user
	 * @param location
	 */
	default void onUpdateState(User user, Location location) { /* default */ };
	
	/**
	 * Called whenever the user's data is auto-saved to
	 * the persistent data store.
	 * 
	 * @param user
	 * @param document
	 */
	default void onAutoSave(User user, Document document) { /* default */ }
	
	/**
	 * Called whenever the user dies.
	 * 
	 * @param user
	 * @return Whether the user should be placed on a death countdown at the default spawnpoint.
	 * 	Return true to retain default behavior.
	 * 	Return false only if you plan to handle the death yourself.
	 */
	default boolean onDeath(User user) { return true; }
	
	/**
	 * Called whenever the user leaves the game.
	 * 
	 * @param user
	 */
	default void onQuit(User user) { /* default */ }
}
