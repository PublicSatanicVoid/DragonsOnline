package mc.dragons.social;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.chat.ChatChannel;

public class DragonsSocialPlugin extends JavaPlugin implements CommandExecutor {
	
	private SocialUserHook socialHook;
	
	public void onEnable() {
		Dragons instance = Dragons.getInstance();
		instance.getLightweightLoaderRegistry().register(new GuildLoader(instance.getMongoConfig()));
		socialHook = new SocialUserHook();
		instance.getUserHookRegistry().registerHook(socialHook);
		
		ChatChannel.GUILD.setHandler(new GuildChannelHandler());
		
		getCommand("guild").setExecutor(new GuildCommand());
		getCommand("guildadmin").setExecutor(new GuildAdminCommand());
		
		DuelCommands duelCommands = new DuelCommands();
		getCommand("duel").setExecutor(duelCommands);
		getCommand("listallduelstatus").setExecutor(duelCommands);
		getCommand("testduelwin").setExecutor(duelCommands);
		
		PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands();
		getCommand("msg").setExecutor(privateMessageCommands);
		getCommand("reply").setExecutor(privateMessageCommands);
		getCommand("chatspy").setExecutor(privateMessageCommands);
		getCommand("shout").setExecutor(new ShoutCommand());
		getCommand("channel").setExecutor(new ChannelCommand());
	}
	
	public SocialUserHook getSocialHook() {
		return socialHook;
	}
}
