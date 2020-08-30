package mc.dragons.tools.content;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.SkillType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class UpdateStatsCommand implements CommandExecutor {

	private UserLoader userLoader;
	
	public UpdateStatsCommand() {
		userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		Player player = null; 
		User user = null;
		
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		}
		
		if(args.length < 3) {
			sender.sendMessage(ChatColor.RED + "Specify a player! /updatestats <Player> <StatName> <Value>");
			sender.sendMessage(ChatColor.RED + "Valid StatNames: xp, level, gold, health, maxHealth, skill.<SkillType>, savedlocation");
			return true;
		}
		
		User target = userLoader.loadObject(args[0]);
		if(target == null) {
			sender.sendMessage(ChatColor.RED + "That user does not exist!");
			return true;
		}
		
		if(args[1].equalsIgnoreCase("xp")) {
			target.setXP(Integer.valueOf(args[2]));
			if(target.getPlayer() != null) {
				target.getPlayer().setHealth(Math.min(target.getPlayer().getHealth(), User.calculateMaxHealth(target.getLevel())));
			}
		}
		if(args[1].equalsIgnoreCase("level")) {
			target.setXP(User.calculateMaxXP(Integer.valueOf(args[2]) - 1));
		}
		else if(args[1].equalsIgnoreCase("gold")) {
			target.setGold(Double.valueOf(args[2]));
		}
		else if(args[1].equalsIgnoreCase("health")) {
			target.getPlayer().setHealth(Double.valueOf(args[2]));
		}
		else if(args[1].equalsIgnoreCase("maxHealth")) {
			if(target.getPlayer() == null) {
				sender.sendMessage(ChatColor.RED + "The specified player must be online to set their max health.");
			}
			target.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Double.valueOf(args[2]));
			sender.sendMessage(ChatColor.YELLOW + "Warning: This change will only persist until the player rejoins.");
		}
		else if(args[1].startsWith("skill.")) {
			String skillName = args[1].replaceAll("skill.", "").toUpperCase();
			try {
				SkillType skill = SkillType.valueOf(skillName);
				user.setSkillProgress(skill, Double.valueOf(args[2]));
			} catch(Exception e) {
				sender.sendMessage(ChatColor.RED + "Invalid skill type! Valid skills are: " + StringUtil.parseList(SkillType.values()));
				return true;
			}
		}
		else if(args[1].equalsIgnoreCase("savedLocation")) {
			if(!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This sub-command requires that you be in-game.");
				return true;
			}
			target.setSavedLocation(player.getLocation());
		}

		sender.sendMessage(ChatColor.GREEN + "Updated player stats successfully.");
		return true;
	}

}
