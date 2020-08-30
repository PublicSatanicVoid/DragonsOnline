package mc.dragons.tools.dev;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class AddonCommand implements CommandExecutor {

	private AddonRegistry addonRegistry;
	
	public AddonCommand(Dragons instance) {
		addonRegistry = instance.getAddonRegistry();
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/addon -list");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-list") || args[0].equalsIgnoreCase("-l")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all addons:");
			addonRegistry.getAllAddons().forEach(addon -> sender.sendMessage(ChatColor.GRAY + "- " + addon.getName() + " [" + addon.getType() + "]"));
			return true;
		}
		
		
		return true;
	}
}
