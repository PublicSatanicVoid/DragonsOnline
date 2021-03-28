package mc.dragons.tools.content.command.statistics;

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
		if(args.length != 0 && requirePermission(sender, PermissionLevel.GM)) {
			target = userLoader.loadObject(args[0]);
		}
		
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user does not exist!");
			return true;
		}
		
		boolean resetRank = false;
		if(args.length > 1) {
			resetRank = hasPermission(sender, PermissionLevel.GM) && (args[1].equalsIgnoreCase("-r") || args[1].equalsIgnoreCase("-resetrank"));
		}
		
		target.setXP(0);
		target.setActivePermissionLevel(PermissionLevel.USER);
		target.setGold(0.0);
		target.setChatSpy(false);
		target.setGodMode(false);
		target.setGameMode(GameMode.ADVENTURE, true);
		target.setDebuggingErrors(false);
		target.clearInventory();
		for(SkillType skill: SkillType.values()) {
			target.setSkillLevel(skill, 0);
			target.setSkillProgress(skill, 0.0);
		}
		for(Quest quest : target.getQuestProgress().keySet()) {
			target.updateQuestProgress(quest, null);
		}
		target.sendToFloor("BeginnerTown");
		for(ItemClass itemClass : PlayerEventListeners.DEFAULT_INVENTORY) {
			target.giveItem(itemLoader.registerNew(itemClass));
		}
		if(resetRank) {
			target.setRank(Rank.DEFAULT);
		}
		target.getPlayer().sendTitle(ChatColor.DARK_RED + "RESET", ChatColor.RED + "Your profile has been reset!", 10, 70, 20);
		target.sendActionBar(ChatColor.DARK_RED + "- Your profile has been reset! -");
		target.debug("Profile forcibly reset");
		if(target.equals(user(sender))) {
			sender.sendMessage(ChatColor.GREEN + "Reset your profile.");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s profile.");
		}
		
		return true;
	}
}
