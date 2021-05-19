package mc.dragons.tools.moderation.punishment.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.moderation.WrappedUser;
import mc.dragons.tools.moderation.punishment.StandingLevelType;

public class StandingLevelCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length < 3) {
			sender.sendMessage(ChatColor.RED +"/setsl <player> <type> <level>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		StandingLevelType type = StringUtil.parseEnum(sender, StandingLevelType.class, args[1].toUpperCase());
		Integer level = parseInt(sender, args[2]);
		if(target == null || type == null || level == null) return true;
		
		WrappedUser wrapped = WrappedUser.of(target);
		
		wrapped.setStandingLevel(type, level);
		sender.sendMessage(ChatColor.GREEN + "Updated " + target.getName() + "'s standing level successfully.");
		
		return true;
	}

}
