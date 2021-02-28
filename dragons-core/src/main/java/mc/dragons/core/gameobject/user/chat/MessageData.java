package mc.dragons.core.gameobject.user.chat;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import mc.dragons.core.gameobject.user.User;

public class MessageData {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	private static int idCounter;
	
	private int id;
	private User sender;
	private String when;
	private String message;
	
	public MessageData(User sender, String message) {
		this.id = ++idCounter;
		this.sender = sender;
		this.when = DATE_FORMAT.format(Date.from(Instant.now()));
		this.message = message;
	}
	
	public int getId() { return id; }
	public User getSender() { return sender; }
	public String getWhen() { return when; }
	public String getMessage() { return message; }
}

