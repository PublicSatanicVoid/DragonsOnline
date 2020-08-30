package mc.dragons.core.gameobject;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.gameobject.loader.GameObjectLoader;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.ItemLoader;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.QuestLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.loader.StructureLoader;
import mc.dragons.core.gameobject.loader.UserLoader;

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
		byte b;
		int i;
		GameObjectType[] arrayOfGameObjectType;
		for (i = (arrayOfGameObjectType = values()).length, b = 0; b < i;) {
			GameObjectType objType = arrayOfGameObjectType[b];
			if (objType.toString().equalsIgnoreCase(type))
				return objType;
			b++;
		}
		return null;
	}
}
