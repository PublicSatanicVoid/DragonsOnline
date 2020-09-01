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
		this.masterRegistry = instance.getGameObjectRegistry();
	}

	public static synchronized QuestLoader getInstance(Dragons instance, StorageManager storageManager) {
		if (INSTANCE == null)
			INSTANCE = new QuestLoader(instance, storageManager);
		return INSTANCE;
	}

	@Override
	public Quest loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		this.LOGGER.fine("Loading quest " + storageAccess.getIdentifier());
		Quest quest = new Quest(this.storageManager, storageAccess);
		this.masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}

	public Quest getQuestByName(String questName) {
		lazyLoadAll();
		for (GameObject gameObject : this.masterRegistry.getRegisteredObjects(new GameObjectType[] { GameObjectType.QUEST })) {
			Quest quest = (Quest) gameObject;
			if (quest.getName().equalsIgnoreCase(questName))
				return quest;
		}
		return null;
	}

	public Quest registerNew(String name, String questName, int lvMin) {
		lazyLoadAll();
		this.LOGGER.fine("Registering new quest " + name);
		Document data = (new Document("_id", UUID.randomUUID())).append("name", name).append("questName", questName).append("lvMin", Integer.valueOf(lvMin)).append("steps", new ArrayList<>());
		StorageAccess storageAccess = this.storageManager.getNewStorageAccess(GameObjectType.QUEST, data);
		Quest quest = new Quest(this.storageManager, storageAccess);
		this.masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}

	public void loadAll(boolean force) {
		if (this.allLoaded && !force)
			return;
		this.LOGGER.fine("Loading all quests...");
		this.allLoaded = true;
		this.masterRegistry.removeFromRegistry(GameObjectType.QUEST);
		this.storageManager.getAllStorageAccess(GameObjectType.QUEST).stream().forEach(storageAccess -> this.masterRegistry.getRegisteredObjects().add(loadObject(storageAccess)));
	}

	public void lazyLoadAll() {
		loadAll(false);
	}
}
