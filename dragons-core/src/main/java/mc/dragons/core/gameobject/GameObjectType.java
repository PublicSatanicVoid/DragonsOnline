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
import mc.dragons.core.storage.StorageManager;

/**
 * Possible types of a {@link mc.dragons.core.gameobject.GameObject}.
 * 
 * A registry is associated with each object type, which is responsible for loading
 * and creating instances of that type.
 * 
 * @author Adam
 *
 */
public enum GameObjectType {
	USER(true, UserLoader.getInstance(dragons(), psm())),
	ITEM_CLASS(false, ItemClassLoader.getInstance(dragons(), psm())),
	ITEM(false, ItemLoader.getInstance(dragons(), psm())),
	NPC_CLASS(false, NPCClassLoader.getInstance(dragons(), psm())),
	NPC(true, NPCLoader.getInstance(dragons(), psm())),
	QUEST(false, QuestLoader.getInstance(dragons(), psm())),
	STRUCTURE(false, StructureLoader.getInstance(dragons(), psm())),
	REGION(false, RegionLoader.getInstance(dragons(), psm())),
	FLOOR(false, FloorLoader.getInstance(dragons(), psm()));
	
	private static Dragons dragons() { return Dragons.getInstance(); }
	private static StorageManager psm() { return dragons().getPersistentStorageManager(); }
	
	private GameObjectLoader<?> loader;

	<T extends GameObject> GameObjectType(boolean autoSaveable, GameObjectLoader<T> loader) {
		this.loader = loader;
	}

	/**
	 * 
	 * @param <T> The game object type
	 * @param <LT>The game object loader type
	 * @return The GameObjectLoader for this type
	 */
	@SuppressWarnings("unchecked")
	public <T extends GameObject, LT extends GameObjectLoader<T>> LT getLoader() {
		return (LT) loader;
	}

	/**
	 * 
	 * @param type
	 * @return The GameObjectType of the specified name
	 */
	public static GameObjectType get(String type) {
		for(GameObjectType objType : values()) {
			if(objType.toString().equalsIgnoreCase(type)) {
				return objType;
			}
		}
		return null;
	}
}
