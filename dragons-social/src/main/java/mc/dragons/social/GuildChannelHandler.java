package mc.dragons.social;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.ChannelHandler;
import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.User;

public class GuildChannelHandler implements ChannelHandler {

	private GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	
	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.STAFF) 
				&& guildLoader.getAllGuildsWith(to.getUUID()).stream().filter(guildLoader.getAllGuildsWith(from.getUUID())::contains).count() > 0;
	}

	@Override
	public ChatChannel getChannel() {
		// TODO Auto-generated method stub
		return null;
	}

}
