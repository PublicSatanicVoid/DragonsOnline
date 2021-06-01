package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestAction;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.StringUtil;

public class QuestDialogueCommands extends DragonsCommandExecutor {

	/* 0=[-one] */
	private void fastForward(CommandSender sender, String[] args) {
		if(args.length == 0) {
			user(sender).fastForwardDialogue();
		}
		else if(args[0].equalsIgnoreCase("-one")) {
			user(sender).nextDialogue();
		}
	}
	
	/* 0=<questName>, 1=<response text> */
	private void questChoice(CommandSender sender, String[] args) {
		if (args.length < 2) {
			return;
		}
		User user = user(sender);
		String questName = args[0];
		String response = StringUtil.concatArgs(args, 1);
		user.debug("Selected quest choice " + response + " for quest " + questName);
		
		// get the current quest action they're on
		Quest quest = questLoader.getQuestByName(questName);
		if (quest == null) return; // invalid quest name
		QuestStep progress = user.getQuestProgress().get(quest);
		if (progress == null) return; // user has not begun quest
		int actionIndex = user.getQuestActionIndex(quest) - 1;
		QuestAction action = progress.getActions().get(actionIndex);
		
		if (action.getActionType() != QuestAction.QuestActionType.CHOICES) return; // not a stage where the user needs to input anything
		
		Integer stage = action.getChoices().get(response); // possible responses are mapped to a new quest stage
		if (stage == null) {
			return;
		}
		sender.sendMessage(ChatColor.GRAY + "[1/1] " + ChatColor.DARK_GREEN + "You: " + ChatColor.GREEN + response);
		user.setQuestPaused(quest, false);
		user.updateQuestProgress(quest, quest.getSteps().get(stage), false);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		if (label.equals("fastforwarddialogue")) {
			fastForward(sender, args);
		} else if (label.equals("questchoice")) {
			questChoice(sender, args);
		}
		
		return true;
	}
}
