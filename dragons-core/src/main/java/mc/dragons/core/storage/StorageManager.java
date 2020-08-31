package mc.dragons.core.storage;

import java.util.Set;
import java.util.UUID;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import org.bson.Document;

public interface StorageManager {
	StorageAccess getStorageAccess(GameObjectType paramGameObjectType, UUID paramUUID);
	StorageAccess getStorageAccess(GameObjectType paramGameObjectType, Document paramDocument);
	Set<StorageAccess> getAllStorageAccess(GameObjectType paramGameObjectType);
	Set<StorageAccess> getAllStorageAccess(GameObjectType paramGameObjectType, Document paramDocument);
	StorageAccess getNewStorageAccess(GameObjectType paramGameObjectType);
	StorageAccess getNewStorageAccess(GameObjectType paramGameObjectType, UUID paramUUID);
	StorageAccess getNewStorageAccess(GameObjectType paramGameObjectType, Document paramDocument);
	void storeObject(GameObject paramGameObject);
	void removeObject(GameObject paramGameObject);
	void push(GameObjectType paramGameObjectType, Document paramDocument1, Document paramDocument2);
}
