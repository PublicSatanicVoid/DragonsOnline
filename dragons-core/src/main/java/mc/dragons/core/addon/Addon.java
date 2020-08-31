package mc.dragons.core.addon;

import java.util.logging.Logger;

import org.bson.Document;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;

public interface Addon {
	public static final Logger LOGGER = Dragons.getInstance().getLogger();

	String getName();
	AddonType getType();
	void initialize(GameObject paramGameObject);
	void onEnable();
	void onCreateStorageAccess(Document paramDocument);
}
