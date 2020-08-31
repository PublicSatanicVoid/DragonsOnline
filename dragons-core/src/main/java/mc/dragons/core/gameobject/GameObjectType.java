package mc.dragons.core.gameobject;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.structure.StructureLoader;
import mc.dragons.core.gameobject.user.UserLoader;

public enum GameObjectType {
	USER(UserLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	ITEM_CLASS(ItemClassLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	ITEM(ItemLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	NPC_CLASS(NPCClassLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	NPC(NPCLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	QUEST(QuestLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	STRUCTURE(StructureLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	REGION(RegionLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager())),
	FLOOR(FloorLoader.getInstance(Dragons.getInstance(), Dragons.getInstance().getPersistentStorageManager()));

	private GameObjectLoader<?> loader;

	<T extends GameObject> GameObjectType(GameObjectLoader<T> loader) {
		this.loader = loader;
	}

	@SuppressWarnings("unchecked")
	public <T extends GameObject, LT extends GameObjectLoader<T>> LT getLoader() {
		return (LT) this.loader;
	}

	public static GameObjectType get(String type) {
		for(GameObjectType objType : values()) {
			if(objType.toString().equalsIgnoreCase(type)) return objType;
		}
		return null;
	}
}
