package mc.dragons.dev;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.dev.DiscordNotifier.DiscordRole;
import mc.dragons.dev.TaskLoader.Task;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
public class TaskCommands extends DragonsCommandExecutor {

	private TaskLoader taskLoader;
	private DiscordNotifier buildNotifier;
	
	public TaskCommands() {
		taskLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
		buildNotifier = JavaPlugin.getPlugin(DragonsDevPlugin.class).getBuildNotifier();
	}
	
	private void taskAlert(String message, DiscordRole... roles) {
		UserLoader.allUsers().stream().filter(u -> hasPermission(u, SystemProfileFlag.TASK_MANAGER))
			.map(u -> u.getPlayer())
			.forEach(p -> p.sendMessage(message));
		String notification = "[Task Manager] " + ChatColor.stripColor(message) + " " + buildNotifier.mentionRoles(roles);
		LOGGER.finer("Sending discord notification: content=" + notification);
		buildNotifier.sendNotification(notification);
	}
	
	private Task lookupTask(CommandSender sender, String idString) {
		Integer id = parseIntType(sender, idString);
		if(id == null) return null;
		Task task = lookup(sender, () -> taskLoader.getTaskById(id), ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
		return task;
	}
	
	private void taskCommand(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Create a new task at your current location.");
			sender.sendMessage(ChatColor.YELLOW + "/task <new task description>");
			return;
		}
		String desc = StringUtil.concatArgs(args, 0);
		long words = desc.chars().filter(i -> ((char) i) == ' ').count();
		if(words < 4) {
			sender.sendMessage(ChatColor.YELLOW + "That's a short task name! Please be more detailed.");
			sender.sendMessage(ChatColor.RESET + "/taskhelp" + ChatColor.GRAY + " for help.");
			return;
		}
		boolean mgr = hasPermission(sender, SystemProfileFlag.TASK_MANAGER);
		boolean dev = desc.contains("[Dev]");
		boolean gm = desc.contains("[GM]");
		Task task = taskLoader.addTask(user(sender), desc);
		if(mgr) {
			task.setApproved(true, user(sender));
		}
		String data = "#" + task.getId() + ", " + task.getName() + " (by " + task.getBy().getName() + ")";
		sender.sendMessage(ChatColor.GREEN + "Created task " + ChatColor.UNDERLINE + "#" + task.getId() + ChatColor.GREEN + " successfully!"
				+ ChatColor.ITALIC + " /taskinfo " + task.getId() + ChatColor.GREEN + " to track it." + (mgr ? "As a task manager, your task was automatically approved." : ""));
		TextComponent selfAssign = new TextComponent(ChatColor.GRAY + "[Click to Self-Assign]");
		selfAssign.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Assign yourself to this task").create()));
		selfAssign.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/assign " + sender.getName() + " " + task.getId()));
		sender.spigot().sendMessage(selfAssign);
		if(!mgr) {
			taskAlert(ChatColor.GREEN + "A new task is awaiting approval: " + data, DiscordRole.TASK_MANAGER);
		}
		else if(dev) {
			taskAlert(ChatColor.GREEN + "A new development task has been created and auto-approved: " + data, DiscordRole.DEVELOPER);
		}
		else if(gm) {
			taskAlert(ChatColor.GREEN + "A new GM task has been created and auto-approved: " + data, DiscordRole.GAME_MASTER);
		}
		else {
			taskAlert(ChatColor.GREEN + "A new task has been created and auto-approved: " + data, DiscordRole.BUILDER);
		}
	}
	
