package mc.dragons.dev;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.impl.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.dev.TaskLoader.Task;
public class TaskCommands implements CommandExecutor {

	private TaskLoader taskLoader;
	
	public TaskCommands() {
		taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, true)) return true;
		
		if(label.equalsIgnoreCase("task")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Create a new task.");
				sender.sendMessage(ChatColor.YELLOW + "/task <new task name>");
				return true;
			}
			Task task = taskLoader.addTask(user.getName(), StringUtil.concatArgs(args, 0));
			sender.sendMessage(ChatColor.GREEN + "Created task " + ChatColor.UNDERLINE + "#" + task.getId() + ChatColor.GREEN + " successfully!"
					+ ChatColor.ITALIC + " /taskinfo " + task.getId() + ChatColor.GREEN + " to track it.");
			UserLoader.allUsers().stream().filter(u -> PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, false))
				.map(u -> u.getPlayer())
				.forEach(p -> p.sendMessage(ChatColor.GREEN + "A new task is awaiting approval: #" + task.getId() + ", " + task.getName() + " (by " + task.getBy() + ")"));
			return true;
		}
	
		if(label.equalsIgnoreCase("tasks")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "List all tasks.");
				sender.sendMessage(ChatColor.YELLOW + "/tasks <all|my|waiting|approved|rejected|done|closed>");
				return true;
			}
			List<Task> tasks = null;
			if(args[0].equalsIgnoreCase("all")) {
				tasks = taskLoader.getAllTasks();
			}
			else if(args[0].equalsIgnoreCase("my")) {
				tasks = taskLoader.getAllTasksWith(user.getName());
			}
			else if(args[0].equalsIgnoreCase("waiting")) {
				tasks = taskLoader.getAllWaitingTasks();
			}
			else if(args[0].equalsIgnoreCase("approved")) {
				tasks = taskLoader.getAllApprovedTasks();
			}
			else if(args[0].equalsIgnoreCase("rejected")) {
				tasks = taskLoader.getAllRejectedTasks();
			}
			else if(args[0].equalsIgnoreCase("done")) {
				tasks = taskLoader.getAllCompletedTasks(true);
			}
			else if(args[0].equalsIgnoreCase("closed")) {
				tasks = taskLoader.getAllClosedTasks(true);
			}
			else {
				sender.sendMessage(ChatColor.RED + "Invalid query! /tasks <all|my|waiting|approved|rejected|done|closed>");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Found " + tasks.size() + " tasks matching your query:");
			for(Task task : tasks) {
				sender.sendMessage(format(task));
			}
			return true;
		}
		
		if(label.equalsIgnoreCase("taskinfo")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "View information about a task.");
				sender.sendMessage(ChatColor.YELLOW + "/taskinfo <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[0]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "=== Task #" + task.getId() + " ===");
			sender.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.GRAY + task.getName());
			sender.sendMessage(ChatColor.GREEN + "By: " + ChatColor.GRAY + task.getBy());
			sender.sendMessage(ChatColor.GREEN + "Status: " + ChatColor.GRAY + status(task));
			sender.sendMessage(ChatColor.GREEN + "Reviewed By: " + ChatColor.GRAY + (task.getReviewedBy() == null ? "(None)" : task.getReviewedBy()));
			sender.sendMessage(ChatColor.GREEN + "Assignees: " + ChatColor.GRAY + StringUtil.parseList(task.getAssignees()));
			return true;
		}
		
		
		if(label.equalsIgnoreCase("done")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Mark a task as completed.");
				sender.sendMessage(ChatColor.YELLOW + "/done <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[0]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			task.setDone(true);
			sender.sendMessage(ChatColor.GREEN + "Marked task #" + args[0] + " as done. It will be reviewed shortly.");
			
		}
		
		if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.TASK_MANAGER, true)) return true;
		
		if(label.equalsIgnoreCase("approve")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Approve a task.");
				sender.sendMessage(ChatColor.YELLOW + "/approve <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[0]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			task.setApproved(true, user.getName());
			sender.sendMessage(ChatColor.GREEN + "Approved task #" + args[0] + " successfully.");
			Player target = Bukkit.getPlayerExact(task.getBy());
			if(target != null) {
				target.sendMessage(ChatColor.GREEN + "Your task #" + args[0] + " (" + task.getName() + ") was accepted!");
			}
			return true;
		}

		if(label.equalsIgnoreCase("reject")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Reject a task.");
				sender.sendMessage(ChatColor.YELLOW + "/reject <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[0]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			task.setApproved(false, user.getName());
			sender.sendMessage(ChatColor.GREEN + "Rejected task #" + args[0] + " successfully.");
			Player target = Bukkit.getPlayerExact(task.getBy());
			if(target != null) {
				target.sendMessage(ChatColor.RED + "Your task #" + args[0] + " (" + task.getName() + ") was rejected.");
			}
			return true;
		}

		if(label.equalsIgnoreCase("assign")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Assign a player to a task.");
				sender.sendMessage(ChatColor.YELLOW + "/assign <player> <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[1]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			task.addAssignee(args[0]);
			sender.sendMessage(ChatColor.GREEN + "Assigned " + args[0] + " to task #" + args[1] + " successfully.");
			return true;
		}
		
		
		if(label.equalsIgnoreCase("close")) {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.GOLD + "Close a task..");
				sender.sendMessage(ChatColor.YELLOW + "/close <task#>");
				return true;
			}
			Task task = taskLoader.getTaskById(Integer.valueOf(args[0]));
			if(task == null) {
				sender.sendMessage(ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
				return true;
			}
			task.setClosed(true);
			sender.sendMessage(ChatColor.GREEN + "Marked task #" + args[0] + " as closed.");
		}
		
		return true;
	}

	private String status(Task task) {
		return task.getReviewedBy() == null ? "Waiting" :
			(!task.isApproved() ? "Rejected" :
				(!task.isDone() ? "Accepted" :
					(task.isClosed() ? "Closed" : "Done")));
	}
	
	private String format(Task task) {
		return ChatColor.GRAY + "- #" + task.getId() + ": " + task.getName() + " (" + status(task) + ")";
	}
	
}
