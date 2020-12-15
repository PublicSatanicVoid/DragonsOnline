package mc.dragons.social;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.social.GuildLoader.Guild;

public class GuildCommand implements CommandExecutor {

	GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		List<Guild> guilds = guildLoader.getAllGuildsWith(user.getUUID());
		
		if(args.length == 0) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.GRAY + "You don't belong to a guild.");
			}
			sender.sendMessage(ChatColor.YELLOW + "/guild list" + ChatColor.AQUA + " lists all public guilds.");
			sender.sendMessage(ChatColor.YELLOW + "/guild join <GuildName>" + ChatColor.AQUA + " requests to join a guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild info <GuildName>" + ChatColor.AQUA + " views information about a guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild invite <Player>" + ChatColor.AQUA + " invites a player to your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild kick <Player>" + ChatColor.AQUA + " kicks a player from your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild leave" + ChatColor.AQUA + " leave your current guild.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds:");
			for(Guild guild : guildLoader.getAllGuilds()) {
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + guild.getName() + ChatColor.GRAY + " (" + guild.getMembers() + " members, " + guild.getXP() + " XP)");
			}
			return true;
		}
		
		
		
		return true;
	}

}
