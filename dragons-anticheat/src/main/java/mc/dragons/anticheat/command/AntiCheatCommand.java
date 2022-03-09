package mc.dragons.anticheat.command;

import java.util.Map;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.anticheat.check.move.FastPackets;
import mc.dragons.anticheat.check.move.MoveData;
import mc.dragons.anticheat.util.ACUtil;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.StringUtil;
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
	
	public AntiCheatCommand(DragonsAntiCheat instance) {
		plugin = instance;
		reportLoader = dragons.getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("acping")) {
			if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acping <player>");
				return true;
			}
			Player target = lookupPlayer(sender, args[0]);
			if(target == null) return true;
			sender.sendMessage(ChatColor.GREEN + "Ping of " + target.getName() + " is " + plugin.getCombatRewinder().getInstantaneousPing(target)
					+ "ms (Bukkit: " + plugin.getDragonsInstance().getBridge().getPing(target) + "ms)");
			return true;
		}
		
		else if(label.equalsIgnoreCase("acpps")) {
			if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acpps <player>");
				return true;
			}
			Player target = lookupPlayer(sender, args[0]);
			FastPackets check = plugin.getCheckRegistry().getCheckByClass(FastPackets.class);
			sender.sendMessage(ChatColor.GREEN + "Current PPS of " + target.getName() + " is " + MathUtil.round(check.getPPS(target)) 
				+ " (" + plugin.getCombatRewinder().getInstantaneousPing(target) + "ms i, " 
				+ plugin.getDragonsInstance().getBridge().getPing(target) + "ms b)");
			return true;
		}
		
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
		
		else if(label.equalsIgnoreCase("acdump")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acdump <Player>");
				return true;
			}
			User target = lookupUser(sender, args[0]);
			sender.sendMessage("CHECK - CATEGORY - VL");
			for(Check check : plugin.getCheckRegistry().getChecks()) {
				ViolationData violationData = ViolationData.of(check, target);
				sender.sendMessage(check.getName() + " - " + check.getType() + " - " + MathUtil.round(violationData.vl));
			}
		}
		
		else if(label.equalsIgnoreCase("acresetplayer")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acresetplayer <Player>");
				return true;
			}
			User target = lookupUser(sender, args[0]);
			for(Check check : plugin.getCheckRegistry().getChecks()) {
				ViolationData violationData = ViolationData.of(check, target);
				violationData.vl = 0;
			}
			MoveData moveData = MoveData.of(target);
			moveData.lastRubberband = 0L;
			moveData.lastValidLocation = target.getPlayer().getLocation();
			moveData.validMoves = MoveData.MIN_VALID_MOVES_TO_SET_VALID_LOCATION;
			sender.sendMessage("Reset check data for " + target.getName());
		}
		
		else if(label.equalsIgnoreCase("acflushlog")) {
			plugin.getTestingMoveListener().dumpLog();
			plugin.getTestingMoveListener().disableLog();
			sender.sendMessage(ChatColor.GREEN + "Dumped log to console and cleared");
			return true;
		}
		
		else if(label.equalsIgnoreCase("acstartlog")) {
			plugin.getTestingMoveListener().clearLog();
			plugin.getTestingMoveListener().enableLog();
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
			sender.sendMessage("calculated onGround=" + ACUtil.isOnGround(player));
			
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
			WrappedUser.of(target).punish(PunishmentType.BAN, PunishmentCode.AC_BAN, PunishmentCode.AC_BAN.getStandingLevel(), "ID " + report.getId(), user(sender));
			sender.sendMessage(ChatColor.GREEN + "Anticheat ban executed successfully. Correlated Report ID: " + report.getId());
		}
		
		else if(label.equalsIgnoreCase("ackick")) {
			if(!requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/ackick <player>");
				return true;
			}
			User target = lookupUser(sender, args[0]);
			if(target == null) return true;
			WrappedUser.of(target).punish(PunishmentType.KICK, PunishmentCode.CHEATING_WARNING, 0, null, null);
			sender.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " for illegal client modifications");
		}
		
		else if(label.equalsIgnoreCase("acspoofping")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acspoofping <value|off>");
				return true;
			}
			if(args[0].equalsIgnoreCase("off")) {
				plugin.getCombatRewinder().debug_pingSpoof.remove(player);
				sender.sendMessage(ChatColor.GREEN + "Stopped spoofing your ping.");
			}
			else {
				plugin.getCombatRewinder().debug_pingSpoof.put(player, (long) parseInt(player, args[0]));
				sender.sendMessage(ChatColor.GREEN + "Began spoofing your anticheat ping to " + args[0] + "ms");
			}
		}
		
		else if(label.equalsIgnoreCase("acraytol")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "/acraytol <tolerance>");
				return true;
			}
			Double tol = parseDouble(sender, args[0]);
			if(tol == null) return true;
			plugin.getCombatRewinder().rayTraceTolerance = tol;
			sender.sendMessage(ChatColor.GREEN + "Set combat rewind ray tracing tolerance to +/- " + args[0] + " blocks");
		}
		
		else if(label.equalsIgnoreCase("acstatus")) {
			for(Check check : plugin.getCheckRegistry().getChecks()) {
				sender.sendMessage((check.isEnabled() ? ChatColor.DARK_GREEN : ChatColor.RED) + check.getName() + (check.isEnabled() ? "(Enabled)" : "(Disabled)"));
			}
		}
		
		else if(label.equalsIgnoreCase("actoggle")) {
			// If any check is active, make them all inactive
			// If all checks are inactive, make them all active
			if(args.length == 0) {
				if(plugin.getCheckRegistry().getChecks().stream().filter(c -> c.isEnabled()).count() > 0) {
					plugin.getCheckRegistry().getChecks().forEach(c -> c.setEnabled(false));
					sender.sendMessage(ChatColor.GREEN + "Disabled all checks.");
				}
				else {
					plugin.getCheckRegistry().getChecks().forEach(c -> c.setEnabled(true));
					sender.sendMessage(ChatColor.GREEN + "Enabled all checks.");
				}
			}
			else {
				Check check = plugin.getCheckRegistry().getCheckByName(args[0]);
				if(check == null) {
					sender.sendMessage(ChatColor.RED + "No check by that name exists! /acstatus");
					return true;
				}
				check.setEnabled(!check.isEnabled());
				sender.sendMessage(ChatColor.GREEN + (check.isEnabled() ? "Enabled" : "Disabled") + " check " + check.getName());
			}
		}
		
		else if(label.equalsIgnoreCase("achitstats")) {
			Map<Player, Boolean> toggle = plugin.getCombatRewinder().debug_hitStats;
			toggle.put(player, !toggle.getOrDefault(player, false));
			sender.sendMessage(ChatColor.GREEN + "Toggled hit rejection stats in action bar");
		}
		
		else if(label.equalsIgnoreCase("acresethitstats")) {
			plugin.getCombatRewinder().debug_hitCount.remove(player);
			plugin.getCombatRewinder().debug_rejectedCount.remove(player);
			sender.sendMessage(ChatColor.GREEN + "Reset your hit rejection stats");
		}
		
		else {
			sender.sendMessage(ChatColor.GREEN + "Dragons Online custom anti-cheat (ALPHA)");
		}
		
		return true;
	}

}
