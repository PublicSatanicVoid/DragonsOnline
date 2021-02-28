package mc.dragons.social;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChannelHandler;
import mc.dragons.core.gameobject.user.chat.ChatChannel;

public class GuildChannelHandler implements ChannelHandler {

	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.GUILD) 
				&& guildLoader.getAllGuildsWithRaw(to.getUUID()).stream().filter(guildLoader.getAllGuildsWithRaw(from.getUUID())::contains).count() > 0;
	}

	@Override
	public ChatChannel getChannel() {
		// TODO Auto-generated method stub
		return null;
	}

}
