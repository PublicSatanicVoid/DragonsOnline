package mc.dragons.core.gameobject;

import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Represents an object in the game. This could be an NPC,
 * an item, a structure, etc.
 * 
 * <p>An {@link org.bson.Document} representation can be fetched
 * via the getData() method, and represents enough of the object's
 * data to fully construct it at any given time. This data may be
 * stored in some form or another of persistent storage, and should
 * only use basic data types like String, double, etc. or sub-Documents.
 * 
 * <p>Subclasses should ensure that all changes to fields are reflected
 * in the data using the protected setData() and getData() methods.
 * 
 * @author Adam 
 *
 */
public abstract class GameObject {
	protected static Logger LOGGER = Dragons.getInstance().getLogger();

	protected StorageManager storageManager;
	protected StorageAccess storageAccess;
	protected Document localData;

	protected GameObject(GameObjectType type, StorageManager storageManager) {
		this(storageManager, storageManager.getNewStorageAccess(type));
	}

	protected GameObject(GameObjectType type, UUID uuid, StorageManager storageManager) {
		this.storageManager = storageManager;
		storageAccess = storageManager.getStorageAccess(type, uuid);
		localData = new Document();
		LOGGER.finer("Initializing game object (" + type + ", " + uuid + ", " + storageManager + ")");
	}

	protected GameObject(StorageManager storageManager, StorageAccess storageAccess) {
		this.storageManager = storageManager;
		this.storageAccess = storageAccess;
		localData = new Document();
		LOGGER.finer("Initializing game object (" + storageManager + ", " + storageAccess + ")");
	}

	protected void setData(String key, Object value) {
		storageAccess.set(key, value);
		LOGGER.finest("Set data on " + this + ": " + key + "=" + value);
	}

	protected void update(Document document) {
		storageAccess.update(document);
		LOGGER.finest("Update document on " + this + ": " + document);
	}

	protected Object getData(String key) {
		return storageAccess.get(key);
	}

	public final GameObjectType getType() {
		return storageAccess.getIdentifier().getType();
	}

	public final UUID getUUID() {
		return storageAccess.getIdentifier().getUUID();
	}

	public StorageAccess getStorageAccess() {
		return storageAccess;
	}
	
	public void replaceStorageAccess(StorageAccess replacement) {
		storageAccess = replacement;
	}

	public Document getData() {
		return storageAccess.getDocument();
	}

	public Identifier getIdentifier() {
		return storageAccess.getIdentifier();
	}

	public void autoSave() {
		LOGGER.fine("Auto-saving game object " + getIdentifier());
	}

	public Document getLocalData() {
		return localData;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (!(object instanceof GameObject)) {
			return false;
		}
		GameObject gameObject = (GameObject) object;
		return getType() == gameObject.getType() && getUUID().equals(gameObject.getUUID());
	}
}
