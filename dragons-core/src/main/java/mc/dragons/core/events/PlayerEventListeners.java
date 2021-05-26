package mc.dragons.core.events;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemConstants;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.UserHookRegistry;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class PlayerEventListeners implements Listener {
	public static final String GOLD_CURRENCY_ITEM_CLASS_NAME = "Currency:Gold";
	
	private static Map<Integer, List<Consumer<User>>> rightClickHandlers = new HashMap<>(); // OMG SOMEONE CALL THE STATIC POLICE OMG
	
	public static void addRightClickHandler(Entity entity, Consumer<User> handler) {
		rightClickHandlers.computeIfAbsent(entity.getEntityId(), e -> new ArrayList<>()).add(handler);
	}

	public static void removeRightClickHandlers(Entity entity) {
		rightClickHandlers.remove(entity.getEntityId());
	}
	
	public static List<Consumer<User>> getRightClickHandlers(Entity entity) {
		return rightClickHandlers.getOrDefault(entity.getEntityId(), List.of());
	}
	
	private static ItemClassLoader itemClassLoader;
	public static ItemClass[] DEFAULT_INVENTORY;
	
	private UserHookRegistry userHookRegistry;
	
	static {
		itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
		DEFAULT_INVENTORY = new ItemClass[] { itemClassLoader.getItemClassByClassName("LousyStick") };
	}
	
	private Dragons plugin;
	private DragonsLogger LOGGER;

	private UserLoader userLoader;
	private ItemLoader itemLoader;
	
	private Table<User, Entity, Long> interacts = HashBasedTable.create();

	public PlayerEventListeners(Dragons instance) {
		plugin = instance;
		LOGGER = instance.getLogger();
		userLoader = GameObjectType.USER.getLoader();
		itemLoader = GameObjectType.ITEM.getLoader();
		userHookRegistry = instance.getUserHookRegistry();
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		LOGGER.debug("Chat event from player " + event.getPlayer().getName());
		User user = UserLoader.fromPlayer(event.getPlayer());
		event.setCancelled(true);
		user.chat(event.getMessage());
	}

	@EventHandler
	public void onClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);
		Action action = event.getAction();
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.MAP) { // TODO do we need this? (changing from EMPTY_MAP [1.12] to MAP [1.16]
			user.debug("Right click with map, denying");
			event.setUseItemInHand(Event.Result.DENY);
			event.setCancelled(true);
		}
		if (action == Action.RIGHT_CLICK_BLOCK) {
			user.debug("Right click block");
			Block clicked = event.getClickedBlock();
			if (clicked.getType().toString().toUpperCase().contains("SIGN")) {
				Sign sign = (Sign) clicked.getState();
				if (sign.getLine(0).equals("[RIGHT CLICK]")) {
					if (sign.getLine(2).equals("Join as player")) {
						if (user.getSystemProfile() != null) {
							user.setSystemProfile(null);
						}
						player.sendMessage(ChatColor.GREEN + "Joining as a player. You can always sign in to your system profile later.");
						player.teleport(user.getSavedLocation());
						user.handleJoin(false);
					} else if (sign.getLine(2).equals("Join as staff")) {
						if (user.getSystemProfile() == null) {
							player.sendMessage(ChatColor.RED + "You must be logged in to your system profile to join as staff!");
							return;
						}
						if (sign.getLine(3).equals("Vanished (Mod+)")) {
							if (!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MODERATOR, true)) {
								return;
							}
							user.setVanished(true);
						}
						player.teleport(user.getSavedStaffLocation());
						user.handleJoin(false);
					} else {
						player.sendMessage(ChatColor.RED + "I don't know what to do with this!");
					}
				}
			}
			return;
		}
		ItemStack heldItem = player.getInventory().getItemInMainHand();
		if (heldItem == null) {
			return;
		}
		Item item = ItemLoader.fromBukkit(heldItem);
		if (item == null) {
			return;
		}
		if (action == Action.LEFT_CLICK_AIR) {
			user.debug("Left click with " + item.getName());
			item.getItemClass().handleLeftClick(user);
		} else if (action == Action.RIGHT_CLICK_AIR) {
			user.debug("Right click with " + item.getName());
			item.getItemClass().handleRightClick(user);
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		LOGGER.debug("Death event from " + event.getEntity().getName());
		Player player = event.getEntity();
		final User user = UserLoader.fromPlayer(player);
		user.debug("death!");
		player.sendMessage(ChatColor.DARK_RED + "You died!");
		final int countdown = plugin.getServerOptions().getDeathCountdown();
		boolean normal = true;
		for(UserHook hook : userHookRegistry.getHooks()) {
			if(!hook.onDeath(user)) {
				normal = false;
			}
		}
		user.debug("-normal="+normal);
		boolean fNormal = normal;
		sync(() -> {
			if(fNormal) {
				user.sendToFloor("BeginnerTown");
			}
			user.respawn();
			if(fNormal) {
				user.getPlayer().sendTitle(ChatColor.RED + "< " + ChatColor.DARK_RED + "You are dead" + ChatColor.RED + " >", ChatColor.GRAY + "Respawning on floor 1", 0, 20 * (countdown - 2), 40);
				user.setDeathCountdown(countdown);
			}
		}, 1);
	}

	/**
	 * FIXME Drop quantity sometimes calculated incorrectly when iterated
	 * 
	 * @param event
	 */
	@EventHandler
	public void onDropItem(PlayerDropItemEvent event) {
		ItemStack drop = event.getItemDrop().getItemStack();
		int amt = drop.getAmount();
		Item item = ItemLoader.fromBukkit(drop);
		
		
		LOGGER.trace("Drop item event on " + event.getPlayer().getName() + " of " + (item == null ? "null" : item.getIdentifier()) + " (x" + amt + ")");
		if (item == null) {
			return;
		}
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.debug("held="+item.getQuantity()+"="+item.getItemStack().getAmount());
		if (item.isUndroppable() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			user.sendActionBar(ChatColor.DARK_RED + "You can't drop this item!");
			event.setCancelled(true);
			return;
		}
		Item dropItem = itemLoader.registerNew(item);
		dropItem.setQuantity(amt);
		user.debug("drop="+dropItem.getQuantity()+"="+dropItem.getItemStack().getAmount());
		event.getItemDrop().setItemStack(dropItem.getItemStack());
		user.takeItem(item, amt, true, false, true);
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		LOGGER.trace("Item held change event on " + event.getPlayer().getName() + ": " + event.getPreviousSlot() + " -> " + event.getNewSlot());
		ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
		Item heldItem = ItemLoader.fromBukkit(held);
		if(heldItem != null && !heldItem.hasCooldownRemaining()) {
			event.getPlayer().getInventory().setItemInMainHand(heldItem.localRename(heldItem.getDecoratedName()));
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		LOGGER.trace("Gamemode change event on " + event.getPlayer().getName() + " to " + event.getNewGameMode());
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.setGameMode(event.getNewGameMode(), false);
	}

	@EventHandler
	public void onHungerChangeEvent(FoodLevelChangeEvent event) {
		LOGGER.trace("Hunger change event on " + event.getEntity().getName());
		event.setCancelled(true);
		Player player = (Player) event.getEntity();
		player.setFoodLevel(20);
	}
	
	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		LOGGER.debug("Interact entity event on " + event.getPlayer().getName() + " to " + StringUtil.entityToString(event.getRightClicked()));
		User user = UserLoader.fromPlayer(event.getPlayer());
		Entity rightClicked = event.getRightClicked();
		user.debug("Right-click");
		Long lastAccess = interacts.get(user, rightClicked);
		if(lastAccess == null) lastAccess = 0L;
		long now = System.currentTimeMillis();
		if(now - lastAccess <= 20L) {
			LOGGER.debug("-Duplicate interact event, ignoring");
			return;
		}
		interacts.put(user, rightClicked, now);
		NPC npc = NPCLoader.fromBukkit(rightClicked);
		List<Consumer<User>> handlers = getRightClickHandlers(rightClicked);
		if(handlers.size() > 0) {
			LOGGER.debug("-Executing associated handlers (" + handlers.size() + ")");
			handlers.forEach(r -> r.accept(user));
		}
		if (npc != null) {
			user.debug("- Clicked an NPC");
			Item item = ItemLoader.fromBukkit(user.getPlayer().getInventory().getItemInMainHand());
			if (item != null) {
				user.debug("- Holding an RPG item");
				if(item.getItemClass().isGMLocked() && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false)) {
					user.debug("- GM Locked, cancelling");
					user.sendActionBar(ChatColor.DARK_RED + "- This item is GM locked! -");
					return;
				}
				if (item.getClassName().equals(ItemConstants.IMMORTAL_OVERRIDE_ITEM_CLASS)) {
					user.debug("- Destroy the NPC");
					npc.getEntity().remove();
					plugin.getGameObjectRegistry().removeFromDatabase(npc);
					user.getPlayer().sendMessage(ChatColor.GREEN + "Removed NPC successfully.");
					return;
				}
			}
			if (npc.getNPCType() == NPC.NPCType.QUEST && user.hasActiveDialogue() && System.currentTimeMillis() - user.getWhenBeganDialogue() > 1000L) {
				user.debug("Next dialogue");
				user.nextDialogue();
				return;
			}
			npc.getNPCClass().executeConditionals(NPCTrigger.CLICK, user, npc);
			npc.getNPCClass().getAddons().forEach(addon -> addon.onInteract(npc, user));
			item.getItemClass().handleRightClick(user); // TODO verify that this works
		}
		user.updateQuests(event);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		LOGGER.debug("Join event on " + event.getPlayer().getName());
		event.setJoinMessage(null);
		Player player = event.getPlayer();
		if(!plugin.isJoinable()) {
			event.getPlayer().kickPlayer(ChatColor.YELLOW + "This server (" + plugin.getServerName() + ") is still loading and is not joinable yet.\n"
					+ "Please try again in a few minutes.\n\n"
					+ "This is not a punishment-related kick.");
			return;
		}
		UUID uuid = player.getUniqueId();
		User user = userLoader.loadObject(uuid);
		boolean firstJoin = false;
		if (user == null) {
			firstJoin = true;
			plugin.getLogger().info("Player " + player.getName() + " joined for the first time");
			user = userLoader.registerNew(player);
			user.sendToFloor("BeginnerTown");
			for(ItemClass itemClass : DEFAULT_INVENTORY) {
				user.giveItem(itemLoader.registerNew(itemClass), true, false, true);
			}
		}
		Floor floor = FloorLoader.fromLocation(player.getLocation());
		if(floor.isPlayerLocked() && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false)) {
			player.kickPlayer(ChatColor.RED + "This floor (#" + floor.getLevelMin() + " " + floor.getDisplayName() + ") is currently locked for maintenance.\n"
					+ "You will be allowed to re-join once the maintenance completes.\n\n"
					+ "This is not a punishment-related kick.\n\n"
					+ new SimpleDateFormat("yyyy MM dd HH:mm z").format(new Date()));
			return;
		}
		if (user.getRank().isStaff()) {
			GameMode restoreTo = user.getSavedGameMode();
			user.setGameMode(GameMode.ADVENTURE, true);
			user.setGameMode(restoreTo, false);
			user.sendToFloor("Staff");
			player.setPlayerListName(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + "[Staff Joining]");
			player.sendMessage(ChatColor.AQUA + "Please login to your system profile or select \"Join as player\".");
		} else {
			user.handleJoin(firstJoin);
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		User user = UserLoader.fromPlayer(event.getPlayer());
		if (user.hasDeathCountdown()) {
			event.setTo(event.getFrom());
			return;
		}
		user.handleMove();
	}

	@EventHandler
	public void onPickupItem(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		final Player player = (Player) event.getEntity();
		ItemStack pickup = event.getItem().getItemStack();
		User user = UserLoader.fromPlayer(player);
		if (pickup == null) {
			return;
		}
		if (pickup.getItemMeta() == null) {
			return;
		}
		if (pickup.getItemMeta().getDisplayName() == null) {
			return;
		}
		final Item item = ItemLoader.fromBukkit(pickup);
		if (item == null) {
			return;
		}

		LOGGER.trace("Pickup item event on " + player.getName() + " of " + (item == null ? "null" : item.getIdentifier()) + " (x" + pickup.getAmount() + ")");
		if (item.getItemClass().getClassName().equals(GOLD_CURRENCY_ITEM_CLASS_NAME)) {
			int amount = pickup.getAmount();
			user.giveGold(amount * 1.0D);
			new BukkitRunnable() {
				@Override
				public void run() {
					Arrays.asList(player.getInventory().getContents()).stream().filter(i -> (i != null)).filter(i -> (i.getItemMeta() != null))
							.map(i -> ItemLoader.fromBukkit(i)).filter(Objects::nonNull).filter(i -> i.getClassName().equals(GOLD_CURRENCY_ITEM_CLASS_NAME))
							.forEach(i -> {
								player.getInventory().remove(i.getItemStack());
								plugin.getGameObjectRegistry().removeFromDatabase(item);
							});
				}
			}.runTaskLater(plugin, 1L);
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0F, 1.3F);
			return;
		}
		
		item.setQuantity(pickup.getAmount());
		user.giveItem(item, true, false, false);
		player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		event.setCancelled(true);
		event.getItem().remove();
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		LOGGER.debug("Quit event on " + event.getPlayer().getName());
		User user = UserLoader.fromPlayer(event.getPlayer());
		if(user == null) return;
		user.handleQuit();
		event.setQuitMessage(null);
	}
}
