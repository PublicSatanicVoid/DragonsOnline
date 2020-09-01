package mc.dragons.core.gameobject.user.channel_handlers;

import mc.dragons.core.gameobject.user.ChannelHandler;
import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.User;

public class LocalChannelHandler implements ChannelHandler {
	@Override
	public boolean canHear(User to, User from) {
		return to.getPlayer().getWorld().equals(from.getPlayer().getWorld());
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.LOCAL;
	}
}
