package mc.dragons.tools.content.command.testing;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

public class TestQuestCommand implements CommandExecutor {

	private QuestLoader questLoader;
	//private UserLoader userLoader;
	private GameObjectRegistry registry;
	
	public TestQuestCommand(Dragons instance) {
		questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		registry = instance.getGameObjectRegistry();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.TESTER, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/testquest -listquests");
			sender.sendMessage(ChatColor.YELLOW + "/testquest <QuestName> [-reset|-stage <#>]");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-listquests")) {			
			sender.sendMessage(ChatColor.GREEN + "Listing all quests:");
			for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.QUEST)) {
				Quest quest = (Quest) gameObject;
				sender.sendMessage(ChatColor.GRAY + "- " + quest.getName() + " (" + quest.getQuestName() + ") [Lv " + quest.getLevelMin() + "] [" + quest.getSteps().size() + " steps]" + (quest.isValid() ? "" : ChatColor.RED + " (Incomplete Setup!)"));
			}
			return true;
		}
		
		Quest quest = questLoader.getQuestByName(args[0]);
		if(quest == null) {
			sender.sendMessage(ChatColor.RED + "No quest by that name exists!");
			return true;
		}
		
		if(args.length >= 2) {
			if(args[1].equalsIgnoreCase("-reset")) {
				user.updateQuestProgress(quest, null);
				sender.sendMessage(ChatColor.GREEN + "Erased your progress for quest " + quest.getQuestName() + ".");
				return true;
			}
			
			if(args[1].equalsIgnoreCase("-stage")) {
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Specify a step name to jump to! /testquest <QuestName> -stage <#>");
					return true;
				}
				int stepNo = 0;
				try {
					stepNo = Integer.valueOf(args[2]);
					user.updateQuestProgress(quest, quest.getSteps().get(stepNo));
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Please specify a valid step number!");
				}
				sender.sendMessage(ChatColor.GREEN + "Jumped to step " + stepNo + " of quest " + quest.getQuestName());
				return true;
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
