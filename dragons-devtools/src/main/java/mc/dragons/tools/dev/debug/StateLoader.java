package mc.dragons.tools.dev.debug;

import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;

public class StateLoader extends AbstractLightweightLoader<UUID> {

	public StateLoader(MongoConfig config) {
		super(config, "#unused#", "state_tokens");
	}
	
	public UUID registerStateToken(Document state) {
		UUID token = UUID.randomUUID();
		collection.insertOne(state.append("_id", token));
		return token;
	}
	
	public Document getState(UUID token) {
		FindIterable<Document> result = collection.find(new Document("_id", token));
		return result.first();
	}

}
