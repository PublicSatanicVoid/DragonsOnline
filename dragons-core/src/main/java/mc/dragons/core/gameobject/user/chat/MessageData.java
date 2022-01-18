package mc.dragons.core.gameobject.user.chat;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.StringUtil;

/**
 * Log entry for the local chat registry.
 * 
 * @author Adam
 *
 */
public class MessageData {
	private static int idCounter = 0;
	
	private int id;
	private User sender;
	private User to;
	private String when;
	private String message;
	private ChatChannel channel;
	
	public MessageData(User sender, String message) {
		this.id = ++idCounter;
		this.sender = sender;
		this.when = StringUtil.dateFormatNow();
		this.message = message;
		this.channel = sender.getSpeakingChannel();
	}
	
	public MessageData(User sender, User to, String message) {
		this(sender, message);
		this.to = to;
		this.channel = null;
	}
	
	public int getId() { return id; }
	public User getSender() { return sender; }
	public User getTo() { return to; }
	public boolean isPrivate() { return to != null; }
	public String getWhen() { return when; }
	public String getMessage() { return message; }
	public ChatChannel getChannel() { return channel; }
}

