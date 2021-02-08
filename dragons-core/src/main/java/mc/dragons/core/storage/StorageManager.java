package mc.dragons.core.storage;

import java.util.Set;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;

/**
 * Responsible for creating and managing instances of StorageAccess.
 * Different storage media implement this differently; some may
 * persist the data, while others may only store data in-memory.
 * 
 * @author Adam
 *
 */
public interface StorageManager {
	
	/**
	 * Retrieves a storage access from the data store.
	 * 
	 * @param gameObjectType
	 * @param uuid
	 * @return The associated StorageAccess, or null if none was found.
	 */
	StorageAccess getStorageAccess(GameObjectType gameObjectType, UUID uuid);
	
	/**
	 * Retrieves a storage access from the data store.
	 * 
	 * @param gameObjectType
	 * @param document A series of key-value pairs that the storage access must satisfy.
	 * @return The associated StorageAccess, or null if none was found.
	 */
	StorageAccess getStorageAccess(GameObjectType gameObjectType, Document document);
	
	/**
	 * Retrieves all storage accesses from the data store of a given type.
	 * 
	 * @param gameObjectType
	 * @return The set of associated StorageAccesses. Empty if none were found.
	 */
	Set<StorageAccess> getAllStorageAccess(GameObjectType gameObjectType);
	
	/**
	 * Retrieves all storage accesses from the data store matching the given criteria.
	 * 
	 * @param gameObjectType
	 * @param document A series of key-value pairs that the storage accesses must satisfy.
	 * @return The set of associated StorageAccesses. Empty if none were found.
	 */
	Set<StorageAccess> getAllStorageAccess(GameObjectType gameObjectType, Document document);
	
	/**
	 * Creates a new storage access in the data store of the given object type.
	 * 
	 * @param gameObjectType
	 * @return The created StorageAccess, or null if it could not be created.
	 */
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType);
	
	/**
	 * Creates a new storage access in the data store of the given object type and with
	 * the given UUID.
	 * 
	 * @param gameObjectType
	 * @param uuid
	 * @return The created StorageAccess, or null if it could not be created.
	 */
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType, UUID uuid);
	
	/**
	 * Creates a new storage access in the data store of the given object type and with
	 * the specified data.
	 * 
	 * @param gameObjectType
	 * @param document
	 * @return The created StorageAccess, or null if it could not be created.
	 */
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType, Document document);
	
	/**
	 * Stores the given game object's data in the data store.
	 * 
	 * @param gameObject
	 */
	void storeObject(GameObject gameObject);
	
	/**
	 * Removes all data associated with the given game object from the data store.
	 * 
	 * @param gameObject
	 */
	void removeObject(GameObject gameObject);
	
	/**
	 * Updates multiple entries in the data store.
	 * 
	 * @param gameObjectType The type of objects to update.
	 * @param filter The data that an entry must match to be updated.
	 * @param update The data that will be updated or appended on a matching entry.
	 */
	void push(GameObjectType gameObjectType, Document filter, Document update);
}
