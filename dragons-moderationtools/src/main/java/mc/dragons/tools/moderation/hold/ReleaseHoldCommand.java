package mc.dragons.tools.moderation.hold;

import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldStatus;

public class ReleaseHoldCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.APPEALS_TEAM) && (!requirePermission(sender, SystemProfileFlag.MODERATION) || !requirePermission(sender, PermissionLevel.ADMIN))) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/releasehold <hold id>");
			return true;
		}
		
		Integer id = parseInt(sender, args[0]);
		if(id == null) return true;
		
		HoldEntry hold = holdLoader.getHoldById(id);
		hold.setStatus(HoldStatus.CLOSED_NOACTION);
		sender.sendMessage(ChatColor.GREEN + "Released hold #" + id + " on " + StringUtil.parseList(hold.getUsers().stream().map(u -> u.getName()).collect(Collectors.toList())));
		
		return true;
	}

}
