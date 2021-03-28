package mc.dragons.social;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.pagination.PaginatedResult;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.GuildLoader.Guild;

public class GuildAdminCommand extends DragonsCommandExecutor {

	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
		
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin new <Owner> <GuildName>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin list [Page#]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin info <GuildID>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin members <GuildID>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin of <Player>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin setxp <GuildID> <XP>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin addxp <GuildID> <XP>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin desc <GuildID> <Description>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin forcejoin <GuildID> [Player=you]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin forceleave <GuildID> [Player=you]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin setowner <GuildID> [Player=you]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin delete <GuildID>");
		}
		
		else if(args[0].equalsIgnoreCase("new")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin new <Owner> <GuildName>");
				return true;
			}
			User owner = lookupUser(sender, args[1]);
			if(owner == null) return true;
			String guildName = StringUtil.concatArgs(args, 2);
			Guild guild = guildLoader.addGuild(owner.getUUID(), guildName);
			owner.updateListName();
			sender.sendMessage(ChatColor.GREEN + "Created guild #" + guild.getId() + ": " + guildName);
		}
		
		else if(args[0].equalsIgnoreCase("list")) {
			Integer page = 1;
			if(args.length > 1) {
				page = parseIntType(sender, args[1]);
				if(page == null) return true;
			}
			PaginatedResult<Guild> results = guildLoader.getAllGuilds(page);
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds (Page " + page + " of " + results.getPages() + ", " + results.getTotal() + " total)");
			for(Guild guild : results.getPage()) {
				sender.sendMessage(ChatColor.GRAY + "- #" + guild.getId() + ": " + guild.getName() + " (owner: " + userLoader.loadObject(guild.getOwner()).getName() + ")");
			}
		}
		
		else if(args[0].equalsIgnoreCase("info")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin info <GuildID>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			sender.sendMessage(ChatColor.GREEN + "Guild #" + guild.getId());
			sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.RESET + guild.getName());
			sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.RESET + guild.getDescription());
			sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.RESET + guild.getXP());
			sender.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.RESET + userLoader.loadObject(guild.getOwner()).getName());
			sender.sendMessage(ChatColor.GRAY + "# Members: " + ChatColor.RESET + guild.getMembers().size());
		}
		
		else if(args[0].equalsIgnoreCase("of")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin of <Player>");
				return true;
			}
			User test = lookupUser(sender, args[1]);
			if(test == null) return true;
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds that " + args[1] + " is in:");
			for(Guild guild : guildLoader.getAllGuildsWithRaw(test.getUUID())) {
				sender.sendMessage(ChatColor.GRAY + "- #" + guild.getId() + ": " + guild.getName() + " (owner: " + userLoader.loadObject(guild.getOwner()).getName() + ")");
			}
		}
		
		else if(args[0].equalsIgnoreCase("members")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin members <GuildID>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			sender.sendMessage(ChatColor.GREEN + "Guild " + guild.getName() + " has " + (guild.getMembers().size() + 1) + " members:"
					+ ChatColor.GOLD + userLoader.loadObject(guild.getOwner()).getName() + " (Owner)" + ChatColor.GRAY + ", " + 
					StringUtil.parseList(guild.getMembers().stream().map(uuid -> userLoader.loadObject(uuid).getName()).collect(Collectors.toList())));
		}
		
		else if(args[0].equalsIgnoreCase("setxp")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin setxp <GuildID> <XP>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			guild.setXP(Integer.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Set XP of guild " + guild.getName() + " to " + guild.getXP());
		}
		
		else if(args[0].equalsIgnoreCase("addxp")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin addxp <GuildID> <XP>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			guild.addXP(Integer.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Added " + args[2] + " XP to guild " + guild.getName() + " (total XP: " + guild.getXP() + ")");
		}
		
		else if(args[0].equalsIgnoreCase("desc")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin desc <GuildID> <Description>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			String desc = StringUtil.concatArgs(args, 2);
			guild.setDescription(desc);
			sender.sendMessage(ChatColor.GREEN + "Set description of guild " + guild.getName() + " to " + desc);
		}
		
		else if(args[0].equalsIgnoreCase("forcejoin")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin forcejoin <GuildID> [Player=you]");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			User joiningUser = user;
			UUID joining = user.getUUID();
			if(args.length >= 3) {
				joiningUser = lookupUser(sender, args[2]);
				if(joiningUser == null) return true;
				joining = joiningUser.getUUID();
			}
			guild.getMembers().add(joining);
			guild.save();
			joiningUser.updateListName();
			sender.sendMessage(ChatColor.GREEN + "Added user " + joiningUser.getName() + " to guild " + guild.getName());
		}
		
		else if(args[0].equalsIgnoreCase("forceleave")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin forceleave <GuildID> [Player=you]");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			User leavingUser = user;
			UUID leaving = user.getUUID();
			if(args.length >= 3) {
				leavingUser = lookupUser(sender, args[2]);
				if(leavingUser == null) return true;
				leaving = leavingUser.getUUID();
			}
			guild.getMembers().remove(leaving);
			guild.save();
			leavingUser.updateListName();
			sender.sendMessage(ChatColor.GREEN + "Removed user " + leavingUser.getName() + " from guild " + guild.getName());
		}
		
		else if(args[0].equalsIgnoreCase("setowner")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin setowner <GuildID> [Player=you]");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			User ownerUser = user;
			UUID owner = user.getUUID();
			if(args.length >= 3) {
				ownerUser = lookupUser(sender, args[2]);
				if(ownerUser == null) return true;
				owner = ownerUser.getUUID();
			}
			UUID formerOwner = guild.getOwner();
			guild.getMembers().add(guild.getOwner());
			guild.setOwner(owner);
			guild.getMembers().remove(owner);
			guild.save();
			userLoader.loadObject(formerOwner).updateListName();
			ownerUser.updateListName();
			sender.sendMessage(ChatColor.GREEN + "Set owner of guid " + guild.getName() + " to " + ownerUser.getName());
		}
		
		else if(args[0].equalsIgnoreCase("delete")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin delete <GuildID>");
				return true;
			}
			Guild guild = lookupGuild(sender, args[1]);
			if(guild == null) return true;
			guildLoader.deleteGuild(guild.getId());
			for(UUID uuid : guild.getMembers()) {
				userLoader.loadObject(uuid).updateListName();
			}
			sender.sendMessage(ChatColor.GREEN + "Deleted guild " + guild.getName() + " successfully.");
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid sub-command! /guildadmin");
		}
		return true;
	}

	private Guild lookupGuild(CommandSender sender, String idStr) {
		Integer id = parseIntType(sender, idStr);
		if(id == null) return null;
		Guild guild = guildLoader.getGuildById(id);
		if(guild == null) {
			sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
			return null;
		}
		return guild;
	}
	
}
