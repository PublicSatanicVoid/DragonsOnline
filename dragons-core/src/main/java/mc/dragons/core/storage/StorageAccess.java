package mc.dragons.core.storage;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;

public interface StorageAccess {
	void set(String key, Object value);
	void update(Document document);
	Object get(String key);
	Set<Entry<String, Object>> getAll();
	Document getDocument();
	Identifier getIdentifier();
}
