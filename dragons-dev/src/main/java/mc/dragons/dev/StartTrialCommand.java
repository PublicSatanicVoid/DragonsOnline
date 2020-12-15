package mc.dragons.dev;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.loader.WarpLoader;
import mc.dragons.core.util.PermissionUtil;
import net.md_5.bungee.api.ChatColor;

public class StartTrialCommand implements CommandExecutor {

	private WarpLoader warpLoader;
	
	public StartTrialCommand() {
		warpLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(WarpLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Begin a build trial for the specified trial builder.");
			sender.sendMessage(ChatColor.YELLOW + "/starttrial <Player>");
			return true;
		}
		
		String target = args[0];
		
		FloorLoader floorLoader = GameObjectType.FLOOR.<Floor, FloorLoader>getLoader();
		Floor trial = floorLoader.registerNew("Trial-" + target, "trial-" + target, target + "'s Trial", 0, true);
		
		if(trial == null) {
			sender.sendMessage(ChatColor.RED + "An error occurred while creating a trial for player " + target);
			return true;
		}
		
		warpLoader.addWarp("trial-" + target, Bukkit.getWorld("trial-" + target).getSpawnLocation());
		
		sender.sendMessage(ChatColor.GREEN + "Created the trial for player " + target + " successfully. Do /warp trial-" + target + " to access it.");
		
		return true;
	}

}
