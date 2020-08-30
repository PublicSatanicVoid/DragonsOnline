package mc.dragons.social;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.ChatChannel;

public class DragonsSocialPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {

		getCommand("guild").setExecutor(new GuildCommand());
		getCommand("guildadmin").setExecutor(new GuildAdminCommand());
		
		ChatChannel.GUILD.setHandler(new GuildChannelHandler());
		Dragons.getInstance().getUserHookRegistry().registerHook(new SocialUserHook());
	}
}
