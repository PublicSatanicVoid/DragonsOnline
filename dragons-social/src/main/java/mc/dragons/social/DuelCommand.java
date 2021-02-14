package mc.dragons.social;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.EntityHider.Policy;

public class DuelCommand implements CommandExecutor {

	private static final String DUEL_ARENA_WARP = "duelarena1";
	public static final String DUEL_MESSAGE_HEADER = ChatColor.DARK_GREEN + "----------------------------------";

	private static Map<User, User> requests = new HashMap<>();
	private static Map<User, Location> restore = new HashMap<>();
	private static List<Set<User>> active = new ArrayList<>();
	
	private static EntityHider entityHider = Dragons.getInstance().getEntityHider();

	public static Map<User, User> getRequests() { return requests; }
	public static List<Set<User>> getActive() { return active; }
	
	public static void restore(User user) { 
		if(restore.get(user) != null) {
			user.getPlayer().teleport(restore.get(user));
			restore.remove(user);
		}
		entityHider.resetPolicy(user.getPlayer());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/duel <Player>");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		Player target = Bukkit.getPlayer(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That player is not online! /duel <Player>");
			return true;
		}
		User userTarget = UserLoader.fromPlayer(target);
		
		if(user.equals(userTarget)) {
			sender.sendMessage(ChatColor.RED + "You can't duel yourself!");
			return true;
		}
		
		if(requests.containsKey(user)) { // Overwriting a request
			if(requests.containsValue(userTarget)) {
				sender.sendMessage(ChatColor.RED + "Your request to " + userTarget.getName() + " is still pending!");
				return true;
			}
			requests.get(user).getPlayer().sendMessage(ChatColor.RED + user.getName() + " cancelled their duel request.");
			requests.put(user, userTarget);
			sender.sendMessage(ChatColor.GREEN + "Cancelled your previous duel request and sent a new request to " + userTarget.getName());
			target.sendMessage(ChatColor.GREEN + user.getName() + " sent you a duel request! Do " + ChatColor.YELLOW + "/duel " + user.getName() + ChatColor.GREEN + " to accept it!");
			return true;
		}
		else if(requests.containsValue(user) && requests.get(userTarget).equals(user)) { // Accepting a request
			target.sendMessage(ChatColor.GREEN + user.getName() + " accepted your request! Teleporting to arena...");
			player.sendMessage(ChatColor.GREEN + "Accepted " + user.getName() + "'s request! Teleporting to arena...");
			requests.remove(userTarget);
			Set<User> pair = new HashSet<>();
			pair.add(user);
			pair.add(userTarget);
			active.add(pair);
			Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {
				restore.put(user, player.getLocation());
				restore.put(userTarget, target.getLocation());
				entityHider.setPolicy(player, Policy.WHITELIST);
				entityHider.setPolicy(target, Policy.WHITELIST);
				entityHider.showEntity(player, target);
				entityHider.showEntity(target, player);
				entityHider.updateEntities(player);
				entityHider.updateEntities(target);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + DUEL_ARENA_WARP + " " + user.getName());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + DUEL_ARENA_WARP + " " + userTarget.getName());
				String[] startMessage = new String[] {
					" ",
					DUEL_MESSAGE_HEADER,
					ChatColor.GOLD + "" + ChatColor.BOLD + "1v1 Duel",
					ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " vs " + ChatColor.YELLOW + player.getName(),
					" ",
					ChatColor.GRAY + "" + ChatColor.ITALIC + "Fight to the death!",
					DUEL_MESSAGE_HEADER,
					" "
				};
				player.sendMessage(startMessage);
				target.sendMessage(startMessage);
			}, 20L);
		}
		else { // Sending a request
			requests.put(user, userTarget);
			sender.sendMessage(ChatColor.GREEN + "Sent a duel request to " + userTarget.getName());
			target.sendMessage(ChatColor.GREEN + user.getName() + " sent you a duel request! Do " + ChatColor.YELLOW + "/duel " + user.getName() + ChatColor.GREEN + " to accept it!");
			return true;
		}
		return true;
	}

}
