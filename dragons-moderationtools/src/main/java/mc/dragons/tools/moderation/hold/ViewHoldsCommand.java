package mc.dragons.tools.moderation.hold;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;

public class ViewHoldsCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/viewholds <player>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		List<HoldEntry> holds = holdLoader.getActiveHoldsByUser(target).stream()
			.filter(h -> hasPermission(sender, SystemProfileFlag.MODERATION) || h.getBy().equals(user(sender)))
			.collect(Collectors.toList());
		sender.sendMessage(ChatColor.DARK_GREEN + "There are " + holds.size() + " pending holds on " + target.getName());
		for(HoldEntry hold : holds) {
			sender.spigot().sendMessage(
					StringUtil.clickableHoverableText(ChatColor.RED + "[-] ", "/releasehold " + hold.getId(), "Click to release this hold on all affected users"),
					StringUtil.clickableHoverableText(ChatColor.YELLOW + "[i] ", "/viewreport " + hold.getReportId(), "Click to view the report associated with this hold"),
					StringUtil.plainText(ChatColor.GRAY + "#" + hold.getId() + " " + hold.getType() + " " + ChatColor.RESET
							+ StringUtil.parseList(hold.getUsers().stream().map(u -> u.getName()).collect(Collectors.toList()))
							+ ChatColor.GRAY + " (Expires in " + hold.getMaxExpiry() +")"));
		}
		sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "When a hold expires, the account suspension is automatically removed, but you can still take action on the hold.");
		
		return true;
	}

}
