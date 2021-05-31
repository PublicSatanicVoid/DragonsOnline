package mc.dragons.tools.dev;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.logging.correlation.CorrelationLogger;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.PathfindingUtil;
import mc.dragons.core.util.StringUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class ExperimentalCommands extends DragonsCommandExecutor {
	
	private MessageHandler debugHandler = new MessageHandler(Dragons.getInstance(), "debug") {
		@Override
		public void receive(String from, Document data) {
			Bukkit.getLogger().info("DEBUG MESSAGE RECEIVED FROM " + from + ": " + data.toJson());
		}
	};
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return true;
		
		Player player = player(sender);
		User user = user(sender);
		
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
								if(block.getType() == Material.AIR && done < regen) {
									done++;
									block.setType(Material.GOLD_BLOCK);
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
		
		else if(label.equalsIgnoreCase("testpermission")) {
			if(player.hasPermission(args[0])) {
				sender.sendMessage("Yes you have it");
			}
			else {
				sender.sendMessage("No you don't have it");
			}
		}
		
		
		else if(label.equalsIgnoreCase("helditemdata") || label.equalsIgnoreCase("whatamiholding")) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			sender.sendMessage("pdc=" + itemStack.getItemMeta().getPersistentDataContainer());
			sender.sendMessage("uuid=" + itemStack.getItemMeta().getPersistentDataContainer().get(Item.ITEM_UUID_KEY, PersistentDataType.STRING));
			Item item = ItemLoader.fromBukkit(itemStack);
			sender.sendMessage("item=" + item);
			if(item != null) {
				sender.sendMessage("item class=" + item.getClassName());
				sender.sendMessage("item data=" + item.getData().toJson());
			}
		}
		
		else if(label.equalsIgnoreCase("testlocaluserstorage")) {
			sender.sendMessage(ChatColor.YELLOW + "METHOD ONE (Full object scan):");
			int n_fullscan = 0;
			for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects(GameObjectType.USER)) {
				User u = (User) gameObject;
				sender.sendMessage("- User: " + u);
				sender.sendMessage("    - name=" + u.getName());
				sender.sendMessage("    - player=" + u.getPlayer());
				n_fullscan++;
			}
			int n_cached = 0;
			sender.sendMessage(ChatColor.YELLOW + "METHOD TWO (UserLoader cache):");
			for(User test : UserLoader.allUsers()) {
				sender.sendMessage("- User: " + test);
				sender.sendMessage("    - name=" + test.getName());
				sender.sendMessage("    - player=" + test.getPlayer());
				n_cached++;
			}
			if(n_fullscan != n_cached) {
				sender.sendMessage(ChatColor.RED + "WARNING: Different methods gave different results (fullscan=" + n_fullscan + " vs cached=" + n_cached + ")");
			}
		}
		
		else if(label.equalsIgnoreCase("testgui")) {
			GUI gui = new GUI(3, "Test GUI")
					.add(new GUIElement(11, Material.COBBLESTONE, "I matter!", "Multi-line\nlore\n\nis cool", 2, u -> u.debug("Clicked the cobble")))
					.add(new GUIElement(13, Material.APPLE, "iApple", "", 5, u -> u.debug("Clicked da appel")))
					.add(itemClassLoader.getItemClassByClassName("GMSword").getAsGuiElement(15, 3, 1999.99, false, u -> u.debug("Purchasing GM Sword!!!")));
			gui.open(user);
		}
		
		else if(label.equalsIgnoreCase("testhdfont")) {
			player.sendMessage(StringUtil.toHdFont(StringUtil.concatArgs(args, 0)));
		}
		
		else if(label.equalsIgnoreCase("rawtext")) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0)));
		}
		
		else if(label.equalsIgnoreCase("testtabname")) {
			player.setPlayerListName(ChatColor.translateAlternateColorCodes('&', StringUtil.concatArgs(args, 0)));
		}
		
		else if(label.equalsIgnoreCase("whoami")) {
			sender.sendMessage("Player="+player);
			sender.sendMessage("User="+user);
			for(User test : UserLoader.allUsers()) {
				if(user.getIdentifier().equals(test.getIdentifier()) && !test.equals(user)) {
					sender.sendMessage("-Also user " + test + " => " + test.getPlayer());
				}
			}
			sender.sendMessage("StorageAccess="+(user==null?"null":user.getStorageAccess()));
		}
		
		else if(label.equalsIgnoreCase("testpathfinding")) {
			Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().clone().setY(0).normalize().multiply(10.0));
			LivingEntity e = (LivingEntity) Bukkit.getWorld("undead_forest").spawnEntity(spawnLoc,
					EntityType.VILLAGER);
			Dragons.getInstance().getBridge().setEntityAI(e, false);
			PathfindingUtil.walkToLocation(e, player.getLocation(), 0.2, unused -> {});
		}
		
		else if(label.equalsIgnoreCase("testphasing")) {
			Entity entity = player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(args[0]));
			if(entity instanceof ArmorStand) {
				((ArmorStand) entity).setCustomName("Test test test");
				((ArmorStand) entity).setCustomNameVisible(true);
			}
			Dragons.getInstance().getEntityHider().hideEntity(player, entity);
		}
		
		else if(label.equalsIgnoreCase("testtpsrecord")) {
			List<Double> record = Dragons.getInstance().getTPSRecord();
			int back = Integer.valueOf(args[0]);
			sender.sendMessage(record.size() + " records");
			sender.sendMessage("starting from " + back + " records back");
			for(int i = record.size() - 1 - back; i < record.size(); i++) {
				sender.sendMessage("#" + i + " = " + record.get(i) + " (" + (record.size() - 1 - i) + " frames back)");
			}
		}
		
		else if(label.equalsIgnoreCase("stresstest")) {
			int n = Integer.valueOf(args[0]);
			World world = player.getWorld();
			Location loc = player.getLocation();
			for(int i = 0; i < n; i++) {
				npcLoader.registerNew(world.spawnEntity(loc, EntityType.ZOMBIE), "F2-UndeadZombie");
			}
			player.sendMessage(ChatColor.GREEN + "Spawned " + n + " undead zombies at your location.");
		}
		
		else if(label.equalsIgnoreCase("killmobs")) {
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
		
		else if(label.equalsIgnoreCase("testarmorstandpose")) {
			ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
			armorStand.setLeftLegPose(new EulerAngle(Double.valueOf(args[0]), Double.valueOf(args[1]), Double.valueOf(args[2])));
			armorStand.setRightLegPose(new EulerAngle(Double.valueOf(args[3]), Double.valueOf(args[4]), Double.valueOf(args[5])));
		}
		
		else if(label.equalsIgnoreCase("testlogging")) {
			for(Level level : new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL }) {
				Dragons.getInstance().getLogger().log(level, "Testing log message on level " + level);
			}
		}
		
		else if(label.equalsIgnoreCase("testleveling")) {
			player.sendMessage("level=" + user.getLevel());
			player.sendMessage("xp="+user.getXP());
			int prevMax = User.calculateMaxXP(user.getLevel());
			player.sendMessage("prevMax=" + prevMax);
			int nextMax = User.calculateMaxXP(user.getLevel() + 1);
			player.sendMessage("nextMax=" + nextMax);
			int n = user.getXP() - prevMax;
			int d = nextMax - prevMax;
			player.sendMessage("numerator=" + n);
			player.sendMessage("denominator=" + d);
			player.sendMessage("progress=" + ((float)n / d));
			player.sendMessage("progress=" + ((double)n / d));
			player.sendMessage("progress=" + user.getLevelProgress());
		}
		
		else if(label.equalsIgnoreCase("testexceptions")) {
			sender.sendMessage("Throwing an NPE");
			((User)null).autoSave(); // Throws an NPE
		}
		
		else if(label.equalsIgnoreCase("testuuidlookup")) {
			UserLoader.uuidFromUsername(args[0], uuid -> {
				sender.sendMessage("UUID of " + args[0] + " is " + uuid);
			});
		}
		
		else if(label.equalsIgnoreCase("testcorrelationlogging")) {
			CorrelationLogger loader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(CorrelationLogger.class);
			UUID id = loader.registerNewCorrelationID();
			loader.log(id, Level.INFO, "hewwo uwu");
			loader.log(id, Level.SEVERE, "ouch");
			sender.sendMessage("correlation id=" + id);
		}
		
		else if(label.equalsIgnoreCase("testbase64encoding")) {
			String encoded = Base64.getEncoder().encodeToString(StringUtil.concatArgs(args, 0).getBytes());
			sender.sendMessage("encoded: " + encoded);
			String decoded = new String(Base64.getDecoder().decode(encoded));
			sender.sendMessage("decoded: " + decoded);
		}
		
		else if(label.equalsIgnoreCase("testnetworkmessage")) {
			debugHandler.send(new Document("payload", new Document("babey", "babey")), args[0]);
		}
		
		else if(label.equalsIgnoreCase("testdocumentdelta")) {
			Document a = new Document("a", 1).append("b", 2).append("c", new Document("x", 3).append("y", 4));
			Document b = new Document("a", 2).append("b", 2).append("d", 3).append("c", new Document("x", 3).append("y", 2));
			Document delta = StorageUtil.getDelta(a, b);
			Document result = StorageUtil.applyDelta(b, delta);
			
			sender.sendMessage("a=" + a.toJson());
			sender.sendMessage("b=" + b.toJson());
			sender.sendMessage("delta=" + delta.toJson());
			sender.sendMessage("result=" + result.toJson());
		}
		
		else if(label.equalsIgnoreCase("testnewfonts")) {
			String[] fonts = { "minecraft:default", "minecraft:uniform", "minecraft:alt" };
			String text = StringUtil.concatArgs(args, 0);
			for(String font : fonts) {
				TextComponent tc = new TextComponent(text);
				tc.setFont(font);
				sender.spigot().sendMessage(new TextComponent(font + " "), tc);
			}
		}
		
		else if(label.equalsIgnoreCase("testuserlookup")) {
			User target = lookupUser(sender, args[0]);
			if(target == null) {
				sender.sendMessage("Not found");
			}
			else {
				sender.sendMessage("User is " + target);
			}
		}
		
		else if(label.equalsIgnoreCase("writelog")) {
			Level level = this.lookup(sender, () -> Level.parse(args[0].toUpperCase()), ChatColor.RED + "Invalid log level! /writelog <level> <message>");
			String message = StringUtil.concatArgs(args, 1);
			if(level == null || message == null) return true;
			dragons.getLogger().log(level, message);
			sender.sendMessage("Log entry written successfully");
		}
		
		else if(label.equalsIgnoreCase("testheader")) {
			player.setPlayerListHeader(user.tablistText(StringUtil.concatArgs(args, 0)));
		}

		else if(label.equalsIgnoreCase("testfooter")) {
			player.setPlayerListFooter(user.tablistText(StringUtil.concatArgs(args, 0)));
		}
		
		else if(label.equalsIgnoreCase("testinvisibleslimes")) {
			boolean allOK = true;
			for(Entity entity : player.getWorld().getEntities()) {
				if(entity instanceof Slime) {
					Slime slime = (Slime) entity;
					if(!slime.isInvisible()) {
						sender.sendMessage(ChatColor.RED + "SLIME #" + slime.getEntityId() + " is NOT invisible (" + StringUtil.locToString(slime.getLocation()) + ")");
						allOK = false;
					}
					else {
						sender.sendMessage(ChatColor.GREEN + "SLIME #" + slime.getEntityId() + " IS invisible (" + StringUtil.locToString(slime.getLocation()) + ")");
					}
				}
			}
			if(!allOK) {
				sender.sendMessage(ChatColor.RED + "-- One or more slimes in this world are visible --");
			}
		}
		
		else if(label.equalsIgnoreCase("testrevealslimes")) {
			for(Entity entity : player.getWorld().getEntities()) {
				if(entity instanceof Slime) {
					Slime slime = (Slime) entity;
					slime.setInvisible(false);
					player.spawnParticle(Particle.DRIP_LAVA, slime.getLocation(), 10);
				}
			}
		}
		
		else if(label.equalsIgnoreCase("testhideslimes")) {
			for(Entity entity : player.getWorld().getEntities()) {
				if(entity instanceof Slime) {
					Slime slime = (Slime) entity;
					slime.setInvisible(true);
					player.spawnParticle(Particle.DRIP_LAVA, slime.getLocation(), 10);
				}
			}
		}
		
		else if(label.equalsIgnoreCase("testdestroyslimes")) {
			for(Entity entity : player.getWorld().getEntities()) {
				if(entity instanceof Slime) {
					Slime slime = (Slime) entity;
					player.spawnParticle(Particle.DRIP_LAVA, slime.getLocation(), 10);
					HologramUtil.unclickableifySlime(slime);
					slime.remove();
				}
			}
		}
		
		else if(label.equalsIgnoreCase("testbadslimes")) {
			int total = 0;
			for(World world : Bukkit.getWorlds()) {
				total += world.getEntitiesByClass(Slime.class).size();
				for(Slime slime : world.getEntitiesByClass(Slime.class)) {
					Dragons.getInstance().getLogger().severe("BAD SLIME YOU SUCK: " + StringUtil.entityToString(slime) + " - allow:" + slime.hasMetadata("allow")
						+ " - nRClickHandlers:" + PlayerEventListeners.getRightClickHandlers(slime) + " - ClickySlime:" + slime.hasMetadata(HologramUtil.KEY_CLICKABLE_SLIME)
						);
				}
			}
			Dragons.getInstance().getLogger().debug("THERE ARE " + total + " SLIMES");
		}
		
		else if(label.equalsIgnoreCase("mockuser")) {
			Document data = Document.parse(user(sender).getData().toJson());
			data.append("_id", UUID.randomUUID());
			data.append("username", args[0]);
			data.append("currentServer", args[1]);
			data.append("mock", true);
			dragons.getMongoConfig().getDatabase().getCollection("gameobjects").insertOne(data);
			sender.sendMessage("UUID: " + data.get("_id", UUID.class));
		}
		
		else if(label.equalsIgnoreCase("mocksudo")) {
			Document data = dragons.getMongoConfig().getDatabase().getCollection("gameobjects").find(new Document("username", args[0])).first();
			if(data.getBoolean("mock", false)) {
				dragons.getRemoteAdminHandler().sendRemoteSudo(data.getString("currentServer"), data.get("_id", UUID.class), StringUtil.concatArgs(args, 1));
			}
		}
		
		else if(label.equalsIgnoreCase("mockinject")) {
			StorageManager storageManager = dragons.getPersistentStorageManager();
			StorageAccess storageAccess = storageManager.getStorageAccess(GameObjectType.USER, new Document("username", args[0]));
			MockPlayer mockPlayer = new MockPlayer(storageAccess.getIdentifier().getUUID(), args[0]);
			MockUser mockUser = new MockUser(null, storageManager, storageAccess);
			dragons.getGameObjectRegistry().getRegisteredObjects().removeIf(obj -> obj instanceof User && ((User) obj).getName().equalsIgnoreCase(args[0]));
			dragons.getGameObjectRegistry().getRegisteredObjects().add(mockUser);
			UserLoader.allUsers().removeIf(u -> u.getName().equalsIgnoreCase(args[0]));
			UserLoader.allUsers().add(mockUser);
			UserLoader.assign(mockPlayer, mockUser);
			mockUser.setPlayer(mockPlayer);
			sender.sendMessage("UUID: " + mockUser.getUUID());
			sender.sendMessage("Server: " + mockUser.getServerName() + " ?= " + storageAccess.get("currentServer", String.class));
			sender.sendMessage("Player: " + mockPlayer);
			sender.sendMessage("CommandSender: " + mockUser.getCommandSender());
			sender.sendMessage("CommandSender is player: " + (mockUser.getCommandSender() instanceof Player));
			sender.sendMessage("Passes for player: " + (mockPlayer instanceof Player));
			sender.sendMessage("Passes for user:"  + (mockUser instanceof User));
			sender.sendMessage("Player casted from sender: " + ((Player) mockUser.getCommandSender()));
			sender.sendMessage("User lookup: " + UserLoader.fromPlayer(mockPlayer));
			sender.sendMessage("User lookup by sender: " + UserLoader.fromPlayer((Player) mockUser.getCommandSender()));
			sender.sendMessage("Exists in UserLoader cache: " + UserLoader.allUsers().contains(mockUser));
			sender.sendMessage("Exists in game object registry: " + dragons.getGameObjectRegistry().getRegisteredObjects().contains(mockUser));
			sender.sendMessage("Exists in filtered game object registry: " + dragons.getGameObjectRegistry().getRegisteredObjects(GameObjectType.USER).contains(mockUser));
		}
		
		else if(label.equalsIgnoreCase("mockserver")) {
			StorageAccess storageAccess = dragons.getPersistentStorageManager().getStorageAccess(GameObjectType.USER, new Document("username", args[0]));
			storageAccess.set("currentServer", args[1]);
		}
		
		else if(label.equalsIgnoreCase("testitemstash")) {
			user.stashItems(questLoader.getQuestByName(args[0]), Material.valueOf(args[1]));
			sender.sendMessage("Stash: " + user.getData().get("questStash", new Document()).toJson());
		}
		
		else if(label.equalsIgnoreCase("testitemunstash")) {
			user.unstashItems(questLoader.getQuestByName(args[0]), Material.valueOf(args[1]));
			sender.sendMessage("Stash: " + user.getData().get("questStash", new Document()).toJson());
		}
		
		else if(label.equalsIgnoreCase("testmobai")) {
			boolean ai = Boolean.valueOf(args[0]);
			boolean gravity = Boolean.valueOf(args[1]);
			boolean collidable = Boolean.valueOf(args[2]);
			LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
			entity.setAI(ai);
			entity.setGravity(gravity);
			entity.setCollidable(collidable);
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid experimental command! Was it removed or registered improperly?");
		}
		
		return true;
	}

	
	
}
