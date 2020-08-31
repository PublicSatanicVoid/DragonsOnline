package mc.dragons.core.gameobject.user;

public interface ChannelHandler {
	boolean canHear(User paramUser1, User paramUser2);
	ChatChannel getChannel();
}
