package mc.dragons.core.gameobject;

import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.structure.StructureLoader;
import mc.dragons.core.gameobject.user.UserLoader;

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
	USER(true, UserLoader.getInstance()),
	ITEM_CLASS(false, ItemClassLoader.getInstance()),
	ITEM(false, ItemLoader.getInstance()),
	NPC_CLASS(false, NPCClassLoader.getInstance()),
	NPC(true, NPCLoader.getInstance()),
	QUEST(false, QuestLoader.getInstance()),
	STRUCTURE(false, StructureLoader.getInstance()),
	REGION(false, RegionLoader.getInstance()),
	FLOOR(false, FloorLoader.getInstance());
	
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
	
	/**
	 * 
	 * @param <GT>
	 * @param <T>
	 * @param clazz
	 * @return The loader associated with the specified class.
	 */
	public static <GT extends GameObject, T extends GameObjectLoader<GT>> T getLoader(Class<? extends T> clazz) {
		for(GameObjectType type : values()) {
			if(type.getLoader().getClass().isAssignableFrom(clazz)) {
				return type.getLoader();
			}
		}
		return null;
	}
}
