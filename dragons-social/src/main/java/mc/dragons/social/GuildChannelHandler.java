package mc.dragons.social;

import mc.dragons.core.gameobject.user.ChannelHandler;
import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.User;

public class GuildChannelHandler implements ChannelHandler {

	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.STAFF) 
				&& GuildLoader.getAllGuildsWith(to.getUUID()).stream().filter(GuildLoader.getAllGuildsWith(from.getUUID())::contains).count() > 0;
	}

	@Override
	public ChatChannel getChannel() {
		// TODO Auto-generated method stub
		return null;
	}

}
