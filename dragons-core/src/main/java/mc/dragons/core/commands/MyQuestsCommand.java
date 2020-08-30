package mc.dragons.core.commands;

import java.util.Map;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MyQuestsCommand implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			User user = UserLoader.fromPlayer((Player) sender);
			sender.sendMessage(ChatColor.GREEN + "Listing active quests:");
			for (Map.Entry<Quest, QuestStep> entry : (Iterable<Map.Entry<Quest, QuestStep>>) user.getQuestProgress().entrySet())
				sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + ((Quest) entry.getKey()).getQuestName() + ChatColor.GRAY + " (Step: "
						+ ((QuestStep) entry.getValue()).getStepName().trim() + ChatColor.GRAY + ")");
		}
		return true;
	}
}
