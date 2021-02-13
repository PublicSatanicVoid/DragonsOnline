package mc.dragons.core.addon;

import java.util.logging.Logger;

import org.bson.Document;

import mc.dragons.core.Dragons;
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
	Logger LOGGER = Dragons.getInstance().getLogger();

	String getName();
	AddonType getType();
	void initialize(GameObject gameObject);
	void onEnable();
	void onCreateStorageAccess(Document initialData);
}
