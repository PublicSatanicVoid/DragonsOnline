package mc.dragons.core.gameobject.user.channel_handlers;

import mc.dragons.core.gameobject.user.ChannelHandler;
import mc.dragons.core.gameobject.user.ChatChannel;
import mc.dragons.core.gameobject.user.User;

public class TradeChannelHandler implements ChannelHandler {
	@Override
	public boolean canHear(User to, User from) {
		return to.getActiveChatChannels().contains(ChatChannel.TRADE);
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.TRADE;
	}
}
