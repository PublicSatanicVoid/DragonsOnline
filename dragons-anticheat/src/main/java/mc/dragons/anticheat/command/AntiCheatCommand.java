package mc.dragons.anticheat.command;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.DragonsModerationTools;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.PunishmentCode;
import mc.dragons.tools.moderation.punishment.PunishmentType;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;
import mc.dragons.tools.moderation.report.ReportLoader.ReportStatus;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;

public class AntiCheatCommand extends DragonsCommandExecutor {
	private DragonsAntiCheat plugin;
	private ReportLoader reportLoader;
	private DragonsModerationTools modToolsPlugin;
	
	public AntiCheatCommand(DragonsAntiCheat instance) {
		plugin = instance;
		reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
		modToolsPlugin = JavaPlugin.getPlugin(DragonsModerationTools.class);
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
		
		else if(label.equalsIgnoreCase("acflushlog")) {
			plugin.getMoveListener().dumpLog();
			plugin.getMoveListener().disableLog();
			sender.sendMessage(ChatColor.GREEN + "Dumped log to console and cleared");
			return true;
		}
		
		else if(label.equalsIgnoreCase("acstartlog")) {
			plugin.getMoveListener().clearLog();
			plugin.getMoveListener().enableLog();
			return true;
		}
		
		else if(label.equalsIgnoreCase("acblockdata")) {
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
		
		else if(label.equalsIgnoreCase("acban")) {
			if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acban <player> [internal info]");
				return true;
			}
			User target = lookupUser(sender, args[0]);
			String info = StringUtil.concatArgs(args, 1);
			if(target == null) return true;
			Report report = reportLoader.fileInternalReport(target, new Document("type", "ac_ban").append("info", info));
			report.addNote("Action automatically taken on this report");
			report.setStatus(ReportStatus.ACTION_TAKEN);
			modToolsPlugin.getPunishCommand().punish(sender, target, PunishmentCode.AC_BAN, "ID " + report.getId());
			sender.sendMessage(ChatColor.GREEN + "Anticheat ban executed successfully. Correlated Report ID: " + report.getId());
		}
		
		else if(label.equalsIgnoreCase("ackick")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/ackick <player>");
				return true;
			}
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			WrappedUser.of(target).punish(PunishmentType.KICK, PunishmentCode.CHEATING_WARNING, 0, null, null);
			sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " for illegal client modifications");
		}
		
		else {
			sender.sendMessage("Coming Soon...");
		}
		
		return true;
	}

}
