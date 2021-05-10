package mc.dragons.social.party;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.chat.ChannelHandler;
import mc.dragons.core.gameobject.user.chat.ChatChannel;
import mc.dragons.social.party.PartyLoader.Party;

public class PartyChannelHandler implements ChannelHandler {
	private PartyLoader partyLoader;
	
	public PartyChannelHandler(Dragons instance) {
		partyLoader = instance.getLightweightLoaderRegistry().getLoader(PartyLoader.class);
	}
	
	@Override
	public boolean canHear(User to, User from) {
		Party toParty = partyLoader.of(to);
		Party fromParty = partyLoader.of(from);
		return to.getActiveChatChannels().contains(ChatChannel.PARTY) && toParty != null && toParty.equals(fromParty);
	}

	@Override
	public ChatChannel getChannel() {
		return ChatChannel.GUILD;
	}
}
