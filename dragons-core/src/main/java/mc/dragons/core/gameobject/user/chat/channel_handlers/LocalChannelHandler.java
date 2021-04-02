package mc.dragons.core.gameobject.user.chat.channel_handlers;

import org.bukkit.Location;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChannelHandler;
import mc.dragons.core.gameobject.user.chat.ChatChannel;

public class LocalChannelHandler implements ChannelHandler {
	@Override
	public boolean canHear(User to, User from) {
		Location fromLoc = from.getSavedLocation();
		if(from.getPlayer() != null) {
			fromLoc = from.getPlayer().getLocation();
		}
		
		return to.hasChatSpy() || to.getPlayer().getWorld().equals(fromLoc.getWorld());
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.LOCAL;
	}
}
