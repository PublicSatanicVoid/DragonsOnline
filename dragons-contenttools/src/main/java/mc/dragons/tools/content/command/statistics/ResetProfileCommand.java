package mc.dragons.tools.content.command.statistics;

import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class ResetProfileCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.TESTER)) return true;
		
		User target = user(sender);
		if(args.length != 0 && requirePermission(sender, PermissionLevel.ADMIN)) {
			target = lookupUser(sender, args[0]);
		}
		
		if(target == null) return true;
		boolean resetRank = false;
		if(args.length > 1) {
			resetRank = hasPermission(sender, PermissionLevel.ADMIN) && (args[1].equalsIgnoreCase("-r") || args[1].equalsIgnoreCase("-resetrank"));
		}
		
		target.setXP(0);
		target.setActivePermissionLevel(PermissionLevel.USER);
		target.setGold(0.0);
		target.setChatSpy(false);
		target.setGodMode(false);
		target.setGameMode(GameMode.ADVENTURE, true);
//		target.setDebuggingErrors(false);
		target.setStreamConsoleLevel(Level.OFF);
		target.clearInventory();
		for(SkillType skill: SkillType.values()) {
			target.setSkillLevel(skill, 0);
			target.setSkillProgress(skill, 0.0);
		}
		for(Quest quest : new HashSet<>(target.getQuestProgress().keySet())) {
			target.updateQuestProgress(quest, null);
		}
		target.sendToFloor("BeginnerTown");
		for(ItemClass itemClass : PlayerEventListeners.DEFAULT_INVENTORY) {
			target.giveItem(itemLoader.registerNew(itemClass));
		}
		if(resetRank) {
			target.setRank(Rank.DEFAULT);
		}
		target.getPlayer().sendTitle(ChatColor.DARK_RED + "RESET", ChatColor.RED + "Your stats have been wiped!", 10, 70, 20);
		target.sendActionBar(ChatColor.DARK_RED + "- Your stats have been wiped! -");
		target.debug("Profile forcibly reset");
		if(target.equals(user(sender))) {
			sender.sendMessage(ChatColor.GREEN + "Wiped your stats.");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Wiped " + target.getName() + "'s stats.");
		}
		
		return true;
	}
}