	private void taskListCommand(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "List all tasks.");
			sender.sendMessage(ChatColor.YELLOW + "/tasks <all|my|waiting|approved|rejected|done|closed>");
			return;
		}
		List<Task> tasks = null;
		if(args[0].equalsIgnoreCase("all")) {
			tasks = taskLoader.getAllTasks();
		}
		else if(args[0].equalsIgnoreCase("my")) {
			tasks = taskLoader.getAllTasksWith(user(sender));
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
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "Found " + tasks.size() + " tasks matching your query:");
		for(Task task : tasks) {
			sender.sendMessage(format(task));
		}
	}
	
	private void taskInfoCommand(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "View information about a task.");
			sender.sendMessage(ChatColor.YELLOW + "/taskinfo <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		sender.sendMessage(ChatColor.DARK_GREEN + "=== Task #" + task.getId() + " ===");
		sender.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.GREEN + task.getName());
		sender.sendMessage(ChatColor.GRAY + "By: " + ChatColor.GREEN + task.getBy().getName());
		sender.sendMessage(ChatColor.GRAY + "Loc: " + ChatColor.GREEN + StringUtil.locToString(task.getLocation()) + " [" + task.getLocation().getWorld().getName() + "]");
		sender.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + status(task));
		sender.sendMessage(ChatColor.GRAY + "Reviewed By: " + ChatColor.GREEN + (task.getReviewedBy() == null ? "(None)" : task.getReviewedBy().getName()));
		sender.sendMessage(ChatColor.GRAY + "Assignees: " + ChatColor.GREEN + StringUtil.parseList(task.getAssignees().stream().map(u -> u.getName()).collect(Collectors.toList())));
		sender.sendMessage(ChatColor.GRAY + "Notes: ");
		for(String note : task.getNotes()) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + note);
		}
	}
	
	private void markDone(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Mark a task as completed.");
			sender.sendMessage(ChatColor.YELLOW + "/done <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setDone(true);
		sender.sendMessage(ChatColor.GREEN + "Marked task #" + args[0] + " as done. It will be reviewed shortly.");
		taskAlert(ChatColor.GREEN + "Task #" + task.getId() + ", " + task.getName() + " was marked as done and is awaiting review.", DiscordRole.TASK_MANAGER);
	}
	
	private void deleteTaskCommand(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Permanently delete a task. " + ChatColor.ITALIC + "Do not use this for only closing tasks! This is meant for test or mistaken tasks.");
			sender.sendMessage(ChatColor.YELLOW + "/deletetask <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		taskLoader.deleteTask(task);
		sender.sendMessage(ChatColor.GREEN + "Task #" + task.getId() + " " + task.getName() + " was deleted.");
	}
	
	private void approve(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Approve a task.");
			sender.sendMessage(ChatColor.YELLOW + "/approve <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setApproved(true, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Approved task #" + args[0] + " successfully.");
		Player target = Bukkit.getPlayerExact(task.getBy().getName());
		if(target != null) {
			target.sendMessage(ChatColor.GREEN + "Your task #" + args[0] + " (" + task.getName() + ") was accepted!");
		}
		if(task.getName().contains("[Dev]")) {
			taskAlert(ChatColor.GREEN + "Development task #" + task.getId() + " " + task.getName() + " was accepted.", DiscordRole.DEVELOPER);
		}
		else if(task.getName().contains("[GM]")) {
			taskAlert(ChatColor.GREEN + "GM task #" + task.getId() + " " + task.getName() + " was accepted.", DiscordRole.GAME_MASTER);
		}
		else {
			taskAlert(ChatColor.GREEN + "Task #" + task.getId() + " " + task.getName() + " was accepted.", DiscordRole.BUILDER);
		}
	}
	
	private void reject(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Reject a task.");
			sender.sendMessage(ChatColor.YELLOW + "/reject <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setApproved(false, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Rejected task #" + args[0] + " successfully.");
		Player target = Bukkit.getPlayerExact(task.getBy().getName());
		if(target != null) {
			target.sendMessage(ChatColor.RED + "Your task #" + args[0] + " (" + task.getName() + ") was rejected.");
		}
		taskAlert(ChatColor.GREEN + "Task #" + task.getId() + " " + task.getName() + " was rejected.", DiscordRole.BUILDER);
	}
	
	private void assign(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Assign a player to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/assign <player> <task#>");
			return;
		}
		User target = lookupUser(sender, args[0]);
		Task task = lookupTask(sender, args[1]);
		if(task == null || target == null) return;
		task.addAssignee(target);
		sender.sendMessage(ChatColor.GREEN + "Assigned " + args[0] + " to task #" + args[1] + " successfully.");
		taskAlert(ChatColor.GREEN + args[0] + " was assigned to task #" + args[1] + " " + task.getName(), DiscordRole.BUILDER);
	}
	
	private void close(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Close a task.");
			sender.sendMessage(ChatColor.YELLOW + "/close <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setClosed(true);
		sender.sendMessage(ChatColor.GREEN + "Marked task #" + args[0] + " as closed.");
		taskAlert(ChatColor.GREEN + "Task #" + task.getId() + " " + task.getName() + " was marked as closed", DiscordRole.BUILDER);
	}
	
	private void taskLocCommand(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Move the location of a task.");
			sender.sendMessage(ChatColor.YELLOW + "/taskloc <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setLocation(player(sender).getLocation());
		sender.sendMessage(ChatColor.GREEN + "Moved location of task #" + args[0] + " to your current location.");
		taskAlert(ChatColor.GREEN + "Task #" + task.getId() + " " + task.getName() + " was moved to " + StringUtil.locToString(task.getLocation()) + " in world " + task.getLocation().getWorld().getName(), DiscordRole.BUILDER);
	}
	
	private void taskNoteCommand(CommandSender sender, String[] args) {
		if(args.length < 2) {
			sender.sendMessage(ChatColor.GOLD + "Add a note to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/tasknote <task#> <note>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.addNote(sender.getName() + ": " + StringUtil.concatArgs(args, 1));
		sender.sendMessage(ChatColor.GREEN + "Added note to task #" + args[0]);
	}
	
	private void gotoTaskCommand(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Teleport to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/gototask <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		player(sender).teleport(task.getLocation());
		sender.sendMessage(ChatColor.GREEN + "Teleported to task #" + args[0]);
	}
	
	private void toggleDiscordNotifierCommand(CommandSender sender, String[] args) {
		unusedParameter(args);
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		buildNotifier.setEnabled(!buildNotifier.isEnabled());
		sender.sendMessage(ChatColor.GREEN + "Discord build notifier has been " + (buildNotifier.isEnabled() ? "enabled" : "disabled"));
	}
	
	private void discordNotifyRawCommand(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return;
		buildNotifier.sendNotification(StringUtil.concatArgs(args, 0));
		sender.sendMessage(ChatColor.GREEN + "Sent notification successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.BUILDER)) return true;
		
		if(label.equalsIgnoreCase("task")) {
			taskCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("tasks")) {
			taskListCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("taskinfo")) {
			taskInfoCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("done")) {
			markDone(sender, args);
		}
		else if(label.equalsIgnoreCase("approve")) {
			approve(sender, args);
		}
		else if(label.equalsIgnoreCase("reject")) {
			reject(sender, args);
		}
		else if(label.equalsIgnoreCase("assign")) {
			assign(sender, args);
		}
		else if(label.equalsIgnoreCase("close")) {
			close(sender, args);
		}
		else if(label.equalsIgnoreCase("taskloc")) {
			taskLocCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("tasknote")) {
			taskNoteCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("gototask")) {
			gotoTaskCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("deletetask")) {
			deleteTaskCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("togglediscordnotifier")) {
			toggleDiscordNotifierCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("discordnotifyraw")) {
			discordNotifyRawCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("taskhelp")) {
			sender.sendMessage(ChatColor.YELLOW + "/task <task description>" + ChatColor.GRAY + " creates a new task at your location.");
			sender.sendMessage(ChatColor.YELLOW + "/gototask <task#>" + ChatColor.GRAY + " teleports you to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/taskinfo <task#>" + ChatColor.GRAY + " views details about a task.");
			sender.sendMessage(ChatColor.YELLOW + "/tasknote <task#> <note about task>" + ChatColor.GRAY + " adds a note to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/tasks my" + ChatColor.GRAY + " lists all tasks assigned to you.");
			sender.sendMessage(ChatColor.YELLOW + "/done <task#>" + ChatColor.GRAY + " marks a task as done.");
			if(hasPermission(sender, SystemProfileFlag.TASK_MANAGER)) {
				sender.sendMessage(ChatColor.YELLOW + "/approve <task#>" + ChatColor.GRAY + " approves a task.");
				sender.sendMessage(ChatColor.YELLOW + "/reject <task#>" + ChatColor.GRAY + " rejects a task.");
				sender.sendMessage(ChatColor.YELLOW + "/assign <player> <task#>" + ChatColor.GRAY + " assigns a player to a task.");
				sender.sendMessage(ChatColor.YELLOW + "/taskloc <task#>" + ChatColor.GRAY + " moves a task to your location.");
				sender.sendMessage(ChatColor.YELLOW + "/close <task#>" + ChatColor.GRAY + " closes a task.");
				sender.sendMessage(ChatColor.YELLOW + "/deletetask <task#>" + ChatColor.GRAY + " permanently delets a task.");
			}
		}
		
		return true;
	}

	private String status(Task task) {
		if(task.isClosed()) return "Closed";
		if(task.isDone()) return "Done";
		if(task.isApproved()) return "Approved";
		if(task.getReviewedBy() == null) return "Waiting";
		return "Rejected";
	}
	
	private String format(Task task) {
		return ChatColor.GRAY + "- #" + task.getId() + ": " + task.getName() + " (" + status(task) + ")";
	}
	
}
