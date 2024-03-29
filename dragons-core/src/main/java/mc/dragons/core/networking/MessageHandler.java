package mc.dragons.core.networking;

import java.util.Date;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.Dragons;

/**
 * Handles inter-server messages of a given type.
 * 
 * @author Adam
 *
 */
public abstract class MessageHandler {
	private Dragons instance;
	private MongoCollection<Document> messages;
	private String type;
	
	public MessageHandler(Dragons instance, String type) {
		messages = instance.getMongoConfig().getDatabase().getCollection(MessageConstants.MESSAGE_COLLECTION);
		this.type = type;
		this.instance = instance;
		instance.getMessageDispatcher().registerHandler(this);
	}
	
	/**
	 * Send a message to a server.
	 * 
	 * @param data The payload to send
	 * @param destServer The server to send it to, or <code>MessageConstants.DEST_ALL</code> for all servers
	 */
	public void send(Document data, String destServer) {
		if(destServer == null) {
			instance.getLogger().warning("Tried to send a message of type " + type + " to a null server");
			return;
		}
		if(destServer.equals(instance.getServerName())) {
			receive(instance.getServerName(), data);
			return;
		}
		if(!instance.getMessageDispatcher().isActive()) return;
		Document message = new Document(MessageConstants.TYPE_FIELD, type)
				.append(MessageConstants.ORIG_FIELD, instance.getServerName())
				.append(MessageConstants.DEST_FIELD, destServer)
				.append(MessageConstants.TIME_FIELD, new Date().getTime())
				.append(MessageConstants.DATA_FIELD, data);
		if(instance.getMessageDispatcher().isDebug()) {
			instance.getLogger().info("Message Sending to " + destServer + ": " + data);
		}
		messages.insertOne(message);
	}
	
	/**
	 * Send a message to all servers.
	 * 
	 * @param data The payload to send
	 */
	public void sendAll(Document data) {
		send(data, MessageConstants.DEST_ALL);
		receive(instance.getServerName(), data);
	}
	
	/**
	 * Process a message sent to this server.
	 * 
	 * @param data The payload sent
	 */
	public abstract void receive(String serverFrom, Document data);
	
	/**
	 * Returns the type of message that this handler
	 * sends and receives.
	 * 
	 * @return
	 */
	public String getMessageType() { return type; }
}
