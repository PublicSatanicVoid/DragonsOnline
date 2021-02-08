package mc.dragons.social;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.GuildLoader.Guild;
import mc.dragons.social.GuildLoader.GuildAccessLevel;

public class GuildCommand implements CommandExecutor {

	GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		List<Guild> guilds = guildLoader.getAllGuildsWith(user.getUUID());
		
		if(args.length == 0) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.GRAY + "You don't belong to a guild.");
			}
			else {
				sender.sendMessage(ChatColor.GRAY + "Your guilds: " + ChatColor.WHITE 
						+ StringUtil.parseList(guilds.stream().map(g -> g.getName()).collect(Collectors.toList()), 
								ChatColor.GRAY + ", " + ChatColor.WHITE));
			}
			sender.sendMessage(ChatColor.YELLOW + "/guild list" + ChatColor.AQUA + " lists all public guilds.");
			sender.sendMessage(ChatColor.YELLOW + "/guild join <GuildName>" + ChatColor.AQUA + " requests to join a guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild info <GuildName>" + ChatColor.AQUA + " views information about a guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild invite <Player>" + ChatColor.AQUA + " invites a player to your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild pending" + ChatColor.AQUA + " view pending applications to your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild accept <Player>" + ChatColor.AQUA + " accepts a player to your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild reject <Player>" + ChatColor.AQUA + " rejects a player from your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild kick <Player>" + ChatColor.AQUA + " kicks a player from your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild [un]ban <Player>" + ChatColor.AQUA + " (un)bans a player from your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild access <ALL|REQUEST|INVITE|UNLISTED>" + ChatColor.AQUA + " sets your guild access level.");
			sender.sendMessage(ChatColor.YELLOW + "/guild broadcast <Message>" + ChatColor.AQUA + " broadcasts to all your guild members.");
			sender.sendMessage(ChatColor.YELLOW + "/guild setowner <Player>" + ChatColor.AQUA + " set a new owner of your guild.");
			sender.sendMessage(ChatColor.YELLOW + "/guild leave [GuildName]" + ChatColor.AQUA + " leave your current guild.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds:");
			for(Guild guild : guildLoader.getAllGuilds()) {
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + guild.getName() + ChatColor.GRAY + " (" + guild.getMembers().size() + " members, " + guild.getXP() + " XP)");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("join")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild join <GuildName>");
				return true;
			}
			Guild guild = guildLoader.getGuildByName(args[1]);
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
				return true;
			}
			if(guild.getMembers().contains(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You already belong to this guild!");
				return true;
			}
			if(guild.getBlacklist().contains(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You are banned from this guild!");
				return true;
			}
			if(guild.getAccessLevel() == GuildAccessLevel.ALL) {
				guild.getMembers().add(player.getUniqueId());
				guild.save();
				// TODO notify guild members
				sender.sendMessage(ChatColor.GREEN + "Joined guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully!");
				return true;
			}
			else if(guild.getAccessLevel() == GuildAccessLevel.REQUEST) {
				guild.getPending().add(player.getUniqueId());
				guild.save();
				// TODO notify guild members
				sender.sendMessage(ChatColor.GREEN + "Requested to join guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + ".");
				return true;
			}
			else if(guild.getAccessLevel() == GuildAccessLevel.INVITE
					|| guild.getAccessLevel() == GuildAccessLevel.UNLISTED) {
				if(guild.getInvited().contains(player.getUniqueId())) {
					guild.getInvited().remove(player.getUniqueId());
					guild.getMembers().add(player.getUniqueId());
					guild.save();
					// TODO notify gulid members
					sender.sendMessage(ChatColor.GREEN + "Accepted invitation and joined guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully!");
					return true;
				}
				else {
					sender.sendMessage(ChatColor.RED + "This guild is invite-only!");
					return true;
				}
			}
			
		}
		
		if(args[0].equalsIgnoreCase("info")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild info <GuildName>");
				return true;
			}
			Guild guild = guildLoader.getGuildByName(args[1]);
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
				return true;
			}
			if(guild.getAccessLevel() == GuildAccessLevel.UNLISTED && !guild.getMembers().contains(player.getUniqueId())) {
				sender.sendMessage(ChatColor.RED + "This guild is unlisted!");
				return true;
			}
			sender.sendMessage(ChatColor.YELLOW + "Information for guild " + ChatColor.GOLD + guild.getName());
			sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + guild.getDescription());
			sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.WHITE + guild.getXP()
				+ ChatColor.GRAY + "  -  Members: " + ChatColor.WHITE + guild.getMembers().size());
			sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.WHITE + guild.getAccessLevel().friendlyName());
			sender.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + userLoader.loadObject(guild.getOwner()).getName());
			sender.sendMessage(ChatColor.GRAY + "Members: " + ChatColor.WHITE 
					+ StringUtil.parseList(guild.getMembers().stream().map(uuid -> userLoader.loadObject(uuid).getName()).collect(Collectors.toList())));
			return true;
		}
		
		
		if(args[0].equalsIgnoreCase("invite")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to any guilds!");
				return true;
			}
			Guild inviting = guilds.get(0);
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild invite <Player>");
				return true;
			}
			if(guilds.size() > 1) {
				if(args.length > 2) {
					inviting = guildLoader.getGuildByName(args[2]);
					if(inviting == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select one guild to invite this player to.");
					sender.sendMessage(ChatColor.RED + "/guild invite <Player> <GuildName>");
					return true;
				}
			}
			User invited = userLoader.loadObject(args[1]);
			if(invited == null) {
				sender.sendMessage(ChatColor.RED + "No player by that name exists in our records!");
				return true;
			}
			if(inviting.getInvited().contains(invited.getUUID())) {
				sender.sendMessage(ChatColor.RED + "This player is already on this guild's invite list!");
				return true;
			}
			if(inviting.getMembers().contains(invited.getUUID())) {
				sender.sendMessage(ChatColor.RED + "This player is already a member of this guild!");
				return true;
			}
			if(invited.getUUID().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You can't invite yourself!");
				return true;
			}
			inviting.getInvited().add(invited.getUUID());
			inviting.save();
			if(invited.getPlayer() != null) {
				invited.getPlayer().sendMessage(ChatColor.GREEN + "You were invited to join the guild " + ChatColor.AQUA + invited.getName() + ChatColor.GREEN + "!");
				invited.getPlayer().sendMessage(ChatColor.YELLOW + "/guild join " + inviting.getName() + ChatColor.GREEN + " to join!");
			}
			sender.sendMessage(ChatColor.GREEN + "Invited " + ChatColor.AQUA + invited.getName() + ChatColor.GREEN + " to join " + ChatColor.AQUA + inviting.getName());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("pending")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to any guilds!");
				return true;
			}
			Guild query = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 1) {
					query = guildLoader.getGuildByName(args[1]);
					if(query == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
					if(!guilds.contains(query)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to that guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select one guild to query.");
					sender.sendMessage(ChatColor.RED + "/guild pending <GuildName>");
					return true;
				}
			}
			if(query.getPending().size() == 0) {
				sender.sendMessage(ChatColor.RED + "This guild has no pending applications!");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "There are " + query.getPending().size() + " applications to your guild:");
			for(UUID pending : query.getPending()) {
				User applicant = userLoader.loadObject(pending);
				sender.sendMessage(ChatColor.GRAY + "- " + applicant.getName() + " [Lv " + applicant.getLevel() + "] [" + applicant.getRank().getShortName() + "]");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("reject") || args[0].equalsIgnoreCase("kick")
				|| args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to any guilds!");
				return true;
			}
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild " + args[0].toLowerCase() + " <Player>");
				return true;
			}
			User target = userLoader.loadObject(args[1]);
			if(target == null) {
				sender.sendMessage(ChatColor.RED + "No user by that name exists in our records!");
				return true;
			}
			List<Guild> shared = new ArrayList<>();
			if(args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("reject")) {
				shared = guilds.stream().filter(g -> g.getPending().contains(target.getUUID())).collect(Collectors.toList());
			} 
			else if(args[0].equalsIgnoreCase("unban")) {
				shared = guilds.stream().filter(g -> g.getBlacklist().contains(target.getUUID())).collect(Collectors.toList());
			}
			else {
				shared = guilds.stream().filter(g -> g.getMembers().contains(target.getUUID())).collect(Collectors.toList());
			}
			if(shared.size() == 0) {
				sender.sendMessage(ChatColor.RED + "Invalid user for the specified guild!");
				return true;
			}
			Guild guild = shared.get(0);
			if(shared.size() > 1) {
				if(args.length > 2) {
					boolean found = false;
					for(Guild test : shared) {
						if(test.getName().equalsIgnoreCase(args[2])) {
							guild = test;
							found = true;
							break;
						}
					}
					if(!found) {
						sender.sendMessage(ChatColor.RED + "You do not share this guild with " + target.getName() + "!");
						sender.sendMessage(ChatColor.RED + "/guild " + args[0].toLowerCase() + " " + target.getName() 
							+ " <" + StringUtil.parseList(shared.stream().map(g -> g.getName()).collect(Collectors.toList()), "|") + ">");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You share multiple guilds with " + target.getName() + "!");
					sender.sendMessage(ChatColor.RED + "/guild " + args[0].toLowerCase() + " " + target.getName() 
						+ " <" + StringUtil.parseList(shared.stream().map(g -> g.getName()).collect(Collectors.toList()), "|") + ">");
					return true;
				}
			}
			if(!guild.getOwner().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You must be the owner of the guild to perform this action!");
				return true; // TODO more fine-grained permissions checking with internal ranks, etc.
			}
			if(args[0].equalsIgnoreCase("accept")) {
				guild.getPending().remove(target.getUUID());
				guild.getMembers().add(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Accepted " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " into " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.GREEN + "You were accepted into the guild " + ChatColor.AQUA + guild.getName());
				}
				// TODO notify
				return true;
			}
			if(args[0].equalsIgnoreCase("reject")) {
				guild.getPending().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Rejected " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were rejected from the guild " + ChatColor.YELLOW + guild.getName());
				}
				return true;
			}
			if(args[0].equalsIgnoreCase("kick")) {
				if(guild.getOwner().equals(target.getUUID())) {
					sender.sendMessage(ChatColor.RED + "You can't kick the guild owner!");
					return true;
				}
				guild.getMembers().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Kicked " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were kicked from the guild " + ChatColor.YELLOW + guild.getName());
				}
				return true;
			}
			if(args[0].equalsIgnoreCase("ban")) {
				if(guild.getOwner().equals(target.getUUID())) {
					sender.sendMessage(ChatColor.RED + "You can't ban the guild owner!");
					return true;
				}
				guild.getMembers().remove(target.getUUID());
				guild.getBlacklist().add(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Banned " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were banned from the guild " + ChatColor.YELLOW + guild.getName());
				}
				return true;
			}
			if(args[0].equalsIgnoreCase("unban")) {
				guild.getBlacklist().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Unbanned " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.GREEN + "You were unbanned from the guild " + ChatColor.AQUA + guild.getName());
				}
				return true;
			}
		}
		
		if(args[0].equalsIgnoreCase("access")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			if(args.length == 1) {
				for(Guild guild : guilds) {
					sender.sendMessage(ChatColor.GREEN + "Access level of " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " is " + ChatColor.AQUA + guild.getAccessLevel().friendlyName());
				}
				return true;
			}
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 2) {
					guild = guildLoader.getGuildByName(args[2]);
					if(guild == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
					if(!guilds.contains(guild)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to this guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select one guild to change its access level.");
					sender.sendMessage(ChatColor.RED + "/guild access <" + StringUtil.parseList(GuildAccessLevel.values(), "|") + "> <GuildName>");
					return true;
				}
			}
			if(!guild.getOwner().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You must be the owner of this guild to change its access level!");
				return true;
			}
			GuildAccessLevel level = StringUtil.parseEnum(sender, GuildAccessLevel.class, args[1]);
			if(level == null) return true;
			guild.setAccessLevel(level);
			sender.sendMessage(ChatColor.GREEN + "Updated guild access level to " + ChatColor.AQUA + level.friendlyName());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc") || args[0].equalsIgnoreCase("chat")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild broadcast <Message>");
				return true;
			}
			String message = StringUtil.concatArgs(args, 1);
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 3) {
					guild = guildLoader.getGuildByName(args[1]);
					message = StringUtil.concatArgs(args, 2);
					if(guild == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
					if(!guilds.contains(guild)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to that guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select a guild to broadcast to.");
					sender.sendMessage(ChatColor.RED + "/guild broadcast <GuildName> <Message>");
					return true;
				}
			}
			String formatted = ChatColor.GRAY + "@" + guild.getName() + " " 
					+ ChatColor.DARK_GREEN + "" + ChatColor.BOLD + user.getName() + " "
					+ ChatColor.GREEN + message;
			for(UUID uuid : guild.getMembers()) {
				User member = userLoader.loadObject(uuid);
				if(member.getPlayer() != null) {
					member.getPlayer().sendMessage(formatted);
				}
			}
			sender.sendMessage(formatted);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("setowner")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 1) {
					guild = guildLoader.getGuildByName(args[2]);
					if(guild == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
					if(!guilds.contains(guild)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to that guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select a guild to transfer ownership of.");
					sender.sendMessage(ChatColor.RED + "/guild setowner <Player> <GuildName>");
					return true;
				}
			}
			if(!guild.getOwner().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You're not the current owner of this guild!");
				return true;
			}
			User target = userLoader.loadObject(args[1]);
			if(target == null) {
				sender.sendMessage(ChatColor.RED + "That player was not found in our records!");
				return true;
			}
			if(target.getUUID().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You are already the owner of this guild!");
				return true;
			}
			if(!guild.getMembers().contains(target.getUUID())) {
				sender.sendMessage(ChatColor.RED + "That player is not a member of this guild!");
				return true;
			}
			guild.setOwner(target.getUUID());
			sender.sendMessage(ChatColor.GREEN + "Transferred ownership of " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " to " + ChatColor.AQUA + target.getName());
			if(user.getPlayer() != null) {
				user.getPlayer().sendMessage(ChatColor.GREEN + "You are now the owner of " + ChatColor.AQUA + guild.getName());
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("leave")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 1) {
					guild = guildLoader.getGuildByName(args[1]);
					if(guild == null) {
						sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
						return true;
					}
					if(!guilds.contains(guild)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to that guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select a guild to leave.");
					sender.sendMessage(ChatColor.RED + "/guild leave <GuildName>");
					return true;
				}
			}
			if(guild.getOwner().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You are the owner of this guild! If you want to leave, you must delegate a new owner first.");
				sender.sendMessage(ChatColor.RED + "/guild setowner <Player>");
				return true;
			}
			guild.getMembers().remove(user.getUUID());
			guild.save();
			sender.sendMessage(ChatColor.GREEN + "Left guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully.");
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid usage! Do /guild for help");
		return true;
	}

}
