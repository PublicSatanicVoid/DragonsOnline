package mc.dragons.social;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.GuildLoader.Guild;

public class SocialUserHook implements UserHook {
	
	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public void onInitialize(User user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVerifiedJoin(User user) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(DragonsSocialPlugin.class), () -> {		
			List<Guild> guilds = guildLoader.getAllGuildsWith(user.getUUID());
			boolean shown = false;
			for(Guild guild : guilds) {
				if(!guild.getMOTD().equals("")) {
					shown = true;
					user.getPlayer().sendMessage(" ");
					user.getPlayer().sendMessage(guild.getThemeColor().primary() + "" + ChatColor.BOLD + guild.getName());
					user.getPlayer().sendMessage(guild.getThemeColor().secondary() + guild.getMOTD());
				}
			}
			if(shown) {
				user.getPlayer().sendMessage(" ");
			}
		}, 20L);

	}
	
	@Override
	public String getListNameSuffix(User user) {
		List<String> guildNames = guildLoader.getAllGuildsWith(user.getUUID()).stream().map(g ->
			(g.getOwner().equals(user.getUUID()) ? g.getThemeColor().primary() + "*" : "") + g.getThemeColor().tag() + g.getName()).collect(Collectors.toList());
		return ChatColor.DARK_GRAY + "[" + StringUtil.parseList(guildNames, ChatColor.DARK_GRAY + "/") + ChatColor.DARK_GRAY + "]";
	}

}
