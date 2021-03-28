package mc.dragons.tools.moderation.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.StringUtil;

public class IPScanCommand extends DragonsCommandExecutor {
	private StorageManager storageManager = instance.getPersistentStorageManager();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.MODERATOR)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/ipscan <player>");
			return true;
		}
		
		User target = lookupUser(sender, args[0]);
		if(target == null) return true;
		
		Set<User> alts = storageManager.getAllStorageAccess(GameObjectType.USER, new Document("ipHistory", new Document("$in", target.getIPHistory())))
				.stream().map(storageAccess -> userLoader.loadObject(storageAccess))
				.sorted((u, v) -> u.getPunishmentHistory().size() - v.getPunishmentHistory().size())
				.collect(Collectors.toSet());
		alts.remove(target);
		
		if(alts.size() == 0) {
			sender.sendMessage(ChatColor.GREEN + "No possible alt accounts found for user " + target.getName());
			return true;
		}
		
		sender.sendMessage(ChatColor.DARK_GREEN + "" + alts.size() + " possible alt accounts found for user " + target.getName());
		for(User alt : alts) {
			List<String> flags = new ArrayList<>();
			PunishmentData ban = alt.getActivePunishmentData(PunishmentType.BAN);
			PunishmentData mute = alt.getActivePunishmentData(PunishmentType.MUTE);
			if(ban != null) {
				flags.add("Banned");
			}
			if(mute != null) {
				flags.add("Muted");
			}
			String data = flags.size() == 0 ? "" : ChatColor.RED + " (" + StringUtil.parseList(flags) + ")";
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + alt.getName() + ChatColor.GRAY + " (" + alt.getPunishmentHistory().size() + " punishments)" + data);
		}
		
		return true;
	}

}
