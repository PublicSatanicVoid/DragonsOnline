package mc.dragons.tools.dev;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.util.HiddenStringUtil;
import mc.dragons.core.util.PathfindingUtil;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class ExperimentalCommands implements CommandExecutor {

	private ItemClassLoader itemClassLoader;
	private NPCLoader npcLoader;
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(itemClassLoader == null) {
			itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
			npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		}
		
		Player player = null;
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, true)) return true;
		}
		
		if(label.equalsIgnoreCase("testmineregen")) {
			int minutesPerSecond = Integer.valueOf(args[0]);
			int radius = Integer.valueOf(args[1]);
			double alpha = Double.valueOf(args[2]);
			double beta = Double.valueOf(args[3]);
			final Player fPlayer = player;
			final Location center = player.getLocation();
			int maxIterations = Integer.valueOf(args[4]);
			Bukkit.broadcastMessage("==Beginning simulation of mining regen. SimulatedMinutesPerSecond=" + minutesPerSecond + ", Radius=" + radius + ", Alpha=" + alpha + ", Beta=" + beta + ", MaxIterations=" + maxIterations);
			new BukkitRunnable() {
				private int iterations = 0;
				
				@Override public void run() {
					int players = (int) fPlayer.getNearbyEntities(radius, radius, radius).stream().filter(e -> e.getType() == EntityType.PLAYER).count();
					int mined = 0;
					int cX = center.getBlockX();
					int cY = center.getBlockY();
					int cZ = center.getBlockZ();
					for(int x = cX - radius; x <= cX + radius; x++) {
						for(int y  = cY - radius; y <= cY + radius; y++) {
							for(int z = cZ - radius; z <= cZ + radius; z++) {
								Block block = center.getWorld().getBlockAt(x, y, z);
								if(block.getType() == Material.AIR) {
									mined++;
								}
							}
						}
					}
					if(mined == 0) {
						Bukkit.broadcastMessage("==No blocks left to regenerate, ending simulation (" + iterations + " iterations)==");
						this.cancel();
						return;
					}
					int regen = (int) Math.ceil(mined *  (alpha + beta * players));
					Bukkit.broadcastMessage("Regenerating " + regen + " blocks (players=" + players + ", mined=" + mined + ")");
					int done = 0;
					for(int x = cX - radius; x <= cX + radius; x++) {
						for(int y  = cY - radius; y <= cY + radius; y++) {
							for(int z = cZ - radius; z <= cZ + radius; z++) {
								Block block = center.getWorld().getBlockAt(x, y, z);
								if(block.getType() == Material.AIR) {
									if(done < regen) {
										done++;
										block.setType(Material.GOLD_BLOCK);
									}
								}
							}
						}
					}
					iterations++;
					if(iterations >= maxIterations) {
						Bukkit.broadcastMessage("==Completed simulation==");
						this.cancel();
					}
				}
			}.runTaskTimer(Dragons.getInstance(), 20L, 20L / minutesPerSecond);
		}
		
		if(label.equalsIgnoreCase("testpermission")) {
			if(player.hasPermission(args[0])) {
				sender.sendMessage("Yes you have it");
			}
			else {
				sender.sendMessage("No you don't have it");
			}
		}
		
		
		if(label.equalsIgnoreCase("helditemdata") || label.equalsIgnoreCase("whatamiholding")) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			sender.sendMessage("meta=" + itemStack.getItemMeta());
			sender.sendMessage("lore=" + itemStack.getItemMeta().getLore());
			sender.sendMessage("lore sz=" + itemStack.getItemMeta().getLore().size());
			sender.sendMessage("lore 0=" + itemStack.getItemMeta().getLore().get(0));
			sender.sendMessage("converted lore 0=" + HiddenStringUtil.extractHiddenString(itemStack.getItemMeta().getLore().get(0)));
			Item item = ItemLoader.fromBukkit(itemStack);
			sender.sendMessage(item.getIdentifier().toString());
			sender.sendMessage("class=" + item.getClassName());
			sender.sendMessage("quantity=" + item.getQuantity());
			sender.sendMessage("bukkit amt=" + itemStack.getAmount());
		}
		
		if(label.equalsIgnoreCase("testlocaluserstorage")) {
			sender.sendMessage(ChatColor.YELLOW + "METHOD ONE (Full object scan):");
			int n_fullscan = 0;
			for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects(GameObjectType.USER)) {
				User u = (User) gameObject;
				sender.sendMessage("- Here's a user: " + u);
				sender.sendMessage("    - name=" + u.getName());
				sender.sendMessage("    - player=" + u.getPlayer());
				n_fullscan++;
			}
			int n_cached = 0;
			sender.sendMessage(ChatColor.YELLOW + "METHOD TWO (UserLoader cache):");
			for(User test : UserLoader.allUsers()) {
				sender.sendMessage("- Here's a user: " + test);
				sender.sendMessage("    - name=" + test.getName());
				sender.sendMessage("    - player=" + test.getPlayer());
				n_cached++;
			}
			if(n_fullscan != n_cached) {
				sender.sendMessage(ChatColor.RED + "WARNING: Different methods gave different results (fullscan=" + n_fullscan + " vs cached=" + n_cached + ")");
			}
		}
		
		if(label.equalsIgnoreCase("testgui")) {
			GUI gui = new GUI(3, "Test GUI")
					.add(new GUIElement(11, Material.COBBLESTONE, "I matter!", "Multi-line\nlore\n\nis cool", 2, u -> u.debug("Clicked the cobble")))
					.add(new GUIElement(13, Material.APPLE, "iApple", "", 5, u -> u.debug("Clicked da appel")))
					.add(itemClassLoader.getItemClassByClassName("GMSword").getAsGuiElement(15, 3, 1999.99, false, u -> u.debug("Purchasing GM Sword!!!")));
			gui.open(user);
		}
		
		if(label.equalsIgnoreCase("testhdfont")) {
			player.sendMessage(StringUtil.toHdFont(StringUtil.concatArgs(args, 0)));
		}
		
		if(label.equalsIgnoreCase("rawtext")) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0)));
		}
		
		if(label.equalsIgnoreCase("testtabname")) {
			player.setPlayerListName(ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0)));
		}
		
		if(label.equalsIgnoreCase("whoami")) {
			sender.sendMessage("Player="+player);
			sender.sendMessage("User="+user);
			for(User test : UserLoader.allUsers()) {
				if(user.getIdentifier().equals(test.getIdentifier()) && test != user) {
					sender.sendMessage("-Also user " + test + " => " + test.getPlayer());
				}
			}
		}
		
		if(label.equalsIgnoreCase("testpathfinding")) {
			Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(10.0));
			LivingEntity e = (LivingEntity) Bukkit.getWorld("undead_forest").spawnEntity(spawnLoc,
					EntityType.VILLAGER);
			Dragons.getInstance().getBridge().setEntityAI(e, false);
			PathfindingUtil.walkToLocation(e, player.getLocation(), 0.2, unused -> {});
		}
		
		if(label.equalsIgnoreCase("testphasing")) {
			Entity entity = player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(args[0]));
			if(entity instanceof ArmorStand) {
				((ArmorStand) entity).setCustomName("Test test test");
				((ArmorStand) entity).setCustomNameVisible(true);
			}
			Dragons.getInstance().getEntityHider().hideEntity(player, entity);
		}
		
		if(label.equalsIgnoreCase("stresstest")) {
			int n = Integer.valueOf(args[0]);
			World world = player.getWorld();
			Location loc = player.getLocation();
			for(int i = 0; i < n; i++) {
				npcLoader.registerNew(world.spawnEntity(loc, EntityType.ZOMBIE), "F2-UndeadZombie");
			}
			player.sendMessage(ChatColor.GREEN + "Spawned " + n + " undead zombies at your location.");
		}
		
		if(label.equalsIgnoreCase("killmobs")) {
			int n = 0;
			for(Entity e : player.getWorld().getEntities()) {
				NPC npc = NPCLoader.fromBukkit(e);
				if(npc == null) continue;
				if(npc.getNPCType().isPersistent()) continue;
				npc.remove();
				n++;
			}
			player.sendMessage(ChatColor.GREEN + "Killed " + n + " non-persistent mobs.");
		}
		
		if(label.equalsIgnoreCase("testarmorstandpose")) {
			ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
			armorStand.setLeftLegPose(new EulerAngle(Double.valueOf(args[0]), Double.valueOf(args[1]), Double.valueOf(args[2])));
			armorStand.setRightLegPose(new EulerAngle(Double.valueOf(args[3]), Double.valueOf(args[4]), Double.valueOf(args[5])));
		}
		
		if(label.equalsIgnoreCase("testlogging")) {
			for(Level level : new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL }) {
				Dragons.getInstance().getLogger().log(level, "Testing log message on level " + level);
			}
		}
		
		if(label.equalsIgnoreCase("testleveling")) {
			int prevMax = User.calculateMaxXP(user.getLevel());
			player.sendMessage("prevMax=" + prevMax);
			int n = user.getXP() - prevMax;
			int d = User.calculateMaxXP(user.getLevel() + 1) - prevMax;
			player.sendMessage("numerator=" + n);
			player.sendMessage("denominator=" + d);
			player.sendMessage("progress=" + ((float)n / d));
			player.sendMessage("progress=" + ((double)n / d));
			player.sendMessage("progress=" + user.getLevelProgress());
		}
		
		if(label.equalsIgnoreCase("testexceptions")) {
			sender.sendMessage("Throwing an NPE");
			((User)null).autoSave(); // Throws an NPE
		}
		
		if(label.equalsIgnoreCase("testuuidlookup")) {
			UserLoader.uuidFromUsername(args[0], uuid -> {
				sender.sendMessage("UUID of " + args[0] + " is " + uuid);
			});
		}
		
		return true;
	}

	
	
}
