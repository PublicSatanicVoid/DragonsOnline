package mc.dragons.core.commands;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class MyQuestsCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			User user = UserLoader.fromPlayer((Player) sender);
			sender.sendMessage(ChatColor.GREEN + "Listing active quests:");
			for (Entry<Quest, QuestStep> entry : (Iterable<Entry<Quest, QuestStep>>) user.getQuestProgress().entrySet()) {
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + entry.getKey().getQuestName() + ChatColor.GRAY + " (Step: "
						+ entry.getValue().getStepName().trim() + ChatColor.GRAY + ")");
			}
		}
		return true;
	}
}
