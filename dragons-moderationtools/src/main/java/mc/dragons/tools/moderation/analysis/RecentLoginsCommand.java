package mc.dragons.tools.moderation.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.util.StringUtil;

public class RecentLoginsCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		Integer n = 10;
		if(args.length > 0) {
			n = parseInt(sender, args[0]);
		}
		
		List<User> users = dragons.getMongoConfig()
								.getDatabase()
								.getCollection(MongoConfig.GAMEOBJECTS_COLLECTION)
								.find(new Document("type", GameObjectType.USER.toString()))
								.sort(new Document("lastJoined", -1))
								.limit(n)
								.map(d -> userLoader.loadObject(d.get("_id", UUID.class)))
								.into(new ArrayList<>());
		sender.sendMessage(ChatColor.DARK_GREEN + "" + n + " most recent logins:");
		for(User user : users) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + user.getName() + ChatColor.GRAY + " (" + StringUtil.DATE_FORMAT.format(user.getLastJoined()) + ")");
		}
		
		return true;
	}

}
