package mc.dragons.core.gameobject.quest;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.Document;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectLoader;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class QuestLoader extends GameObjectLoader<Quest> {
	private static QuestLoader INSTANCE;

	private Logger LOGGER = Dragons.getInstance().getLogger();

	private GameObjectRegistry masterRegistry;

	private boolean allLoaded = false;

	private QuestLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
	}

	public static synchronized QuestLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null) {
			INSTANCE = new QuestLoader(instance, storageManager);
		}
		return INSTANCE;
	}

	@Override
	public Quest loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		LOGGER.fine("Loading quest " + storageAccess.getIdentifier());
		Quest quest = new Quest(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}

	public Quest getQuestByName(String questName) {
		lazyLoadAll();
		for (GameObject gameObject : masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.QUEST })) {
			Quest quest = (Quest) gameObject;
			if (quest.getName().equalsIgnoreCase(questName)) {
				return quest;
			}
		}
		return null;
	}

	public Quest registerNew(String name, String questName, int lvMin) {
		lazyLoadAll();
		LOGGER.fine("Registering new quest " + name);
		Document data = new Document("_id", UUID.randomUUID()).append("name", name).append("questName", questName).append("lvMin", Integer.valueOf(lvMin)).append("steps", new ArrayList<>());
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.QUEST, data);
		Quest quest = new Quest(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}

	public void loadAll(boolean force) {
		if (allLoaded && !force) {
			return;
		}
		LOGGER.fine("Loading all quests...");
		allLoaded = true;
		masterRegistry.removeFromRegistry(GameObjectType.QUEST);
		storageManager.getAllStorageAccess(GameObjectType.QUEST).stream().forEach(storageAccess -> masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
