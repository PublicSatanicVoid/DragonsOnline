package mc.dragons.npcs;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.util.StringUtil;

public class NPCUserHook implements UserHook {

	@Override
	public void onInitialize(User user) {}

	@Override
	public void onVerifiedJoin(User user) {
		UUID companionUUID = user.getStorageAccess().getDocument().get("companion", UUID.class);
		if(companionUUID == null) return;
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().loadObject(companionUUID);
		if(companion == null) {
			user.getPlayer().sendMessage(ChatColor.RED + "Your companion could not be found! Try re-joining and if the issue persists screenshot this error and report it: " + companionUUID);
			return;
		}
		if(companion.getEntity() == null) {
			companion.regenerate(user.getPlayer().getLocation());
			user.debug("Regenerated your companion " + companionUUID);
		}
		user.debug("Spawned your companion " + companion.getName() + " at " + StringUtil.locToString(companion.getEntity().getLocation()));
	}

	@Override
	public void onUpdateState(User user, Location cached) {}

	@Override
	public void onAutoSave(User user, Document autoSaveData) {}

	@Override
	public void onQuit(User user) {
		UUID companionUUID = user.getStorageAccess().getDocument().get("companion", UUID.class);
		if(companionUUID == null) return;
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().loadObject(companionUUID);
		if(companion == null) return;
		if(companion.getEntity() == null) return;
		if(companion.getEntity().isDead() || !companion.getEntity().isValid()) return;
		companion.getEntity().remove();
	}
	
}
