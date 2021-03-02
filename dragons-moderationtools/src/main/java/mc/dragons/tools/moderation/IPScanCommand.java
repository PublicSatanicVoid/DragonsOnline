package mc.dragons.tools.moderation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.punishment.PunishmentData;
import mc.dragons.core.gameobject.user.punishment.PunishmentType;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class IPScanCommand implements CommandExecutor {

	private UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	private StorageManager storageManager = Dragons.getInstance().getPersistentStorageManager();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.MODERATION, true)) return true;
		}	
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/ipscan <player>");
			return true;
		}
		
		User target = userLoader.loadObject(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user was not found!");
			return true;
		}
		
		Set<User> alts = storageManager.getAllStorageAccess(GameObjectType.USER, new Document("ipHistory", new Document("$in", target.getIPHistory())))
				.stream().map(storageAccess -> userLoader.loadObject(storageAccess))
				.sorted((u, v) -> u.getPunishmentHistory().size() - v.getPunishmentHistory().size())
				.collect(Collectors.toSet());
		alts.remove(target);
		
		if(alts.size() == 0) {
			sender.sendMessage(ChatColor.GREEN + "No possible alt accounts found for user " + target.getName());
			return true;
		}
		
		Date now = Date.from(Instant.now());
		sender.sendMessage(ChatColor.DARK_GREEN + "" + alts.size() + " possible alt accounts found for user " + target.getName());
		for(User alt : alts) {
			List<String> flags = new ArrayList<>();
			PunishmentData ban = alt.getActivePunishmentData(PunishmentType.BAN);
			PunishmentData mute = alt.getActivePunishmentData(PunishmentType.MUTE);
			if(ban != null && (ban.isPermanent() || ban.getExpiry().after(now))) {
				flags.add("Banned");
			}
			if(mute != null && (mute.isPermanent() || mute.getExpiry().after(now))) {
				flags.add("Muted");
			}
			String data = flags.size() == 0 ? "" : ChatColor.RED + " (" + StringUtil.parseList(flags) + ")";
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + alt.getName() + ChatColor.GRAY + " (" + alt.getPunishmentHistory().size() + " punishments)" + data);
		}
		
		return true;
	}

}
