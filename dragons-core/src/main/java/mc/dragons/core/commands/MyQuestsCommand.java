package mc.dragons.core.commands;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestStep;

public class MyQuestsCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender)) return true;
		sender.sendMessage(ChatColor.GREEN + "Listing active quests:");
		for (Entry<Quest, QuestStep> quest : user(sender).getQuestProgress().entrySet()) {
			sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + quest.getKey().getQuestName() 
					+ ChatColor.GRAY + " (Step: " + quest.getValue().getStepName().trim() + ChatColor.GRAY + ")");
		}
		return true;
	}
}
