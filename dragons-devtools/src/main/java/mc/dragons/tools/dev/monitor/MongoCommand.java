package mc.dragons.tools.dev.monitor;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mongodb.client.MongoDatabase;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.MongoConfig;

public class MongoCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		MongoConfig config = instance.getMongoConfig();
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
