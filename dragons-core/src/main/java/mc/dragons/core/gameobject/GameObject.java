package mc.dragons.core.gameobject;

import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;
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
	protected static DragonsLogger LOGGER = Dragons.getInstance().getLogger();

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
		LOGGER.verbose("Initializing game object (" + type + ", " + uuid + ", " + storageManager + ")");
	}

	protected GameObject(StorageManager storageManager, StorageAccess storageAccess) {
		this.storageManager = storageManager;
		this.storageAccess = storageAccess;
		localData = new Document();
		LOGGER.verbose("Initializing game object (" + storageManager + ", " + storageAccess + ")");
	}

	/**
	 * Set the specified key and value in the associated data store.
	 * 
	 * @param key
	 * @param value
	 */
	protected void setData(String key, Object value) {
		storageAccess.set(key, value);
		LOGGER.verbose("Set data on " + this + ": " + key + "=" + value);
	}

	/**
	 * Removes the specified key from the associated data store.
	 * 
	 * @param key
	 */
	protected void removeData(String key) {
		storageAccess.delete(key);
		LOGGER.verbose("Remove data on " + this + ": " + key);
	}
	
	/**
	 * Update the specified data on the associated data store.
	 * 
	 * @param document
	 */
	protected void update(Document document) {
		storageAccess.update(document);
		LOGGER.verbose("Update document on " + this + ": " + document);
	}

	/**
	 * 
	 * @param key
	 * @return The value associated with the specified key in the associated data store.
	 */
	protected Object getData(String key) {
		return storageAccess.get(key);
	}

	/**
	 * 
	 * @return The type of this game object.
	 */
	public final GameObjectType getType() {
		return storageAccess.getIdentifier().getType();
	}

	/**
	 * 
	 * @return The unique identifier of this game object.
	 */
	public final UUID getUUID() {
		return storageAccess.getIdentifier().getUUID();
	}

	/**
	 * 
	 * @return The data store unique to this game object.
	 * 
	 * @implNote May or may not be persistent.
	 */
	public StorageAccess getStorageAccess() {
		return storageAccess;
	}
	
	/**
	 * Change the data store for this game object.
	 * 
	 * <p>Existing data is overwritten.
	 * 
	 * @param replacement
	 */
	public void replaceStorageAccess(StorageAccess replacement) {
		storageAccess = replacement;
	}

	/**
	 * 
	 * @return The underlying, non-persistent document representing
	 * this game object's data store.
	 */
	public Document getData() {
		return storageAccess.getDocument();
	}

	/**
	 * 
	 * @return The unique identifier for this game object.
	 */
	public Identifier getIdentifier() {
		return storageAccess.getIdentifier();
	}

	/**
	 * Run any periodic auto-save functionality, for example to
	 * synchronize live Bukkit data to the data store.
	 */
	public void autoSave() {
		LOGGER.trace("Auto-saving game object " + getIdentifier());
	}

	/**
	 * 
	 * @return The non-persistent document representing <i>local-only</i>
	 * data. Not associated with the storage access.
	 */
	public Document getLocalData() {
		return localData;
	}

	/**
	 * Whether this game object represents the same game object as another.
	 * 
	 * <p>Typically there should only be one reference per game object,
	 * but use of this method is still encouraged.
	 */
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
