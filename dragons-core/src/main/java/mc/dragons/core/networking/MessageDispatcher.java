package mc.dragons.core.networking;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.scheduler.BukkitRunnable;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import mc.dragons.core.Dragons;

/**
 * Interfaces with MongoDB to handle inter-server messaging.
 * 
 * @author Adam
 *
 */
public class MessageDispatcher {
	private Dragons instance;
	private boolean debug;
	private boolean active;
	private Map<String, MessageHandler> handlers;
	
	private MongoCollection<Document> messages;
	
	public MessageDispatcher(Dragons instance) {
		this.instance = instance;
		handlers = new HashMap<>();
		debug = false;
		active = true;
		messages = instance.getMongoConfig().getDatabase().getCollection(MessageConstants.MESSAGE_COLLECTION);
	}
	
	public void setDebug(boolean debug) { this.debug = debug; }
	public boolean isDebug() { return debug; }
	
	public void setActive(boolean active) { this.active = active; }
	public boolean isActive() { return active; }
	
	/**
	 * Creates a change stream watching for messages of the given type inbound to this server.
	 * The handler cannot receive any data without being registered here.
	 * Handlers automatically register upon construction.
	 * 
	 * @param handler The handler to register
	 */
	protected void registerHandler(MessageHandler handler) {
		handlers.put(handler.getMessageType(), handler);
		new BukkitRunnable() {
			@Override public void run() {
				ChangeStreamIterable<Document> watcher = messages.watch(List.of(Aggregates.match(
						// type=what we're looking for, AND ( dest=this server OR (dest=all AND origin =/= this server ) )
						Filters.and(Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.TYPE_FIELD, handler.getMessageType()), 
								Filters.or(Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.DEST_FIELD, instance.getServerName()), 
										Filters.and(Filters.eq(MessageConstants.STREAM_PREFIX + MessageConstants.DEST_FIELD, MessageConstants.DEST_ALL),
												Filters.ne(MessageConstants.STREAM_PREFIX + MessageConstants.ORIG_FIELD, instance.getServerName())))))));
				watcher.forEach((Consumer<ChangeStreamDocument<Document>>) d -> {
					if(!active) return;
					if(debug) {
						long latency = new Date().getTime() - d.getFullDocument().getLong("timestamp");
						instance.getLogger().info("Message Received from " + d.getFullDocument().getString(MessageConstants.ORIG_FIELD) 
								+ " (" + latency + "ms) into " + handler.getClass().getSimpleName() + ": " 
								+ d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class).toJson());
					}
					handler.receive(d.getFullDocument().getString(MessageConstants.ORIG_FIELD), d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class));
				});
			}
		}.runTaskAsynchronously(instance);
	}
}
