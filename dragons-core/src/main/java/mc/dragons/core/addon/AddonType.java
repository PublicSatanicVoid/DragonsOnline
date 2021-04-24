package mc.dragons.core.addon;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.npc.NPC;

public enum AddonType {
	ITEM(Item.class), 
	NPC(NPC.class);

	private Class<? extends GameObject> bindType;

	AddonType(Class<? extends GameObject> bindType) {
		this.bindType = bindType;
	}

	/**
	 * 
	 * @return The class that this add-on binds to.
	 */
	public Class<? extends GameObject> getBindType() {
		return bindType;
	}
}
