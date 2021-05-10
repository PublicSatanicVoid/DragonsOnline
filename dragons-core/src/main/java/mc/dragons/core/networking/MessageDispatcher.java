package mc.dragons.core.networking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bson.Document;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.logging.LogLevel;
import mc.dragons.core.util.BukkitUtil;

/**
 * Interfaces with MongoDB to handle inter-server messaging.
 * 
 * @author Adam
 *
 */
public class MessageDispatcher {
	private DragonsLogger LOGGER;
	private boolean debug;
	private boolean active;
	private Map<String, MessageHandler> handlers;
	
	private ChangeStreamIterable<Document> watcher;
	private MongoCollection<Document> messages;
	
	private String streamDestField = MessageConstants.STREAM_PREFIX + MessageConstants.DEST_FIELD;
	private String streamOrigField = MessageConstants.STREAM_PREFIX + MessageConstants.ORIG_FIELD;
	
	public MessageDispatcher(Dragons instance) {
		LOGGER = instance.getLogger();
		String server = instance.getServerName();
		handlers = new HashMap<>();
		debug = false;
		active = true;
		messages = instance.getMongoConfig().getDatabase().getCollection(MessageConstants.MESSAGE_COLLECTION);
		watcher = messages.watch(List.of(Aggregates.match(Filters.or(Filters.eq(streamDestField, server), 
			Filters.and(Filters.eq(streamDestField, MessageConstants.DEST_ALL), Filters.ne(streamOrigField, server))))));
		BukkitUtil.async(() -> {
			watcher.forEach((Consumer<ChangeStreamDocument<Document>>) d -> {
				if(!active) return;
				LOGGER.log(debug ? LogLevel.INFO : LogLevel.DEBUG, "Message Received from " + d.getFullDocument().getString(MessageConstants.ORIG_FIELD)
						+ d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class).toJson());
				MessageHandler handler = handlers.get(d.getFullDocument().getString(MessageConstants.TYPE_FIELD));
				if(handler == null) {
					LOGGER.severe("Could not receive message " + d.getFullDocument().toJson() + ": no handler for this message type exists!");
					return;
				}
				handler.receive(d.getFullDocument().getString(MessageConstants.ORIG_FIELD), d.getFullDocument().get(MessageConstants.DATA_FIELD, Document.class));
			});
		});
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
		LOGGER.debug("Registering message handler for messages of type: " + handler.getMessageType());
		handlers.put(handler.getMessageType(), handler);
	}
}
