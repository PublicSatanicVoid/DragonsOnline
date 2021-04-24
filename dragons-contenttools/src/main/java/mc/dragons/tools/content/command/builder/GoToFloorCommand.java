package mc.dragons.tools.content.command.builder;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class GoToFloorCommand extends DragonsCommandExecutor {

	private GameObjectRegistry registry = dragons.getGameObjectRegistry();
	
	private void listFloors(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Listing all floors:");
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.FLOOR)) {
			Floor floor = (Floor) gameObject;
			sender.sendMessage(ChatColor.GRAY + "- " + floor.getFloorName() + " [" + floor.getWorldName() + "] [Lv " + floor.getLevelMin() + "]");
		}
	}
	
	private void goToFloor(CommandSender sender, String[] args) {
		Floor floor = lookupFloor(sender, args[0]);
		if(floor == null) return;
		user(sender).sendToFloor(floor.getFloorName(), true);
		sender.sendMessage(ChatColor.GREEN + "Teleported to floor successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if(!requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/gotofloor -list");
			sender.sendMessage(ChatColor.YELLOW + "/gotofloor <FloorName>");
			return true;
		}
		
		
		if(args[0].equalsIgnoreCase("-listfloors") || args[0].equalsIgnoreCase("-list") || args[0].equalsIgnoreCase("-l")) {
			listFloors(sender);
			return true;
		}
		
		goToFloor(sender, args);
		return true;
	}

}
