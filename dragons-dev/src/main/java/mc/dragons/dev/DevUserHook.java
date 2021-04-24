package mc.dragons.dev;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.dev.notifier.DiscordNotifier;
import mc.dragons.dev.tasks.TaskLoader;
import mc.dragons.dev.tasks.TaskLoader.Task;
import net.md_5.bungee.api.ChatColor;

public class DevUserHook implements UserHook {
	private TaskLoader taskLoader;
	private DiscordNotifier buildNotifier;
	
	public DevUserHook() {
		taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
		buildNotifier = JavaPlugin.getPlugin(DragonsDev.class).getBuildNotifier();
	}

	@Override
	public void onVerifiedJoin(User user) {
		/* Remind of any outstanding tasks */
		List<Task> myTasks = taskLoader.getAllTasksWith(user);
		user.getPlayer().sendMessage(ChatColor.GOLD + "You have " + myTasks.size() + " tasks! " + ChatColor.YELLOW + "/tasks my");
		if(PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, false)) {
			List<Task> myTasksToReview = taskLoader.getAllWaitingTasks();
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + myTasksToReview.size() + " tasks that need reviewing. " + ChatColor.YELLOW + "/tasks waiting");
			List<Task> doneTasks = taskLoader.getAllCompletedTasks(true);
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + doneTasks.size() + " completed tasks that need to be closed. " + ChatColor.YELLOW + "/tasks done");
		}
		
		/* Notify Discord when builders join the server */
		Rank rank = user.getRank();
		if(rank == Rank.BUILDER || rank == Rank.BUILDER_CMD || rank == Rank.BUILD_MANAGER
				|| rank == Rank.HEAD_BUILDER || rank == Rank.NEW_BUILDER) {
			buildNotifier.sendNotification("[Builder Join] " + rank.getRankName() + " " + user.getName() + " joined the server!");
		}
	}
}
