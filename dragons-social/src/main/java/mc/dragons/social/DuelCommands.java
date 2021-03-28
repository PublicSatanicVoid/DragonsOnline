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
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.loader.WarpLoader;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.EntityHider.Policy;

public class DuelCommands extends DragonsCommandExecutor {

	private static final String DUEL_ARENA_WARP = "duelarena1";
	public static final String DUEL_MESSAGE_HEADER = ChatColor.DARK_GREEN + "--------------------------------------------------------------------";

	private static Map<User, User> requests = new HashMap<>();
	private static Map<User, Location> restore = new HashMap<>();
	private static List<Set<User>> active = new ArrayList<>();
	
	private static EntityHider entityHider = Dragons.getInstance().getEntityHider();
	private static WarpLoader warpLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(WarpLoader.class);
	
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
		if(!requirePlayer(sender)) return true;
		
		if(label.equalsIgnoreCase("duel")) {
			duelCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("listallduelstatus")) {
			if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
			World duelWorld = warpLoader.getWarp(DUEL_ARENA_WARP).getWorld();
			sender.sendMessage(ChatColor.DARK_GREEN + "Outstanding Duel Requests:");
			requests.forEach((a, b) -> {
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + a.getName() + ChatColor.GRAY + " -> " + ChatColor.YELLOW + b.getName());
			});
			sender.sendMessage(ChatColor.DARK_GREEN + "Active Duels:");
			active.forEach(users -> {
				sender.sendMessage(ChatColor.GRAY + "- " + users.stream().map(u -> ChatColor.YELLOW + u.getName()).reduce((a, b) -> a + ChatColor.GRAY + ", " + b).get());
				for(User user : users) {
					if(!user.getPlayer().getWorld().equals(duelWorld)) {
						sender.sendMessage(ChatColor.RED + "   Error: " + user.getName() + " is in the wrong world");
					}
				}
			});
		}
		else if(label.equalsIgnoreCase("testduelwin")) {
			if(!requirePermission(sender, PermissionLevel.ADMIN)) return true;
			JavaPlugin.getPlugin(DragonsSocialPlugin.class).getSocialHook().onDeath(user(sender));
		}
		
		return true;
	}

	private void duelCommand(CommandSender sender, String[] args) {

		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/duel <Player>");
			if(hasPermission(sender, PermissionLevel.ADMIN)) {
				sender.sendMessage(ChatColor.RED + "/duel " + sender.getName() + " --force-allow-self-duel");
			}
			return;
		}
		
		Player player = player(sender);
		User user = user(sender);
		Player target = lookupPlayer(sender, args[0]);
		if(target == null) return;
		
		User userTarget = UserLoader.fromPlayer(target);
		
		// Since I'm lonely and don't have any alt accounts, gotta make a bypass so the duel system can be tested with one player only.
		boolean selfDuel = false;
		if(user.equals(userTarget)) {
			selfDuel = true;
			if(!hasPermission(sender, PermissionLevel.ADMIN) || !(args.length > 1 && args[1].equalsIgnoreCase("--force-allow-self-duel"))) {
				sender.sendMessage(ChatColor.RED + "You can't duel yourself!");
				return;
			}
		}
		
		if(requests.containsKey(user) && !selfDuel) { // Overwriting a request
			if(requests.containsValue(userTarget)) {
				sender.sendMessage(ChatColor.RED + "Your request to " + userTarget.getName() + " is still pending!");
				return;
			}
			requests.get(user).getPlayer().sendMessage(ChatColor.RED + user.getName() + " cancelled their duel request.");
			requests.put(user, userTarget);
			sender.sendMessage(ChatColor.GREEN + "Cancelled your previous duel request and sent a new request to " + userTarget.getName());
			
			target.sendMessage(DUEL_MESSAGE_HEADER);
			target.sendMessage(ChatColor.GREEN + user.getName() + " sent you a duel request! Do " + ChatColor.YELLOW + "/duel " + user.getName() + ChatColor.GREEN + " to accept it!");
			target.sendMessage(DUEL_MESSAGE_HEADER);
		}
		
		else if(requests.containsValue(user) && requests.get(userTarget).equals(user)) { // Accepting a request
			target.sendMessage(DUEL_MESSAGE_HEADER);
			target.sendMessage(ChatColor.GREEN + user.getName() + " accepted your request! Teleporting to arena...");
			target.sendMessage(DUEL_MESSAGE_HEADER);
			
			player.sendMessage(DUEL_MESSAGE_HEADER);
			player.sendMessage(ChatColor.GREEN + "Accepted " + user.getName() + "'s request! Teleporting to arena...");
			player.sendMessage(DUEL_MESSAGE_HEADER);
			
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

			target.sendMessage(DUEL_MESSAGE_HEADER);
			target.sendMessage(ChatColor.GREEN + user.getName() + " sent you a duel request! Do " + ChatColor.YELLOW + "/duel " + user.getName() + ChatColor.GREEN + " to accept it!");
			target.sendMessage(DUEL_MESSAGE_HEADER);
		}
	}
	
}
