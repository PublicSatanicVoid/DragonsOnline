package mc.dragons.dev.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.dev.DragonsDev;
import mc.dragons.dev.notifier.DiscordNotifier;
import mc.dragons.dev.notifier.DiscordNotifier.DiscordRole;
import mc.dragons.dev.tasks.TaskLoader.Task;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
public class TaskCommands extends DragonsCommandExecutor {

	private TaskLoader taskLoader;
	private DiscordNotifier buildNotifier;
	
	private static TextComponent space = new TextComponent(" ");
	private static TextComponent options = new TextComponent(ChatColor.GRAY + "  Options: ");
	
	private static final String PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + ">> " + ChatColor.GRAY;
	private static final ChatColor ACCENT_COLOR = ChatColor.YELLOW;
	private static final ChatColor REGULAR_COLOR = ChatColor.GRAY;
	private static final String ACCENT = ACCENT_COLOR + "";
	private static final String REGULAR = REGULAR_COLOR + "";
	
	private static TextComponent approveText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.DARK_GREEN + "[Approve]", "/approve " + id, "Click to approve task #" + id);
	}
	
	private static TextComponent rejectText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.DARK_RED + "[Reject]", "/reject " + id, "Click to reject task #" + id);
	}
	
	private static TextComponent viewText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[View]", "/taskinfo " + id, "Click to view info about task #" + id);
	}
	
	private static TextComponent gotoText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[Go To]", "/gototask " + id, "Click to teleport to task #" + id);
	}
	
	private static TextComponent selfAssignText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[Self-Assign]", "/assign " + id, "Click to assign yourself to task #" + id);
	}
	
	private static TextComponent selfUnassignText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[Self-Unassign]", "/unassign " + id, "Click to remove yourself from task #" + id);
	}
	
	private static TextComponent assignText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[Assign]", "/assign " + id + " ", true, "Click to assign a user to task #" + id);
	}
	
	private static TextComponent doneText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GREEN + "[I'm Done]", "/done " + id, "Click to mark task #" + id + " done and ready for review");
	}
	
	private static TextComponent closeText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GOLD + "[Close]", "/close " + id, "Click to close task #" + id);
	}
	
	private static TextComponent reopenText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.YELLOW + "[Re-Open]", "/reopen " + id, "Click to re-open task #" + id);
	}
	
	private static TextComponent noteText(int id) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[+Add Note]", "/tasknote " + id + " ", true, "Click to add a note to task #" + id);
	}
	
	public TaskCommands(DragonsDev plugin) {
		taskLoader = plugin.getDragonsInstance().getLightweightLoaderRegistry().getLoader(TaskLoader.class);
		buildNotifier = plugin.getBuildNotifier();
	}
	
	private void taskAlert(String message, DiscordRole... roles) {
		taskAlert(new TextComponent(message), roles);
	}

	private void taskAlert(BaseComponent message, DiscordRole... roles) {
		taskAlert(new BaseComponent[] { message }, roles);
	}
	
	private void taskAlert(BaseComponent[] message, DiscordRole... roles) {
		for(BaseComponent c : message) {
			c.setColor(REGULAR_COLOR.asBungee());
		}
		Bukkit.getOnlinePlayers().stream()
			.map(p -> UserLoader.fromPlayer(p))
			.filter(u -> hasPermission(u, SystemProfileFlag.TASK_MANAGER))
			.forEach(u -> u.getPlayer().spigot().sendMessage(message));
		buildNotifier.sendNotification("[Tasks] " + ChatColor.stripColor(BaseComponent.toLegacyText(message)) + " " + buildNotifier.mentionRoles(roles));
	}
	
	private Task lookupTask(CommandSender sender, String idString) {
		Integer id = parseInt(sender, idString);
		if(id == null) return null;
		Task task = lookup(sender, () -> taskLoader.getTaskById(id), PREFIX + ChatColor.RED + "Invalid task number! Do " + ChatColor.ITALIC + "/tasks" + ChatColor.RED + " to list tasks.");
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
		sender.sendMessage(PREFIX + "Created task " + ACCENT + "#" + task.getId() + REGULAR + " successfully!"
				+ ACCENT + ChatColor.ITALIC + " /taskinfo " + task.getId() + REGULAR + " to track it." + (mgr ? " As a task manager, your task was automatically approved." : ""));

		if(!mgr) {
			TextComponent approve = approveText(task.getId());
			TextComponent reject = rejectText(task.getId());
			TextComponent go = gotoText(task.getId());
			TextComponent view = viewText(task.getId());
			taskAlert(mentionTask(task, "A new task is awaiting approval: " + ACCENT + data), DiscordRole.TASK_MANAGER);
			UserLoader.allUsers().stream().filter(u -> hasPermission(u, SystemProfileFlag.TASK_MANAGER)).forEach(u -> {
				u.getPlayer().spigot().sendMessage(options, approve, space, reject, space, view, space, go);
			});
		}
		else if(dev) {
			taskAlert(mentionTask(task, "A new development task has been created and auto-approved: " + ACCENT + data), DiscordRole.DEVELOPER);
		}
		else if(gm) {
			taskAlert(mentionTask(task, "A new GM task has been created and auto-approved: " + ACCENT + data), DiscordRole.GAME_MASTER);
		}
		else {
			taskAlert(mentionTask(task, "A new task has been created and auto-approved: " + ACCENT + data), DiscordRole.BUILDER);
		}
		if(mgr) {
			TextComponent selfAssign = selfAssignText(task.getId());
			TextComponent otherAssign = assignText(task.getId());
			TextComponent go = gotoText(task.getId());
			TextComponent view = viewText(task.getId());
			Bukkit.getOnlinePlayers().stream()
				.map(p -> UserLoader.fromPlayer(p))
				.filter(u -> hasPermission(u, PermissionLevel.BUILDER))
				.forEach(u -> {
					u.getPlayer().spigot().sendMessage(options, selfAssign, space, (hasPermission(u, SystemProfileFlag.TASK_MANAGER) ? otherAssign : new TextComponent()), space, go, space, view);
			});
		}
	}
	
	private void taskListCommand(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "List all tasks.");
			sender.sendMessage(ChatColor.YELLOW + "/tasks <all|my|waiting|approved|assigned|rejected|done|closed> [page#]");
			sender.sendMessage(ChatColor.YELLOW + "/tasks <of|by> <player> [page#]");
			return;
		}
		int pageIndex = 1;
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
			tasks = taskLoader.getAllApprovedUnassignedTasks();
		}
		else if(args[0].equalsIgnoreCase("assigned")) {
			tasks = taskLoader.getAllAssignedInProgressTasks();
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
		else if(args[0].equalsIgnoreCase("of")) {
			User target = lookupUser(sender, args[1]);
			if(target == null) return;
			tasks = taskLoader.getAllTasksWith(target);
			pageIndex = 2;
		}
		else if(args[0].equalsIgnoreCase("by")) {
			User target = lookupUser(sender, args[1]);
			if(target == null) return;
			tasks = taskLoader.getAllTasksBy(target);
			pageIndex = 2;
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid query! /tasks <all|my|waiting|approved|assigned|rejected|done|closed>");
			return;
		}
		if(tasks == null || tasks.size() == 0) {
			sender.sendMessage(ChatColor.RED + "No tasks were returned for this query!");
			return;
		}
		Integer page = 1;
		if(args.length > pageIndex) {
			page = parseInt(sender, args[pageIndex]);
			if(page == null) return;
		}
		int pageSize = 5;
		List<Task> results = PaginationUtil.paginateList(tasks, page, pageSize);
		int pages = (int) Math.ceil((double) tasks.size() / pageSize);
		sender.sendMessage(PREFIX + "Found " + ACCENT + tasks.size() + REGULAR + " tasks matching your query." + ChatColor.GRAY + " [Page " + page + "/" + pages + ". Showing " + results.size() + " Results]");
		for(Task task : results) {
			sender.spigot().sendMessage(mentionTask(task, format(task)));
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
		sender.sendMessage(ChatColor.GOLD + "=== Task #" + task.getId() + " ===");
		TextComponent selfAssign = task.getAssignees().contains(user(sender)) ? selfUnassignText(task.getId()) : selfAssignText(task.getId());
		TextComponent go = gotoText(task.getId());
		TextComponent done = task.isDone() ? reopenText(task.getId()) : doneText(task.getId());
		TextComponent otherAssign = assignText(task.getId());
		otherAssign.addExtra(space);
		TextComponent otherAssignEffective = hasPermission(sender, SystemProfileFlag.TASK_MANAGER) ? otherAssign : new TextComponent();
		TextComponent note = noteText(task.getId());
		sender.spigot().sendMessage(go, space, otherAssignEffective, selfAssign, space, done, space, note);
		sender.sendMessage(REGULAR + "Name: " + ACCENT + task.getName());
		sender.sendMessage(REGULAR + "By: " + ACCENT + task.getBy().getName());
		sender.sendMessage(REGULAR + "Loc: " + ACCENT + StringUtil.locToString(task.getLocation()) + " [" + task.getLocation().getWorld().getName() + "]");
		sender.sendMessage(REGULAR + "Status: " + ACCENT + status(task));
		sender.sendMessage(REGULAR + "Reviewed By: " + ACCENT + (task.getReviewedBy() == null ? "(None)" : task.getReviewedBy().getName()));
		List<BaseComponent> assignees = new ArrayList<>(List.of(new TextComponent(ChatColor.GRAY + "Assignees: ")));
		assignees.addAll(task.getAssignees()
				.stream()
				.map(u -> StringUtil.clickableHoverableText(ACCENT + u.getName() + REGULAR + " [X], ", "/unassign " + task.getId() + " " + u.getName(), "Click to un-assign " + u.getName()))
				.collect(Collectors.toList()));
		sender.spigot().sendMessage(assignees.toArray(new BaseComponent[] {}));
		sender.sendMessage(ChatColor.GRAY + "Notes: ");
		for(String line : task.getNotes()) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + line);
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
		boolean mgr = hasPermission(sender, SystemProfileFlag.TASK_MANAGER);
		if(mgr) {
			task.setClosed(true);
		}
		sender.sendMessage(PREFIX + "Marked task " + ACCENT + "#" + args[0] + REGULAR + " as done." + (mgr ? " Since you're a task manager, it was auto-closed." : " It will be reviewed shortly."));
		taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + REGULAR + ", " + task.getName() + " was marked as done" + (mgr ? " and was auto-closed." : " and is awaiting review.")), DiscordRole.TASK_MANAGER);
		TextComponent reopen = reopenText(task.getId());
		TextComponent close = closeText(task.getId());
		TextComponent go = gotoText(task.getId());
		Bukkit.getOnlinePlayers().stream()
			.map(p -> UserLoader.fromPlayer(p))
			.filter(u -> hasPermission(u, SystemProfileFlag.TASK_MANAGER))
			.forEach(u -> {
				if(mgr) {
					u.getPlayer().spigot().sendMessage(options, reopen, space, go);
				}
				else {
					u.getPlayer().spigot().sendMessage(options, reopen, space, close, space, go);
				}
			});
	}
	
	private void reopen(CommandSender sender, String[] args) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Re-open a completed task.");
			sender.sendMessage(ChatColor.YELLOW + "/reopen <task#>");
			return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null) return;
		task.setDone(false);
		task.setApproved(true, user(sender));
		sender.sendMessage(PREFIX + "Re-opened task " + ACCENT + "#" + args[0] + REGULAR + ".");
		taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + ", " + task.getName() + REGULAR + " was re-opened."), DiscordRole.BUILDER);
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
		sender.sendMessage(PREFIX + "Task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was deleted.");
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
		sender.sendMessage(PREFIX + "Approved task " + ACCENT + "#" + args[0] + REGULAR + " successfully.");
		Player target = Bukkit.getPlayerExact(task.getBy().getName());
		if(target != null) {
			target.sendMessage(PREFIX + "Your task " + ACCENT + "#" + args[0] + " (" + task.getName() + ") " + REGULAR + "was accepted!");
		}
		if(task.getName().contains("[Dev]")) {
			taskAlert(mentionTask(task, "Development task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was accepted."), DiscordRole.DEVELOPER);
		}
		else if(task.getName().contains("[GM]")) {
			taskAlert(mentionTask(task, "GM task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was accepted."), DiscordRole.GAME_MASTER);
		}
		else {
			taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was accepted."), DiscordRole.BUILDER);
		}
		TextComponent selfAssign = selfAssignText(task.getId());
		TextComponent otherAssign = assignText(task.getId());
		TextComponent go = gotoText(task.getId());
		TextComponent view = viewText(task.getId());
		Bukkit.getOnlinePlayers().stream()
			.map(p -> UserLoader.fromPlayer(p))
			.filter(u -> hasPermission(u, PermissionLevel.BUILDER) || u.getPlayer().equals(target)).forEach(u -> {
				u.getPlayer().spigot().sendMessage(options, selfAssign, space, (hasPermission(u, SystemProfileFlag.TASK_MANAGER) ? new TextComponent(otherAssign, space) : new TextComponent()), go, space, view);
		});
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
		sender.sendMessage(PREFIX + "Rejected task " + ACCENT + "#" + args[0] + REGULAR + " successfully.");
		Player target = Bukkit.getPlayerExact(task.getBy().getName());
		if(target != null) {
			target.sendMessage(PREFIX + ChatColor.RED + "Your task #" + args[0] + " (" + task.getName() + ") was rejected.");
		}
		taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was rejected."), DiscordRole.BUILDER);
	}
	
	private void assign(CommandSender sender, String[] args) {
		if(args.length > 1 && !requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Assign a player to a task.");
			sender.sendMessage(ChatColor.YELLOW + "/assign <task#> <user> " + ChatColor.GRAY + "(requires Task Manager role)");
			sender.sendMessage(ChatColor.YELLOW + "/assign <task#> " + ChatColor.GRAY + "to self-assign");
			return;
		}
		User target = user(sender);
		if(args.length > 1) {
			target = lookupUser(sender, args[1]);
			if(target == null) return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null || target == null) return;
		task.addAssignee(target);
		sender.sendMessage(PREFIX + "Assigned " + ACCENT + target.getName() + REGULAR + " to task " + ACCENT + "#" + task.getId() + REGULAR + ".");
		taskAlert(mentionTask(task, ACCENT + target.getName() + REGULAR + " was assigned to task " + ACCENT + "#" + task.getId() + " " + task.getName()), DiscordRole.TASK_MANAGER);
	}
	
	private void unassign(CommandSender sender, String[] args) {
		if(args.length > 1 && !requirePermission(sender, SystemProfileFlag.TASK_MANAGER)) return;
		if(args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Un-assign a player from a task.");
			sender.sendMessage(ChatColor.YELLOW + "/unassign <task#> <user> " + ChatColor.GRAY + "(requires Task Manager role)");
			sender.sendMessage(ChatColor.YELLOW + "/unassign <task#> " + ChatColor.GRAY + "to remove yourself from a task");
			return;
		}
		User target = user(sender);
		if(args.length > 1) {
			target = lookupUser(sender, args[1]);
			if(target == null) return;
		}
		Task task = lookupTask(sender, args[0]);
		if(task == null || target == null) return;
		task.removeAssignee(target);
		sender.sendMessage(PREFIX + "Un-assigned " + ACCENT + target.getName() + REGULAR + " from task " + ACCENT + "#" + task.getId() + REGULAR + ".");
		taskAlert(mentionTask(task, ACCENT + target.getName() + REGULAR + " was un-assigned from task " + ACCENT + "#" + task.getId() + " " + task.getName()), DiscordRole.TASK_MANAGER);
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
		if(!task.isDone()) {
			sender.sendMessage(PREFIX + ChatColor.RED + "This task is not done yet! To mark it as done, do /done " + task.getId());
			return;
		}
		task.setClosed(true);
		sender.sendMessage(PREFIX + "Marked task " + ACCENT + "#" + args[0] + REGULAR + " as closed.");
		taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was marked as closed"), DiscordRole.BUILDER);
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
		sender.sendMessage(PREFIX + "Moved location of task " + ACCENT + "#" + args[0] + REGULAR + " to your current location.");
		taskAlert(mentionTask(task, "Task " + ACCENT + "#" + task.getId() + " " + task.getName() + REGULAR + " was moved to " + ACCENT + StringUtil.locToString(task.getLocation()) + " in world " + task.getLocation().getWorld().getName()), 
				DiscordRole.BUILDER);
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
		sender.sendMessage(PREFIX + "Added note to task " + ACCENT + "#" + args[0]);
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
		sender.sendMessage(PREFIX + "Teleported to task " + ACCENT + "#" + args[0]);
	}
	
	private void toggleDiscordNotifierCommand(CommandSender sender, String[] args) {
		unusedParameter(args);
		if(!requirePermission(sender, PermissionLevel.DEVELOPER)) return;
		buildNotifier.setEnabled(!buildNotifier.isEnabled());
		sender.sendMessage(PREFIX + "Discord build notifier has been " + (buildNotifier.isEnabled() ? "enabled" : "disabled"));
	}
	
	private void discordNotifyRawCommand(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.DEVELOPMENT)) return;
		taskAlert(StringUtil.concatArgs(args, 0), DiscordRole.TASK_MANAGER);
		sender.sendMessage(PREFIX + "Sent notification successfully.");
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
		else if(label.equalsIgnoreCase("unassign")) {
			unassign(sender, args);
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
		else if(label.equalsIgnoreCase("reopen")) {
			reopen(sender, args);
		}
		else if(label.equalsIgnoreCase("togglediscordnotifier")) {
			toggleDiscordNotifierCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("discordnotifyraw")) {
			discordNotifyRawCommand(sender, args);
		}
		else if(label.equalsIgnoreCase("taskhelp")) {
			sender.sendMessage(ACCENT + "/task <task description>" + REGULAR + " creates a new task at your location.");
			sender.sendMessage(ACCENT + "/gototask <task#>" + REGULAR + " teleports you to a task.");
			sender.sendMessage(ACCENT + "/taskinfo <task#>" + REGULAR + " views details about a task.");
			sender.sendMessage(ACCENT + "/tasknote <task#> <note about task>" + REGULAR + " adds a note to a task.");
			sender.sendMessage(ACCENT + "/tasks my" + REGULAR + " lists all tasks assigned to you.");
			sender.sendMessage(ACCENT + "/done <task#>" + REGULAR + " marks a task as done.");
			if(hasPermission(sender, SystemProfileFlag.TASK_MANAGER)) {
				sender.sendMessage(ACCENT + "/approve <task#>" + REGULAR + " approves a task.");
				sender.sendMessage(ACCENT + "/reject <task#>" + REGULAR + " rejects a task.");
				sender.sendMessage(ACCENT + "/assign <task#> <user>" + REGULAR + " assigns a player to a task.");
				sender.sendMessage(ACCENT + "/unassign <task#> <user>" + REGULAR + " un-assigns a palyer from a task.");
				sender.sendMessage(ACCENT + "/taskloc <task#>" + REGULAR + " moves a task to your location.");
				sender.sendMessage(ACCENT + "/close <task#>" + REGULAR + " closes a task.");
				sender.sendMessage(ACCENT + "/reopen <task#>" + REGULAR + " re-opens a task.");
				sender.sendMessage(ACCENT + "/deletetask <task#>" + REGULAR + " permanently delets a task.");
			}
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GOLD + "[View My Tasks]", "/tasks my", "Click to view your open tasks"), space,
					StringUtil.clickableHoverableText(ChatColor.GRAY + "[View In-Progress Tasks]", "/tasks approved", "Click to view all in-progress tasks"));
		}
		
		return true;
	}

	private String status(Task task) {
		if(task.isClosed()) return "Closed";
		if(task.isDone()) return "Done";
		if(task.isApproved() && task.getAssignees().size() > 0) return "Assigned (" + task.getAssignees().size() + ")";
		if(task.isApproved()) return "Approved";
		if(task.getReviewedBy() == null) return "Waiting";
		return "Rejected";
	}
	
	private String getStrikethrough(Task task) {
		return task.isClosed() ? ChatColor.STRIKETHROUGH + "" : "";
	}
	
	private String formatName(Task task) {
		return (getStrikethrough(task) + task.getName()).replaceAll(Pattern.quote("[Dev] "), ChatColor.AQUA + "[Dev] " + ACCENT + getStrikethrough(task))
				.replaceAll(Pattern.quote("[GM] "), ChatColor.GREEN + "[GM] " + ACCENT + getStrikethrough(task));
	}
	
	private String format(Task task) {
		return ChatColor.DARK_GRAY + "#" + ChatColor.GOLD + ChatColor.BOLD + task.getId() + ChatColor.DARK_GRAY + " | " 
			+ ChatColor.YELLOW + formatName(task) + ChatColor.GRAY + " (" + status(task).toUpperCase() + ")";
	}
	
	private TextComponent mentionTask(Task task, String text) {
		TextComponent tc = new TextComponent(text);
		tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to view task #" + task.getId())));
		tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/taskinfo " + task.getId()));
		return tc;
	}
	
}
