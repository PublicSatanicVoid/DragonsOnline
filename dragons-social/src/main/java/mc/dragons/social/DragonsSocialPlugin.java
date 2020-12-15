package mc.dragons.social;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.ChatChannel;

public class DragonsSocialPlugin extends JavaPlugin implements CommandExecutor {
	
	public void onEnable() {
		Dragons.getInstance().getLightweightLoaderRegistry().register(new GuildLoader(Dragons.getInstance().getMongoConfig()));
		
		ChatChannel.GUILD.setHandler(new GuildChannelHandler());
		
		Dragons.getInstance().getUserHookRegistry().registerHook(new SocialUserHook());
		
		getCommand("guild").setExecutor(new GuildCommand());
		getCommand("guildadmin").setExecutor(new GuildAdminCommand());

		PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands();
		getCommand("msg").setExecutor(privateMessageCommands);
		getCommand("reply").setExecutor(privateMessageCommands);
		getCommand("chatspy").setExecutor(privateMessageCommands);
		getCommand("shout").setExecutor(new ShoutCommand());
		getCommand("channel").setExecutor(new ChannelCommand());
		
		
	}
}
