package mc.dragons.core.commands;

import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.loader.FeedbackLoader;
import mc.dragons.core.util.StringUtil;

/**
 * Command for users to report a problem with a quest,
 * for example deadlock.
 * 
 * @author Adam
 *
 */
public class StuckQuestCommand extends DragonsCommandExecutor {
	private FeedbackLoader feedbackLoader = dragons.getLightweightLoaderRegistry().getLoader(FeedbackLoader.class);;
	
	private final String[][] POSSIBLE_ISSUES = {
			{ "Deadlock", "There is no way to advance to the next objective of the quest", "deadlock" },
			{ "Looping", "The same objective or action keeps repeating", "looping" },
			{ "Missing Item or NPC", "An NPC or item that is required for the quest is missing", "missing" },
			{ "Wrong Objective", "You were given an objective that you already completed or that does not make sense", "wrong-objective" }
	};
	
	private void selectQuest(CommandSender sender) {
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please confirm the quest you are having issues with:");
		
		for(Entry<Quest, QuestStep> entry : user(sender).getQuestProgress().entrySet()) {
			if(entry.getValue().getStepName().equalsIgnoreCase("Complete")) continue;
			player(sender).spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + entry.getKey().getQuestName(),
					"/stuckquest " + entry.getKey().getName(), 
					ChatColor.GRAY + "Quest: " + ChatColor.RESET + entry.getKey().getQuestName()));
		}
		
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.GRAY + "Click on one of the quests above to continue with the report.");
		sender.sendMessage(" ");
	}
	
	private void selectIssue(CommandSender sender, String[] args) {
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Please select the issue with this quest:");
		for(String[] issue : POSSIBLE_ISSUES) {
			player(sender).spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + " • " + ChatColor.GREEN + issue[0], 
					"/stuckquest " + args[0] + " " + issue[2], 
					ChatColor.GRAY + issue[1]));
		}
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.GRAY + "Click on one of the issues above to submit the report.");
		sender.sendMessage(" ");
	}
	
	private void submit(CommandSender sender, String[] args) {
		User user = user(sender);
		Quest quest = questLoader.getQuestByName(args[0]);
		if(quest == null) {
			sender.sendMessage(ChatColor.RED + "Invalid quest name! /stuckquest");
			return;
		}
		UUID cid = user.getQuestCorrelationID(quest);
		user.logQuestEvent(quest, Level.INFO, "User reported an issue with the quest: " + args[1]);
		user.logAllQuestData(quest);
		feedbackLoader.addFeedback("SYSTEM", "User " + user.getName() + " reported a problem with a quest. Correlation ID: " + cid);
		sender.sendMessage(" ");
		sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Quest report submitted successfully.");
		sender.sendMessage(ChatColor.YELLOW + "We're sorry you're having issues with this quest.");
		sender.sendMessage(ChatColor.GRAY + "In any follow-up communications with support staff, please include the following message.");
		sender.sendMessage(ChatColor.GRAY + StringUtil.toHdFont("Correlation ID: " + cid));
		sender.sendMessage(" ");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		
		if(args.length == 0) {
			selectQuest(sender);
		}
		else if(args.length == 1) {
			selectIssue(sender, args);
		}	
		else if(args.length == 2) {
			submit(sender, args);
		}
		
		return true;
	}

}
