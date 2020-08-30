package mc.dragons.core.gameobject.user.channel_handlers;

import mc.dragons.core.gameobject.user.ChannelHandler;
import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class StaffChannelHandler implements ChannelHandler {
	public boolean canHear(User to, User from) {
		return (to.getActiveChatChannels().contains(ChatChannel.STAFF) && PermissionUtil.verifyActivePermissionLevel(to, PermissionLevel.BUILDER, false));
	}

	public ChatChannel getChannel() {
		return ChatChannel.STAFF;
	}
}
