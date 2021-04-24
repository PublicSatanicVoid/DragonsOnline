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
	private static int idCounter;
	
	private int id;
	private User sender;
	private String when;
	private String message;
	
	public MessageData(User sender, String message) {
		this.id = ++idCounter;
		this.sender = sender;
		this.when = StringUtil.dateFormatNow();
		this.message = message;
	}
	
	public int getId() { return id; }
	public User getSender() { return sender; }
	public String getWhen() { return when; }
	public String getMessage() { return message; }
}

