package mc.dragons.tools.content.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.mongo.pagination.PaginationUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.COLoader;
import mc.dragons.tools.content.COLoader.COStatus;
import mc.dragons.tools.content.COLoader.CommunityObjective;
import net.md_5.bungee.api.ChatColor;

public class CommunityObjectiveCommands extends DragonsCommandExecutor {
	private static final int PAGE_SIZE = 5;

	private COLoader loader;

	public CommunityObjectiveCommands(COLoader loader) {
		this.loader = loader;
	}

	private CommunityObjective loadObjective(CommandSender sender, String[] args, String cmd) {
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + cmd + " <objective id>");
			return null;
		}
		Integer id = parseInt(sender, args[0]);
		if(id == null) return null;
		CommunityObjective objective = loader.getObjectiveById(id);
		if(objective == null) {
			sender.sendMessage(ChatColor.RED + "No objective by that ID exists! /listobjectives");
			return null;
		}
		return objective;
	}
	
	@SuppressWarnings("unused") // Eclipse is WRONG
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		boolean gm = hasPermission(sender, PermissionLevel.GM);
		if (label.equalsIgnoreCase("listobjectives")) {
			Integer page = args.length == 0 ? 1 : parseInt(sender, args[0]);
			if (page == null) return true;
			List<CommunityObjective> results = gm ? loader.getAllObjectives() : loader.getPublicObjectives();
			int total = results.size();
			results = PaginationUtil.paginateList(gm ? loader.getAllObjectives() : loader.getPublicObjectives(), page, PAGE_SIZE);
			results.sort((a, b) -> a.getStatus().ordinal() - b.getStatus().ordinal());
			if (total == 0) {
				sender.sendMessage(ChatColor.RED + "There are no community objectives to display!");
				return true;
			}
			sender.sendMessage(ChatColor.DARK_GREEN + "There are " + total + " community objectives, showing page "
					+ page + " of " + PaginationUtil.pageCount(total, PAGE_SIZE));
			for(CommunityObjective objective : results) {
				ChatColor color = null;
				switch(objective.getStatus()) {
				case LOCKED:
					color = ChatColor.GRAY;
					break;
				case UNLOCKED:
					color = ChatColor.LIGHT_PURPLE;
					break;
				case COMPLETED:
					color = ChatColor.GREEN;
					break;
				case FAILED:
					color = ChatColor.RED;
					break;
				default:
					break;
				}
				sender.spigot().sendMessage(StringUtil.hoverableText(color + "" + ChatColor.BOLD + "" + objective.getStatus() + " " + color 
						+ objective.getTitle(), objective.getDescription(), ChatColor.GRAY + "ID#" + objective.getId()));
			}
			return true;
		}
		
		if(!requirePermission(sender, PermissionLevel.GM)) return true;
		
		if(label.equalsIgnoreCase("createobjective")) {
			int descChar = StringUtil.getFlagIndex(args, "#", 1);
			if(args.length < 3 || descChar == -1 || descChar == args.length - 1) {
				sender.sendMessage(ChatColor.RED + "/createobjective <objective title> # <objective description>");
				return true;
			}
			CommunityObjective objective = loader.newObjective(StringUtil.concatArgs(args, 0, descChar), StringUtil.concatArgs(args, descChar + 1));
			sender.sendMessage(ChatColor.GREEN + "Objective created successfully. ID: " + objective.getId());
		}
		
		else if(label.equalsIgnoreCase("unlockobjective")) {
			CommunityObjective objective = loadObjective(sender, args, "/unlockobjective");
			if(objective == null) return true;
			objective.setStatus(COStatus.UNLOCKED);
		}
		
		else if(label.equalsIgnoreCase("lockobjective")) {
			CommunityObjective objective = loadObjective(sender, args, "/lockobjective");
			if(objective == null) return true;
			objective.setStatus(COStatus.LOCKED);
		}

		else if(label.equalsIgnoreCase("completeobjective")) {
			CommunityObjective objective = loadObjective(sender, args, "/completeobjective");
			if(objective == null) return true;
			objective.setStatus(COStatus.COMPLETED);
		}

		else if(label.equalsIgnoreCase("failobjective")) {
			CommunityObjective objective = loadObjective(sender, args, "/failobjective");
			if(objective == null) return true;
			objective.setStatus(COStatus.FAILED);
		}

		else if(label.equalsIgnoreCase("deleteobjective")) {
			CommunityObjective objective = loadObjective(sender, args, "/unlockobjective");
			if(objective == null) return true;
			loader.deleteObjective(objective);
			sender.sendMessage(ChatColor.GREEN + "Objective deleted successfully.");
		}
		
		else if(label.equalsIgnoreCase("reloadobjectives")) {
			loader.reloadObjectives();
			sender.sendMessage(ChatColor.GREEN + "Objectives reloaded successfully.");
		}
		
		return true;
	}

}
