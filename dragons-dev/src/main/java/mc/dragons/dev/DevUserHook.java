package mc.dragons.dev;

import static mc.dragons.core.util.BukkitUtil.async;
import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.dev.notifier.DiscordNotifier;
import mc.dragons.dev.tasks.TaskLoader;
import mc.dragons.dev.tasks.TaskLoader.Task;
import net.md_5.bungee.api.ChatColor;

public class DevUserHook implements UserHook, Listener {
	private TaskLoader taskLoader;
	private ItemLoader itemLoader;
	private DiscordNotifier buildNotifier;
	private boolean loadedTasks = false;
	
	public DevUserHook() {
		taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
		itemLoader = GameObjectType.getLoader(ItemLoader.class);
		buildNotifier = JavaPlugin.getPlugin(DragonsDev.class).getBuildNotifier();
	}
	
	@Override
	public void onVerifiedJoin(User user) {		
		/* Lazy-load task markers */
		if(!loadedTasks) {
			
			// This is extremely important to clear out pre-existing slimes that would obstruct the new ones
			async(() -> {
				for(int i = 0; i < 5; i++) {
					sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "verifygameintegrity -resolve -silent"));
					try {
						Thread.sleep(500);
					} catch (InterruptedException ignored) {}
				}
			});
			
			taskLoader.getAllInProgressTasks();
			loadedTasks = true;
		}
		
		/* Remind of any outstanding tasks */
		List<Task> myTasks = taskLoader.getAllTasksWith(user);
		boolean tm = PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, false);
		user.getPlayer().sendMessage(ChatColor.GOLD + "You have " + myTasks.size() + " tasks! " + ChatColor.YELLOW + "/tasks" + (tm ? " my" : ""));
		if(tm) {
			List<Task> myTasksToReview = taskLoader.getAllWaitingTasks();
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + myTasksToReview.size() + " tasks that need reviewing. " + ChatColor.YELLOW + "/tasks waiting");
			List<Task> doneTasks = taskLoader.getAllCompletedTasks(true);
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + doneTasks.size() + " completed tasks ready for review. " + ChatColor.YELLOW + "/tasks done");
		}
		
		/* Notify Discord when builders join the server */
		Rank rank = user.getRank();
		if(rank == Rank.BUILDER || rank == Rank.BUILDER_CMD || rank == Rank.BUILD_MANAGER
				|| rank == Rank.HEAD_BUILDER || rank == Rank.NEW_BUILDER) {
			buildNotifier.sendNotification("[Builder Join] " + rank.getRankName() + " " + user.getName() + " joined the server!");
		}
		
		/* Set star count, if not already set */
		if(!user.getData().containsKey("stars")) {
			user.getStorageAccess().set("stars", 0);
		}
		user.getPlayer().sendMessage(ChatColor.GOLD + "You are have " + user.getData().getInteger("stars", 0) + " stars in your balance");
	}
	
	@Override
	public void onAutoSave(User user, Document data) {
		tryUnvanillify(user, data);
	}
	
	@EventHandler
	public void onInventoryPlace(InventoryClickEvent event) {
		if(event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE
				|| event.getAction() == InventoryAction.PLACE_SOME) {
			sync(() -> tryUnvanillify(UserLoader.fromPlayer((Player) event.getWhoClicked()), new Document()), 10);
		}
	}
	
	public void tryUnvanillify(User user, Document data) {
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, false)) return;
		for(ItemStack is : user.getPlayer().getInventory()) {
			if(is == null) continue;
			if(!is.getType().isItem()) continue;
			Item item = ItemLoader.fromBukkit(is);
			if(item == null) {
				itemLoader.makeFromVanilla(is);
			}
		}
		data.append("inventory", user.getInventoryAsDocument());
	}
	
	public static int getStars(User user) {
		return user.getData().get("stars", 0);
	}
	
	public static void addStars(User user, int stars) {
		user.getStorageAccess().pull("stars", Integer.class);
		user.getStorageAccess().set("stars", getStars(user) + stars);
		if(user.getPlayer() != null) {
			user.getPlayer().sendMessage(ChatColor.GOLD + "You " + (stars > 0 ? "received " : "lost ") + Math.abs(stars) + " stars!");
		}
	}
}
