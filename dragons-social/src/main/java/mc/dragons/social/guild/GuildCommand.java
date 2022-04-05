package mc.dragons.social.guild;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.guild.GuildLoader.Guild;
import mc.dragons.social.guild.GuildLoader.GuildAccessLevel;
import mc.dragons.social.guild.GuildLoader.GuildEvent;
import mc.dragons.social.guild.GuildLoader.GuildThemeColor;

public class GuildCommand extends DragonsCommandExecutor {

	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		Player player = player(sender);
		User user = user(sender);
		
		List<Guild> guilds = guildLoader.getAllGuildsWithRaw(user.getUUID());
		
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
			sender.sendMessage(ChatColor.YELLOW + "/guild motd <Message of the day|REMOVE>" + ChatColor.AQUA + " sets the guild's MOTD.");
			sender.sendMessage(ChatColor.YELLOW + "/guild themecolor <GRAY|GREEN|BLUE|GOLD|RED|PURPLE>" + ChatColor.AQUA + " sets the guild's theme color.");
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
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			Integer page = 1;
			if(args.length > 1) {
				page = parseInt(sender, args[1]);
				if(page == null) return true;
			}
			PaginatedResult<Guild> results = guildLoader.getAllGuilds(page);
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds (Page " + page + " of " + results.getPages() + ", " + results.getTotal() + " total)");
			for(Guild guild : results.getPage()) {
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + guild.getName() + ChatColor.GRAY + " (" + guild.getMembers().size() + " members, " + guild.getXP() + " XP)");
			}
		}
		
		else if(args[0].equalsIgnoreCase("join")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild join <GuildName>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
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
				sender.sendMessage(ChatColor.GREEN + "Joined guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully!");
				guild.update(GuildEvent.JOIN, user);
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
					sender.sendMessage(ChatColor.GREEN + "Accepted invitation and joined guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully!");
					guild.update(GuildEvent.JOIN, user);
					user.updateListName();
				}
				else {
					sender.sendMessage(ChatColor.RED + "This guild is invite-only!");
				}
			}
		}
		
		else if(args[0].equalsIgnoreCase("info")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild info <GuildName>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			if(guild.getAccessLevel() == GuildAccessLevel.UNLISTED && !guild.getMembers().contains(player.getUniqueId())) {
				sender.sendMessage(ChatColor.RED + "This guild is unlisted!");
				return true;
			}
			sender.sendMessage(guild.getThemeColor().secondary() + "Information for guild " + guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName());
			sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + guild.getDescription());
			sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.WHITE + guild.getXP()
				+ ChatColor.GRAY + "  -  Members: " + ChatColor.WHITE + guild.getMembers().size());
			sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.WHITE + guild.getAccessLevel().friendlyName());
			sender.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.WHITE + userLoader.loadObject(guild.getOwner()).getName());
			sender.sendMessage(ChatColor.GRAY + "Members: (" + guild.getMembers().size() + ") " + ChatColor.WHITE 
					+ StringUtil.parseList(guild.getMembers().stream().map(uuid -> userLoader.loadObject(uuid).getName()).collect(Collectors.toList())));
		}
		
		else if(args[0].equalsIgnoreCase("motd") || args[0].equalsIgnoreCase("themecolor") || args[0].equalsIgnoreCase("access")) {
			String setting;
			switch(args[0].toLowerCase()) {
			case "motd":
				setting = "Message of the day";
				break;
			case "themecolor":
				setting = "Theme color";
				break;
			case "access":
				setting = "Access level";
				break;
			default:
				setting = "[Error parsing option parameter]";
				break;
			}
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			if(args.length == 1) {
				for(Guild guild : guilds) {
					if(setting.equalsIgnoreCase("Message of the day")) {
						sender.sendMessage(guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName() + ": " + guild.getThemeColor().secondary() + guild.getMOTD());
					}
					else if(setting.equalsIgnoreCase("Theme color")) {
						sender.sendMessage(guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName() + ": " + guild.getThemeColor().secondary() + guild.getThemeColor().toString());
					}
					else if(setting.equalsIgnoreCase("Access level")) {
						sender.sendMessage(guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName() + ": " + guild.getThemeColor().secondary() + guild.getAccessLevel().friendlyName());
					}
				}
				return true;
			}
			List<Guild> ownedGuilds = guilds.stream().filter(g -> g.getOwner().equals(user.getUUID())).collect(Collectors.toList());
			Guild target = null;
			if(ownedGuilds.size() == 1) {
				target = ownedGuilds.get(0);
			}
			else {
				if(args.length > 2) {
					target = lookupGuild(sender, args[1]);
					if(target == null) return true;
					if(!guilds.contains(target)) {
						sender.sendMessage(ChatColor.RED + "You don't belong to that guild!");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "You own multiple guilds! Select one guild to set its " + setting + ".");
					sender.sendMessage(ChatColor.RED + "/guild motd <" + StringUtil.parseList(ownedGuilds.stream().map(g -> g.getName()).collect(Collectors.toList()), "|") + ">"
							+ " <" + setting + ">");
					return true;
				}
			}
			if(!target.getOwner().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You must be the guild owner to set the " + setting + "!");
				return true;
			}
			int motdIndex = ownedGuilds.size() == 1 ? 1 : 2;
			String value = StringUtil.concatArgs(args, motdIndex);
			if(setting.equalsIgnoreCase("Message of the day")) {
				if(value.equalsIgnoreCase("REMOVE")) {
					target.setMOTD("");
					sender.sendMessage(ChatColor.GREEN + "Removed guild MOTD successfully.");
					return true;
				}
				target.setMOTD(value);
				sender.sendMessage(ChatColor.GREEN + "Updated guild MOTD successfully. Members will see this upon joining.");
			}
			else if(setting.equalsIgnoreCase("Theme color")) {
				GuildThemeColor themeColor = StringUtil.parseEnum(sender, GuildThemeColor.class, value);
				if(themeColor == null) {
					return true;
				}
				if(target.getXP() < themeColor.xpreq()) {
					sender.sendMessage(ChatColor.RED + "Your guild must have at least " + themeColor.xpreq() + " XP to unlock this theme color! (Currently have " + target.getXP() + ")");
					return true;
				}
				target.setThemeColor(themeColor);
				sender.sendMessage(ChatColor.GREEN + "Updated guild theme color successfully.");
				sender.sendMessage(ChatColor.GRAY + "Primary Color: " + themeColor.primary() + "" + ChatColor.BOLD + themeColor.primary().name());
				sender.sendMessage(ChatColor.GRAY + "Secondary Color: " + themeColor.secondary() + themeColor.secondary().name());
				sender.sendMessage(ChatColor.GRAY + "Tab List Tag: " + themeColor.tag() + "[" + target.getName() + "]");
				return true;
			}
			else if(setting.equalsIgnoreCase("Access level")) {
				GuildAccessLevel level = StringUtil.parseEnum(sender, GuildAccessLevel.class, value);
				if(level == null) {
					return true;
				}
				target.setAccessLevel(level);
				sender.sendMessage(ChatColor.GREEN + "Updated guild access level to " + ChatColor.AQUA + level.friendlyName());
				return true;
			}
		}
		
		
		else if(args[0].equalsIgnoreCase("invite")) {
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
					inviting = lookupGuild(sender, args[2]);
					if(inviting == null) return true;
				}
				else {
					sender.sendMessage(ChatColor.RED + "You belong to multiple guilds! Select one guild to invite this player to.");
					sender.sendMessage(ChatColor.RED + "/guild invite <Player> <GuildName>");
					return true;
				}
			}
			User invited = lookupUser(sender, args[1]);
			if(invited == null) return true;
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
				invited.getPlayer().sendMessage(ChatColor.GREEN + "You were invited to join the guild " + ChatColor.AQUA + inviting.getName() + ChatColor.GREEN + "!");
				invited.getPlayer().sendMessage(ChatColor.YELLOW + "/guild join " + inviting.getName() + ChatColor.GREEN + " to join!");
			}
			sender.sendMessage(ChatColor.GREEN + "Invited " + ChatColor.AQUA + invited.getName() + ChatColor.GREEN + " to join " + ChatColor.AQUA + inviting.getName());
		}
		
		else if(args[0].equalsIgnoreCase("pending")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to any guilds!");
				return true;
			}
			Guild query = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 1) {
					query = lookupGuild(sender, args[1]);
					if(query == null) return true;
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
		}
		
		else if(args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("reject") || args[0].equalsIgnoreCase("kick")
				|| args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to any guilds!");
				return true;
			}
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "/guild " + args[0].toLowerCase() + " <Player>");
				return true;
			}
			User target = lookupUser(sender, args[1]);
			if(target == null) return true;
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
			else if(args[0].equalsIgnoreCase("accept")) {
				guild.getPending().remove(target.getUUID());
				guild.getMembers().add(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Accepted " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " into " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.GREEN + "You were accepted into the guild " + ChatColor.AQUA + guild.getName());
				}
				guild.update(GuildEvent.JOIN, target);
				target.updateListName();
			}
			else if(args[0].equalsIgnoreCase("reject")) {
				guild.getPending().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Rejected " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were rejected from the guild " + ChatColor.YELLOW + guild.getName());
				}
			}
			else if(args[0].equalsIgnoreCase("kick")) {
				if(guild.getOwner().equals(target.getUUID())) {
					sender.sendMessage(ChatColor.RED + "You can't kick the guild owner!");
					return true;
				}
				guild.update(GuildEvent.KICK, target);
				guild.getMembers().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Kicked " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were kicked from the guild " + ChatColor.YELLOW + guild.getName());
				}
				target.updateListName();
				target.updatePrimaryNameTag(true);
			}
			else if(args[0].equalsIgnoreCase("ban")) {
				if(guild.getOwner().equals(target.getUUID())) {
					sender.sendMessage(ChatColor.RED + "You can't ban the guild owner!");
					return true;
				}
				guild.update(GuildEvent.BAN, target);
				guild.getMembers().remove(target.getUUID());
				guild.getBlacklist().add(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Banned " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.RED + "You were banned from the guild " + ChatColor.YELLOW + guild.getName());
				}
				target.updateListName();
				target.updatePrimaryNameTag(true);
			}
			else if(args[0].equalsIgnoreCase("unban")) {
				guild.getBlacklist().remove(target.getUUID());
				guild.save();
				sender.sendMessage(ChatColor.GREEN + "Unbanned " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " from " + ChatColor.AQUA + guild.getName());
				if(target.getPlayer() != null) {
					target.getPlayer().sendMessage(ChatColor.GREEN + "You were unbanned from the guild " + ChatColor.AQUA + guild.getName());
				}
				guild.update(GuildEvent.UNBAN, target);
			}
		}
		
		else if(args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc") || args[0].equalsIgnoreCase("chat")) {
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
				if(args.length > 2) {
					guild = lookupGuild(sender, args[1]);
					if(guild == null) return true;
					message = StringUtil.concatArgs(args, 2);
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
					+ guild.getThemeColor().primary() + "" + ChatColor.BOLD + user.getName() + " "
					+ guild.getThemeColor().secondary() + message;
			GuildLoader.getGuildMessageHandler().send(guild.getName(), formatted);
		}
		
		else if(args[0].equalsIgnoreCase("setowner")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 2) {
					guild = lookupGuild(sender, args[2]);
					if(guild == null) return true;
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
			User target = lookupUser(sender, args[1]);
			if(target == null) return true;
			if(target.getUUID().equals(user.getUUID())) {
				sender.sendMessage(ChatColor.RED + "You are already the owner of this guild!");
				return true;
			}
			if(!guild.getMembers().contains(target.getUUID())) {
				sender.sendMessage(ChatColor.RED + "That player is not a member of this guild!");
				return true;
			}
			guild.getMembers().add(guild.getOwner());
			guild.setOwner(target.getUUID());
			guild.getMembers().remove(target.getUUID());
			sender.sendMessage(ChatColor.GREEN + "Transferred ownership of " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " to " + ChatColor.AQUA + target.getName());
			if(target.getPlayer() != null) {
				target.getPlayer().sendMessage(ChatColor.GREEN + "You are now the owner of " + ChatColor.AQUA + guild.getName());
			}
			guild.update(GuildEvent.TRANSFER_OWNERSHIP, target);
			user.updateListName();
			target.updateListName();
			user.updatePrimaryNameTag(true);
			target.updatePrimaryNameTag(true);
		}
		
		else if(args[0].equalsIgnoreCase("leave")) {
			if(guilds.size() == 0) {
				sender.sendMessage(ChatColor.RED + "You don't belong to a guild!");
				return true;
			}
			Guild guild = guilds.get(0);
			if(guilds.size() > 1) {
				if(args.length > 1) {
					guild = lookupGuild(sender, args[1]);
					if(guild == null) return true;
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
			guild.update(GuildEvent.LEAVE, user);
			guild.getMembers().remove(user.getUUID());
			guild.save();
			sender.sendMessage(ChatColor.GREEN + "Left guild " + ChatColor.AQUA + guild.getName() + ChatColor.GREEN + " successfully.");
			user.updateListName();
			user.updatePrimaryNameTag(true);
			return true;
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid usage! Do /guild for help");
		}
		
		return true;
	}
	
	private Guild lookupGuild(CommandSender sender, String guildName) {
		Guild guild = guildLoader.getGuildByName(guildName);
		if(guild == null) {
			sender.sendMessage(ChatColor.RED + "No guild by that name exists!");
			return null;
		}
		return guild;
	}

}
