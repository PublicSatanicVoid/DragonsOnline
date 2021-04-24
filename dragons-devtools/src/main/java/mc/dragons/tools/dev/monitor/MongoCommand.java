package mc.dragons.tools.dev.monitor;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mongodb.client.MongoDatabase;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.MongoConfig;

public class MongoCommand extends DragonsCommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true;
		
		MongoConfig config = dragons.getMongoConfig();
		MongoDatabase db = config.getDatabase();
		if(db == null) {
			sender.sendMessage(ChatColor.RED + "No MongoDB instance connected!");
			return true;
		}
		
		sender.sendMessage(ChatColor.DARK_GREEN + "Connected to MongoDB");
		sender.sendMessage(ChatColor.GREEN + "IP: " + ChatColor.GRAY + config.getHost() + ":" + config.getPort());
		sender.sendMessage(ChatColor.GREEN + "Database name: " + ChatColor.GRAY + db.getName());
		sender.sendMessage(ChatColor.GREEN + "Collections: " + ChatColor.GRAY + db.listCollectionNames().into(new ArrayList<>()).stream()
				.map(coll -> coll + " (" + db.getCollection(coll).estimatedDocumentCount() + ")").reduce((a,b) -> a + ", " + b).get());
		
		return true;
	}

}
