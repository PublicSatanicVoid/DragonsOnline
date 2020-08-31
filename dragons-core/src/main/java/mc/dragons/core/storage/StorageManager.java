package mc.dragons.core.storage;

import java.util.Set;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;

public interface StorageManager {
	StorageAccess getStorageAccess(GameObjectType gameObjectType, UUID uuid);
	StorageAccess getStorageAccess(GameObjectType gameObjectType, Document document);
	Set<StorageAccess> getAllStorageAccess(GameObjectType gameObjectType);
	Set<StorageAccess> getAllStorageAccess(GameObjectType gameObjectType, Document document);
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType);
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType, UUID uuid);
	StorageAccess getNewStorageAccess(GameObjectType gameObjectType, Document document);
	void storeObject(GameObject gameObject);
	void removeObject(GameObject gameObject);
	void push(GameObjectType gameObjectType, Document filter, Document update);
}
