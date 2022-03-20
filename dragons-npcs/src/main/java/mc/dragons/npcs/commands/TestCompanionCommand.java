package mc.dragons.npcs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.CompanionUtil;
import mc.dragons.npcs.CompanionAddon;

public class TestCompanionCommand extends DragonsCommandExecutor {
	CompanionAddon addon = (CompanionAddon) dragons.getAddonRegistry().getAddonByName("Companion");
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		Player player = player(sender);
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage("/testcompanion remove");
			sender.sendMessage("/testcompanion create <class>");
			sender.sendMessage("/testcompanion tphere");
			sender.sendMessage("/testcompanion mine");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("create")) {
			NPC companion = GameObjectType.getLoader(NPCLoader.class).registerNew(player.getLocation(), args[1]);
			CompanionUtil.addCompanion(user, companion);
			return true;
		}
		
		else if(args[0].equalsIgnoreCase("mine")) {
			for(NPC npc : CompanionUtil.getCompanions(user)) {
				sender.sendMessage("- " + npc.getNPCClass().getClassName() + " " + npc.getUUID());
			}
		}
		
		else if(args[0].equalsIgnoreCase("tphere")) {
			for(NPC npc : CompanionUtil.getCompanions(user)) {
				if(npc.getEntity() == null) continue;
				npc.getEntity().teleport(player);
			}
		}
		
		else if(args[0].equalsIgnoreCase("remove")) {
			for(NPC npc : CompanionUtil.getCompanions(user)) {
				CompanionUtil.removeCompanion(user, npc);
				npc.remove();
			}
		}
		
		else {
			sender.sendMessage("I dont understandddddd");
		}
		
		return true;
	}

}
