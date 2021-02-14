package mc.dragons.tools.content;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.events.PlayerEventListeners;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.Rank;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;

public class ResetProfileCommand implements CommandExecutor {
	private UserLoader userLoader;
	private ItemLoader itemLoader;
	
	public ResetProfileCommand() {
		userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
		itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();
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
		
		boolean gm = PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false);
		
		User target = user;
		if(args.length != 0) {
			if(!gm) {
				sender.sendMessage(ChatColor.RED + "Only GM+ can reset others' profiles.");
				return true;
			}
			target = userLoader.loadObject(args[0]);
		}
		
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user does not exist!");
			return true;
		}
		
		boolean resetRank = false;
		if(args.length > 1) {
			resetRank = gm && (args[1].equalsIgnoreCase("-r") || args[1].equalsIgnoreCase("-resetrank"));
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
		if(target.equals(user)) {
			sender.sendMessage(ChatColor.GREEN + "Reset your profile.");
		}
		else {
			sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s profile.");
		}
		
		
		return true;
	}
}
