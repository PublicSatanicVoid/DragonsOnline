package mc.dragons.dev;

import java.util.List;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.dev.TaskLoader.Task;
import net.md_5.bungee.api.ChatColor;

public class DevUserHook implements UserHook {
	
	private TaskLoader taskLoader;
	
	public DevUserHook() {
		taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
	}
	
	@Override
	public void onInitialize(User user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVerifiedJoin(User user) {
		List<Task> myTasks = taskLoader.getAllTasksWith(user.getName());
		user.getPlayer().sendMessage(ChatColor.GOLD + "You have " + myTasks.size() + " tasks! " + ChatColor.YELLOW + "/tasks my");
		if(PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, false)) {
			List<Task> myTasksToReview = taskLoader.getAllWaitingTasks();
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + myTasksToReview.size() + " tasks that need reviewing. " + ChatColor.YELLOW + "/tasks waiting");
			List<Task> doneTasks = taskLoader.getAllCompletedTasks(true);
			user.getPlayer().sendMessage(ChatColor.GOLD + "There are " + doneTasks.size() + " completed tasks that need to be closed. " + ChatColor.YELLOW + "/tasks done");
		}
	}

	@Override
	public void onUpdateState(User user, Location cachedLocation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAutoSave(User user, Document autoSaveData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onQuit(User user) {
		// TODO Auto-generated method stub
		
	}

}
