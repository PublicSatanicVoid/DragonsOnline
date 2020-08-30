package mc.dragons.core.gameobject;

import java.util.UUID;
import java.util.logging.Logger;
import mc.dragons.core.Dragons;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import org.bson.Document;

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
		this.storageAccess = storageManager.getStorageAccess(type, uuid);
		this.localData = new Document();
		LOGGER.finer("Initializing game object (" + type + ", " + uuid + ", " + storageManager + ")");
	}

	protected GameObject(StorageManager storageManager, StorageAccess storageAccess) {
		this.storageManager = storageManager;
		this.storageAccess = storageAccess;
		this.localData = new Document();
		LOGGER.finer("Initializing game object (" + storageManager + ", " + storageAccess + ")");
	}

	protected void setData(String key, Object value) {
		this.storageAccess.set(key, value);
		LOGGER.finest("Set data on " + this + ": " + key + "=" + value);
	}

	protected void update(Document document) {
		this.storageAccess.update(document);
		LOGGER.finest("Update document on " + this + ": " + document);
	}

	protected Object getData(String key) {
		return this.storageAccess.get(key);
	}

	public final GameObjectType getType() {
		return this.storageAccess.getIdentifier().getType();
	}

	public final UUID getUUID() {
		return this.storageAccess.getIdentifier().getUUID();
	}

	public StorageAccess getStorageAccess() {
		return this.storageAccess;
	}

	public Document getData() {
		return this.storageAccess.getDocument();
	}

	public Identifier getIdentifier() {
		return this.storageAccess.getIdentifier();
	}

	public void autoSave() {
		LOGGER.fine("Auto-saving game object " + getIdentifier());
	}

	public Document getLocalData() {
		return this.localData;
	}

	public boolean equals(Object object) {
		if (object == null)
			return false;
		if (!(object instanceof GameObject))
			return false;
		GameObject gameObject = (GameObject) object;
		return (getType() == gameObject.getType() && getUUID().equals(gameObject.getUUID()));
	}
}
