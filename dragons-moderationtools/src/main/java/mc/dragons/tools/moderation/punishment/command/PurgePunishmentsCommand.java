package mc.dragons.tools.moderation.punishment.command;

import java.util.ArrayList;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.tools.moderation.punishment.StandingLevelType;

public class PurgePunishmentsCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN) || !requirePermission(sender, SystemProfileFlag.MODERATION)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/purgepunishments <player>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		Document standingLevelsEmpty = new Document();
		for(StandingLevelType level : StandingLevelType.values()) {
			standingLevelsEmpty.append(level.toString(), new Document("level", 0).append("on", 0L));
		}
		
		target.getStorageAccess().set("punishmentHistory", new ArrayList<>());
		target.getStorageAccess().set("standingLevel", standingLevelsEmpty);
		
		sender.sendMessage(ChatColor.GREEN + "Purged punishment history of " + target.getName());
		
		return true;
	}
}
