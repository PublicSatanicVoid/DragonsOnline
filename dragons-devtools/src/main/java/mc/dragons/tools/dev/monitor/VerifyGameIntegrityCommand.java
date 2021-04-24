package mc.dragons.tools.dev.monitor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class VerifyGameIntegrityCommand extends DragonsCommandExecutor {
	private GameObjectRegistry registry = dragons.getGameObjectRegistry();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return true;
		
		int errors = 0;
		int fixed = 0;
		
		boolean resolve = false;
		boolean silent = false;
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equalsIgnoreCase("-resolve")) {
				resolve = true;
			}
			if(args[i].equalsIgnoreCase("-silent")) {
				silent = true;
			}
		}
		
		sendMessageIfNotSilent(sender, silent, ChatColor.GREEN + "Verifying integrity of active game environment...");
		sendMessageIfNotSilent(sender, silent, ChatColor.GRAY + "- Validating user data:");
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.USER)) {
			User testUser = (User) gameObject;
			if(testUser.getPlayer() == null) {
				sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - User " + testUser.getName() + " does not correspond to a valid player");
				errors++;
				if(resolve) {
					userLoader.unregister(testUser);
					sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
					fixed++;
				}
			}
			if(testUser.getSystemProfile() == null) {
				if(testUser.getActivePermissionLevel() != PermissionLevel.USER) {
					sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - User " + testUser.getName() + " has an elevated active permission level without being logged in to a system profile");
					errors++;
					if(resolve) {
						testUser.setActivePermissionLevel(PermissionLevel.USER);
						sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
						fixed++;
					}
				}
			}
			else {
				if(testUser.getActivePermissionLevel().ordinal() > testUser.getSystemProfile().getMaxPermissionLevel().ordinal()) {
					sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - User " + testUser.getName() + " has an active permission level exceeding the maximum of their system profile");
					errors++;
					if(resolve) {
						testUser.setActivePermissionLevel(PermissionLevel.USER);
						sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
						fixed++;
					}
				}
			}
		}
		for(Player testPlayer : Bukkit.getOnlinePlayers()) {
			if(UserLoader.fromPlayer(testPlayer) == null) {
				sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - Player " + testPlayer.getName() + " does not correspond to a valid GameObject");
				errors++;
				if(resolve) {
					testPlayer.kickPlayer("An error was found with your account, please relog");
					sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
					fixed++;
				}
			}
			for(ItemStack itemStack : testPlayer.getInventory().getContents()) {
				if(ItemLoader.fromBukkit(itemStack) == null) {
					if(itemStack == null) continue;
					if(itemStack.getType() == null) continue;
					if(itemStack.getAmount() == 0) continue; // Is this possible?
					if(testPlayer.getGameMode() == GameMode.CREATIVE) continue;
					User testUser = UserLoader.fromPlayer(testPlayer);
					if(testUser.getActivePermissionLevel().ordinal() >= PermissionLevel.BUILDER.ordinal()) continue;
					sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - Player " + testPlayer.getName() + " has an unauthorized vanilla item [" + itemStack.getType().toString() + "]");
					errors++;
					if(resolve) {
						testPlayer.getInventory().remove(itemStack);
						testPlayer.sendMessage(ChatColor.RED + "A vanilla item was removed from your inventory.");
						sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
						fixed++;
					}
				}
			}
		}
		
		sendMessageIfNotSilent(sender, silent, ChatColor.GRAY + "- Validating entities:");
		for(World world : Bukkit.getWorlds()) {
			for(Entity entity : world.getEntities()) {
				if(entity.getPersistentDataContainer().has(Dragons.FIXED_ENTITY_KEY, PersistentDataType.INTEGER)) continue;
				if(entity instanceof Player) continue;
				if(entity instanceof ItemFrame) continue;
				if(entity instanceof Item) {
					if(ItemLoader.fromBukkit(((Item) entity).getItemStack()) == null) {
						sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - Entity #" + entity.getEntityId() + " [type " + entity.getType().toString() + "] in world " + world.getName() + " does not correspond to a valid GameObject");
						if(resolve) {
							entity.remove();
							sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
							fixed++;
						}
						errors++;
					}
				}
				else if(NPCLoader.fromBukkit(entity) == null) {
					if(entity.isInsideVehicle()) continue; // Riding things is OK, usually
					if(entity.hasMetadata("partOf")) continue;
					if(entity.hasMetadata("allow")) continue;
					sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - Entity #" + entity.getEntityId() + " [type " + entity.getType().toString() + "] in world " + world.getName() + " does not correspond to a valid GameObject");
					errors++;
					if(resolve) {
						entity.remove();
						sendMessageIfNotSilent(sender, silent, ChatColor.DARK_GREEN + "        [Fixed]");
						fixed++;
					}
				}
			}
		}
		
		sendMessageIfNotSilent(sender, silent, ChatColor.GRAY + "- Validating floors:");
		for(World world : Bukkit.getWorlds()) {
			if(world.getName().equals("world") || world.getName().contains("_nether") || world.getName().contains("_the_end")) continue;
			if(FloorLoader.fromWorld(world) == null) {
				sendMessageIfNotSilent(sender, silent, ChatColor.RED + "    - World " + world.getName() + " does not correspond to a valid floor");
				errors++;
			}
		}
		
		if(errors == 0) {
			sendMessageIfNotSilent(sender, silent, ChatColor.GREEN + "All checks passed! Active game environment is valid.");
		}
		else {
			sendMessageIfNotSilent(sender, silent, ChatColor.RED + "There were " + errors + " errors found in the active game environment.");
			if(resolve) {
				sendMessageIfNotSilent(sender, silent, ChatColor.RED + "" + fixed + " errors were fixed.");
			}
			else {
				sendMessageIfNotSilent(sender, silent, ChatColor.RED + "Run " + ChatColor.GRAY + "/verifygameintegrity -resolve " + ChatColor.RED + "to attempt to fix these issues. "
						+ "You may need to run this multiple times in some cases.");
			}
		}
		
		// If it's a console command, we'll log even if it's silent
		if(silent && !(sender instanceof Player)) {
			Dragons.getInstance().getLogger().info("Verified game environment. Found " + errors + " errors, fixed " + fixed);
		}
		
		return true;
		
	}
	
	private void sendMessageIfNotSilent(CommandSender sender, boolean silent, String message) {
		if(!silent) {
			sender.sendMessage(message);
		}
	}

}
