package mc.dragons.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class CompanionUtil {
	public static List<NPC> getCompanions(User user, UUID cid) {
		NPCLoader npcLoader = GameObjectType.getLoader(NPCLoader.class);
		List<UUID> companions = user.getData().getList("companions", UUID.class);
		if(companions == null) return new ArrayList<>();
		return companions.stream().map(uuid -> npcLoader.loadObject(uuid, cid)).filter(Objects::nonNull).collect(Collectors.toList());
	}
	
	public static List<NPC> getCompanions(User user) {
		return getCompanions(user, null);
	}
	
	public static boolean hasCompanion(User user) {
		return !getCompanions(user).isEmpty();
	}
	
	public static void addCompanion(User user, NPC npc) {
		List<UUID> companions = user.getData().getList("companions", UUID.class);
		if(companions == null) companions = new ArrayList<>();
		companions.add(npc.getUUID());
		user.setData("companions", companions);
		npc.setData("companionOwner", user.getUUID());
	}
	
	public static void removeCompanion(User user, NPC npc) {
		List<UUID> companions = user.getData().getList("companions", UUID.class);
		if(companions == null) companions = new ArrayList<>();
		companions.remove(npc.getUUID());
		user.setData("companions", companions);
		npc.getStorageAccess().delete("companionOwner");
	}
	
	public static User getCompanionOwner(NPC npc) {
		UUID owner = npc.getStorageAccess().getDocument().get("companionOwner", UUID.class);
		if(owner == null) return null;
		return GameObjectType.getLoader(UserLoader.class).loadObject(owner);
	}
	
	public static boolean isCompanionType(NPC npc) {
		return npc.getNPCType() == NPCType.COMPANION;
	}
	
	public static boolean isCompanion(NPC npc) {
		return isCompanionType(npc) && getCompanionOwner(npc) != null;
	}
	
	public static boolean isCompanionOf(NPC npc, User user) {
		if(!isCompanion(npc)) return false;
		for(NPC test : getCompanions(user)) {
			if(test.getUUID().equals(npc.getUUID())) return true;
		}
		return false;
	}

}
