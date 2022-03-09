package mc.dragons.tools.content;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.COLoader.CommunityObjective;

public class ContentUserHook implements UserHook {
	private COLoader loader;
	
	public ContentUserHook(COLoader loader) {
		this.loader = loader;
	}
	
	@Override
	public void onVerifiedJoin(User user) {
		Player player = user.getPlayer();
		List<CommunityObjective> active = loader.getActiveObjectives();
		List<CommunityObjective> completed = loader.getCompletedObjectives();
		for(CommunityObjective objective : active) {
			if(objective.getActivatedOn().before(user.getFirstJoined())) continue;
			player.spigot().sendMessage(StringUtil.hoverableText(
					ChatColor.DARK_PURPLE + "Community Objective: " + ChatColor.LIGHT_PURPLE + objective.getTitle(),
					objective.getDescription()));
		}
		for(CommunityObjective objective : completed) {
			if(objective.getActivatedOn().before(user.getFirstJoined()) || objective.getCompletedOn().before(user.getLastSeen())) continue;
			player.spigot().sendMessage(StringUtil.hoverableText(
					ChatColor.DARK_GREEN + "Community Objective completed since you were last online: " + ChatColor.GREEN + objective.getTitle(),
					objective.getDescription()));
		}
	}
}
