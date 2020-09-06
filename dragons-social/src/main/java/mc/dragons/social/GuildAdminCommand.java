package mc.dragons.social;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.GuildLoader.Guild;

public class GuildAdminCommand implements CommandExecutor {

	private UserLoader userLoader;
	private GuildLoader guildLoader;
	
	public GuildAdminCommand() {
		userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
		guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin new <Owner> <GuildName>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin list");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin info <GuildID>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin members <GuildID>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin of <Player>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin setxp <GuildID> <XP>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin addxp <GuildID> <XP>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin desc <GuildID> <Description>");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin forcejoin <GuildID> [Player=you]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin forceleave <GuildID> [Player=you]");
			sender.sendMessage(ChatColor.YELLOW + "/guildadmin delete <GuildID>");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("new")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin new <Owner> <GuildName>");
				return true;
			}
			User owner = userLoader.loadObject(args[1]);
			if(owner == null) {
				sender.sendMessage(ChatColor.RED + "Invalid user!");
				return true;
			}
			String guildName = StringUtil.concatArgs(args, 2);
			Guild guild = guildLoader.addGuild(owner.getUUID(), guildName);
			sender.sendMessage(ChatColor.GREEN + "Created guild #" + guild.getId() + ": " + guildName);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds:");
			for(Guild guild : guildLoader.getAllGuilds()) {
				sender.sendMessage(ChatColor.GRAY + "- #" + guild.getId() + ": " + guild.getName() + " (owner: " + userLoader.loadObject(guild.getOwner()).getName() + ")");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("info")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin info <GuildID>");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Guild #" + guild.getId());
			sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.RESET + guild.getName());
			sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.RESET + guild.getDescription());
			sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.RESET + guild.getXP());
			sender.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.RESET + userLoader.loadObject(guild.getOwner()).getName());
			sender.sendMessage(ChatColor.GRAY + "# Members: " + ChatColor.RESET + guild.getMembers().size());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("of")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin of <Player>");
				return true;
			}
			User test = userLoader.loadObject(args[1]);
			if(test == null) {
				sender.sendMessage(ChatColor.RED + "Invalid user!");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Listing all guilds that " + args[1] + " is in:");
			for(Guild guild : guildLoader.getAllGuildsWith(test.getUUID())) {
				sender.sendMessage(ChatColor.GRAY + "- #" + guild.getId() + ": " + guild.getName() + " (owner: " + userLoader.loadObject(guild.getOwner()).getName() + ")");
			}
			return true;
		}
		
		if(args[0].equalsIgnoreCase("members")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin members <GuildID>");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Guild " + guild.getName() + " has " + (guild.getMembers().size() + 1) + " members:"
					+ ChatColor.GOLD + userLoader.loadObject(guild.getOwner()).getName() + " (Owner)" + ChatColor.GRAY + ", " + 
					StringUtil.parseList(guild.getMembers().stream().map(uuid -> userLoader.loadObject(uuid).getName()).collect(Collectors.toList())));
			return true;
		}
		
		if(args[0].equalsIgnoreCase("setxp")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin setxp <GuildID> <XP>");
				return true;
			}			
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			guild.setXP(Integer.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Set XP of guild " + guild.getName() + " to " + guild.getXP());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("addxp")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin addxp <GuildID> <XP>");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			guild.addXP(Integer.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Added " + args[2] + " XP to guild " + guild.getName() + " (total XP: " + guild.getXP() + ")");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("desc")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin desc <GuildID> <Description>");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			String desc = StringUtil.concatArgs(args, 2);
			guild.setDescription(desc);
			sender.sendMessage(ChatColor.GREEN + "Set description of guild " + guild.getName() + " to " + desc);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("forcejoin")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin forcejoin <GuildID> [Player=you]");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			User joiningUser = user;
			UUID joining = user.getUUID();
			if(args.length >= 3) {
				joiningUser = userLoader.loadObject(args[2]);
				if(joiningUser == null) {
					sender.sendMessage(ChatColor.RED + "Invalid user!");
					return true;
				}
				joining = joiningUser.getUUID();
			}
			guild.getMembers().add(joining);
			guild.save();
			sender.sendMessage(ChatColor.GREEN + "Added user " + joiningUser.getName() + " to guild " + guild.getName());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("forceleave")) {
			if(args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin forceleave <GuildID> [Player=you]");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			User joiningUser = user;
			UUID joining = user.getUUID();
			if(args.length >= 3) {
				joiningUser = userLoader.loadObject(args[2]);
				if(joiningUser == null) {
					sender.sendMessage(ChatColor.RED + "Invalid user!");
					return true;
				}
				joining = joiningUser.getUUID();
			}
			guild.getMembers().remove(joining);
			guild.save();
			sender.sendMessage(ChatColor.GREEN + "Removed user " + joiningUser.getName() + " from guild " + guild.getName());
			return true;
		}
		
		if(args[0].equalsIgnoreCase("delete")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Usage: /guildadmin delete <GuildID>");
				return true;
			}
			Guild guild = guildLoader.getGuildById(Integer.valueOf(args[1]));
			if(guild == null) {
				sender.sendMessage(ChatColor.RED + "Invalid guild ID!");
				return true;
			}
			guildLoader.deleteGuild(guild.getId());
			sender.sendMessage(ChatColor.GREEN + "Deleted guild " + guild.getName() + " successfully.");
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid sub-command! /guildadmin");
		return true;
	}
	
}
