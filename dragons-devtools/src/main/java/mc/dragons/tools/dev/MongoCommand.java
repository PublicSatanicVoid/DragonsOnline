package mc.dragons.tools.dev;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mongodb.client.MongoDatabase;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.core.util.PermissionUtil;

public class MongoCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		User user = null;
		Player player = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		MongoConfig config = Dragons.getInstance().getMongoConfig();
		MongoDatabase db = config.getDatabase();
		if(db == null) {
			sender.sendMessage(ChatColor.RED + "No MongoDB instance connected!");
			return true;
		}
		sender.sendMessage(ChatColor.GREEN + "Connected to MongoDB instance " + config.getHost() + "/" + config.getPort());
		sender.sendMessage(ChatColor.GREEN + "Database name: " + db.getName());
		sender.sendMessage(ChatColor.GREEN + "Collections: ");
		for(String collection : db.listCollectionNames()) {
			sender.sendMessage(ChatColor.GRAY + "- " + collection);
		}
		
		return true;
	}

}
