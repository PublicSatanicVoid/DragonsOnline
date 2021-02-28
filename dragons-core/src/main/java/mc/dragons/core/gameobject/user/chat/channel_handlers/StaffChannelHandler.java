package mc.dragons.core.gameobject.user.chat.channel_handlers;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChannelHandler;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

public class StaffChannelHandler implements ChannelHandler {
	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.STAFF) && PermissionUtil.verifyActivePermissionLevel(to, PermissionLevel.BUILDER, false);
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.STAFF;
	}
}
