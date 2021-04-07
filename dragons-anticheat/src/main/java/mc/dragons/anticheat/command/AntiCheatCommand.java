package mc.dragons.anticheat.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;

import mc.dragons.anticheat.DragonsAntiCheatPlugin;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;

public class AntiCheatCommand extends DragonsCommandExecutor {

	private DragonsAntiCheatPlugin plugin;
	
	public AntiCheatCommand(DragonsAntiCheatPlugin instance) {
		plugin = instance;
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		Player player = player(sender);
		
		if(label.equalsIgnoreCase("acdebug")) {
			plugin.setDebug(!plugin.isDebug());
			if(plugin.isDebug()) {
				sender.sendMessage(ChatColor.GREEN + "Now debugging anticheat");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "No longer debugging anticheat");
			}
			return true;
		}
		
		if(label.equalsIgnoreCase("acflushlog")) {
			plugin.getMoveListener().dumpLog();
			plugin.getMoveListener().disableLog();
			sender.sendMessage(ChatColor.GREEN + "Dumped log to console and cleared");
			return true;
		}
		
		if(label.equalsIgnoreCase("acstartlog")) {
			plugin.getMoveListener().clearLog();
			plugin.getMoveListener().enableLog();
			return true;
		}
		
		if(label.equalsIgnoreCase("acblockdata")) {
			IBlockData blockData = ((CraftWorld) player.getWorld()).getHandle().getType(new BlockPosition(player.getLocation().getBlockX(), player.getLocation().getBlockY() - 1, player.getLocation().getBlockZ()));
			Block nmsBlock = blockData.getBlock();
			sender.sendMessage("nmsBlock="+nmsBlock);
			float ff = nmsBlock.getFrictionFactor();
			sender.sendMessage("frictionFactor="+ff);
			ff *= 0.91; // ?
			sender.sendMessage("*multiplier="+ 0.1 * (0.1627714 / (ff * ff * ff)));
			sender.sendMessage("*a="+ff);
			return true;
		}
		
		
		
		sender.sendMessage("Coming Soon...");
		
		return true;
	}

}
