package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestAction;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.StringUtil;

public class QuestDialogueCommands implements CommandExecutor {
	private QuestLoader questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			User user = UserLoader.fromPlayer((Player) sender);
			if (label.equals("fastforwarddialogue")) {
				if (args.length == 0) {
					user.fastForwardDialogue();
					return true;
				}
				if (args[0].equalsIgnoreCase("-one")) {
					user.nextDialogue();
					return true;
				}
			} else if (label.equals("questchoice")) {
				if (args.length < 2) {
					return true;
				}
				String questName = args[0];
				String response = StringUtil.concatArgs(args, 1);
				user.debug("Selected quest choice " + response + " for quest " + questName);
				Quest quest = questLoader.getQuestByName(questName);
				if (quest == null) {
					return true;
				}
				QuestStep progress = user.getQuestProgress().get(quest);
				if (progress == null) {
					return true;
				}
				int actionIndex = user.getQuestActionIndex(quest) - 1;
				if (actionIndex >= progress.getActions().size()) {
					return true;
				}
				QuestAction action = progress.getActions().get(actionIndex);
				if (action == null) {
					return true;
				}
				if (action.getActionType() == QuestAction.QuestActionType.CHOICES) {
					Integer stage = action.getChoices().get(response);
					if (stage == null) {
						return true;
					}
					sender.sendMessage(ChatColor.GRAY + "[1/1] " + ChatColor.DARK_GREEN + user.getName() + ": " + ChatColor.GREEN + response);
					user.setQuestPaused(quest, false);
					user.updateQuestProgress(quest, quest.getSteps().get(stage.intValue()), false);
				}
			}
		}
		return true;
	}
}
