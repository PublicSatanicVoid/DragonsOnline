package mc.dragons.core.storage.mongo;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;

/**
 * Persistent unit of data storage backed by a MongoDB instance.
 * 
 * @author Adam
 *
 */
public class MongoStorageAccess implements StorageAccess {
	private Identifier identifier;
	private Document document;
	private MongoCollection<Document> collection;

	public MongoStorageAccess(Identifier identifier, Document document, MongoCollection<Document> collection) {
		this.identifier = identifier;
		this.document = document.append("type", identifier.getType().toString()).append("_id", identifier.getUUID());
		this.collection = collection;
	}

	@Override
	public void set(String key, Object value) {
		if (key.equals("type") || key.equals("_id")) {
			throw new IllegalArgumentException("Cannot modify type or UUID of storage access once instantiated");
		}
		document.append(key, value);
		update(new Document(key, value));
	}

	@Override
	public void update(Document document) {
		this.document.putAll(document);
		collection.updateOne(identifier.getDocument(), new Document("$set", document));
	}

	@Override
	public Object get(String key) {
		return document.get(key);
	}

	@Override
	public Set<Entry<String, Object>> getAll() {
		return document.entrySet();
	}

	@Override
	public Document getDocument() {
		return document;
	}

	@Override
	public Identifier getIdentifier() {
		return identifier;
	}
}
