package mc.dragons.social;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.social.duel.DuelCommands;
import mc.dragons.social.guild.GuildAdminCommand;
import mc.dragons.social.guild.GuildChannelHandler;
import mc.dragons.social.guild.GuildCommand;
import mc.dragons.social.guild.GuildLoader;
import mc.dragons.social.messaging.PrivateMessageCommands;
import mc.dragons.social.shout.ShoutCommand;

public class DragonsSocial extends JavaPlugin implements CommandExecutor {
	private Dragons dragons;
	private SocialUserHook socialHook;
	
	public void onEnable() {
		dragons = Dragons.getInstance();
		dragons.registerDragonsPlugin(this);
		
		dragons.getLightweightLoaderRegistry().register(new GuildLoader(dragons.getMongoConfig()));
		socialHook = new SocialUserHook();
		dragons.getUserHookRegistry().registerHook(socialHook);
		
		
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
		getCommand("toggleselfmessage").setExecutor(privateMessageCommands);
		
		getCommand("shout").setExecutor(new ShoutCommand());
		getCommand("channel").setExecutor(new ChannelCommand());
	}
	
	public SocialUserHook getSocialHook() {
		return socialHook;
	}
}