package mc.dragons.tools.content.command.testing;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class TestQuestCommand extends DragonsCommandExecutor {
	private GameObjectRegistry registry = dragons.getGameObjectRegistry();
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.TESTER) || !requirePlayer(sender)) return true;
		
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/testquest -listquests");
			sender.sendMessage(ChatColor.YELLOW + "/testquest <QuestName> [-reset|-stage <#>]");
			return true;
		}
	
		else if(args[0].equalsIgnoreCase("-listquests")) {			
			sender.sendMessage(ChatColor.GREEN + "Listing all quests:");
			for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.QUEST)) {
				Quest quest = (Quest) gameObject;
				sender.sendMessage(ChatColor.GRAY + "- " + quest.getName() + " (" + quest.getQuestName() + ") [Lv " + quest.getLevelMin() + "] [" + quest.getSteps().size() + " steps]" + (quest.isValid() ? "" : ChatColor.RED + " (Incomplete Setup!)"));
			}
			return true;
		}
		
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return true;
		
		if(quest.isLocked() && !hasPermission(sender, PermissionLevel.GM)) {
			sender.sendMessage(ChatColor.RED + "That quest is currently locked! Try again later.");
		}
		
		if(args.length >= 2) {
			if(args[1].equalsIgnoreCase("-reset")) {
				user.removeQuest(quest);
				sender.sendMessage(ChatColor.GREEN + "Erased your progress for quest " + quest.getQuestName() + ".");
			}
			else if(args[1].equalsIgnoreCase("-stage")) {
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Specify a step name to jump to! /testquest <QuestName> -stage <#>");
					return true;
				}
				Integer stepNo = parseInt(sender, args[2]);
				if(stepNo == null) return true;
				user.updateQuestProgress(quest, quest.getSteps().get(stepNo));
				sender.sendMessage(ChatColor.GREEN + "Jumped to step " + stepNo + " of quest " + quest.getQuestName());
			}
		}
		
		if(!quest.isValid()) {
			sender.sendMessage(ChatColor.RED + "Warning: This quest is invalid or incomplete and may not work as expected.");
		}
		
		user.updateQuestProgress(quest, quest.getSteps().get(0));
		sender.sendMessage(ChatColor.GREEN + "Began testing quest " + quest.getName() + " (" + quest.getQuestName() + ")");
		return true;
	}

}
