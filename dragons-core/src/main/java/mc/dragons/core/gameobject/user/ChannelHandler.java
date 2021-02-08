package mc.dragons.core.gameobject.user;

/**
 * Implements channel-specific functionality to determine who
 * can hear whom on a channel.
 * 
 * When a user speaking on channel X speaks, all users listening
 * on channel X are checked via the associated channel handler
 * to see if they can hear. The rationale behind this method 
 * is that some channels require additional checking; for example,
 * two users may both be on the "Local" channel but are not local
 * relative to each other.
 * 
 * @author Adam
 *
 */
public interface ChannelHandler {
	boolean canHear(User paramUser1, User paramUser2);
	ChatChannel getChannel();
}
