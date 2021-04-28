package mc.dragons.npcs;

import java.util.UUID;

import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.StringUtil;

public class NPCUserHook implements UserHook {
	private Dragons dragons;
	private DragonsLogger LOGGER;
	
	public NPCUserHook(DragonsNPCAddons plugin) {
		dragons = plugin.getDragonsInstance();
		LOGGER = dragons.getLogger();
	}

	@Override
	public void onVerifiedJoin(User user) {
		UUID companionUUID = user.getStorageAccess().getDocument().get("companion", UUID.class);
		if(companionUUID == null) return;
		UUID cid = LOGGER.newCID();
		LOGGER.trace(cid, "Loading companion for user " + user.getName() + " (" + user.getUUID() + ") with companion uuid " + companionUUID);
		NPC companion = GameObjectType.NPC.<NPC, NPCLoader>getLoader().loadObject(companionUUID, cid);
		if(companion == null) {
			user.getPlayer().sendMessage(ChatColor.RED + "Your companion could not be found! Try re-joining and if the issue persists report the following error message.");
			user.getPlayer().sendMessage(ChatColor.RED + StringUtil.toHdFont("Correlation ID: " + cid));
			return;
		}
		if(companion.getEntity() == null) {
			LOGGER.trace(cid, "Regenerating companion because Bukkit entity could not be found");
			companion.regenerate(user.getPlayer().getLocation());
			user.debug("Regenerated your companion " + companionUUID);
		}
		user.debug("Spawned your companion " + companion.getName() + " at " + StringUtil.locToString(companion.getEntity().getLocation()));
	}

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
