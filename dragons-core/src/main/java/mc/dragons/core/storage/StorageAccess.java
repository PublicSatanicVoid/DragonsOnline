package mc.dragons.core.storage;

import java.util.Map;
import java.util.Set;
import org.bson.Document;

public interface StorageAccess {
	void set(String paramString, Object paramObject);

	void update(Document paramDocument);

	Object get(String paramString);

	Set<Map.Entry<String, Object>> getAll();

	Document getDocument();

	Identifier getIdentifier();
}
