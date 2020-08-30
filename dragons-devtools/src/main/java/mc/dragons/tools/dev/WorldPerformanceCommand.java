package mc.dragons.tools.dev;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.PermissionUtil;

public class WorldPerformanceCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			User user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		sender.sendMessage(ChatColor.GREEN + "World Performance Statistics:");
		for(World w : Bukkit.getWorlds()) {
			long populatedChunks = Arrays.stream(w.getLoadedChunks())
					.filter(ch -> Arrays.stream(ch.getEntities()).filter(e -> e.getType() == EntityType.PLAYER).count() > 0)
					.count();
			sender.sendMessage(ChatColor.GRAY + "- " + w.getName());
			sender.sendMessage(ChatColor.GRAY + "   - Entities: " + ChatColor.GREEN + w.getEntities().size() 
					+ " (" + w.getLivingEntities().size() + " Living, " + w.getPlayers().size() + " Players)");
			sender.sendMessage(ChatColor.GRAY + "   - Chunks: " + ChatColor.GREEN + w.getLoadedChunks().length + " Loaded, " 
					+ populatedChunks + " Populated (" + MathUtil.round(100 * (double) populatedChunks / w.getLoadedChunks().length) + "%)");
		}
		return true;
	}

}
