package mc.dragons.tools.dev.gameobject;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;

public class AddonCommand extends DragonsCommandExecutor {
	private AddonRegistry addonRegistry = dragons.getAddonRegistry();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/addon list");
			return true;
		}
		
		if(StringUtil.equalsAnyIgnoreCase(args[0], "list", "-list", "-l")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all addons:");
			addonRegistry.getAllAddons().forEach(addon -> sender.sendMessage(ChatColor.GRAY + "- " + addon.getName() + " [" + addon.getType() + "]"));
			return true;
		}
		
		
		return true;
	}
}
