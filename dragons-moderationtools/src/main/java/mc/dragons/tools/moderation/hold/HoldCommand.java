package mc.dragons.tools.moderation.hold;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldEntry;
import mc.dragons.tools.moderation.hold.HoldLoader.HoldType;
import mc.dragons.tools.moderation.punishment.StandingLevelType;
import mc.dragons.tools.moderation.util.CmdUtil;
import mc.dragons.tools.moderation.util.CmdUtil.CmdData;

public class HoldCommand extends DragonsCommandExecutor {
	private HoldLoader holdLoader = dragons.getLightweightLoaderRegistry().getLoader(HoldLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, SystemProfileFlag.HELPER)) return true;
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Specify a player and code! /hold <player1 [player2 ...]> <code> [extra info] [--forceEscalate]");
			return true;
		}

		int forceEscalateIndex = StringUtil.getFlagIndex(args, "--forceEscalate", 0);
		
		CmdData data = CmdUtil.parse(sender, "/hold <players> <code> ", args, "--forceEscalate");
		if(data == null) return true;
		boolean error = false;
		for(User target : data.targets) {
			if(WrappedUser.of(target).getStandingLevel(data.code.getType()) + data.code.getStandingLevel() <= 1) {
				sender.sendMessage(ChatColor.RED + "This code (" + data.code.getCode() + ") is not sufficient to place a hold on user " + target.getName());
				error = true;
			}
		}
		if(error) {
			sender.sendMessage(ChatColor.RED + "Could not place hold.");
			return true;
		}
		
		boolean canApply = data.code.canApply(user);
		HoldEntry hold = holdLoader.newHold(data.targets, user, data.formatInternalReason(), null, forceEscalateIndex != -1 || !canApply, 
			data.code.getType() == StandingLevelType.BAN ? HoldType.SUSPEND : HoldType.MUTE);
		
		if(hold == null) {
			sender.sendMessage(ChatColor.RED + "Could not place hold: An internal error occurred.");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Hold placed successfully. Report ID: " + hold.getReportId());
		}
		
		return true;
	}
}
