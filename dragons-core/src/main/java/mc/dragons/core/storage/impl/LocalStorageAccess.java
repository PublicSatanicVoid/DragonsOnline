package mc.dragons.core.storage.impl;

import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.Identifier;
import mc.dragons.core.storage.StorageAccess;

public class LocalStorageAccess implements StorageAccess {
	private Document data;
	private Identifier id;

	public LocalStorageAccess(Identifier identifier, Document data) {
		this.id = identifier;
		this.data = data;
		this.data.putAll(this.id.getDocument());
	}

	public LocalStorageAccess(GameObjectType type, Document data) {
		this.id = new Identifier(type, UUID.randomUUID());
		this.data = data;
		this.data.putAll(this.id.getDocument());
	}

	public void set(String key, Object value) {
		if (key.equals("type") || key.equals("_id"))
			throw new IllegalArgumentException("Cannot modify type or UUID of storage access once instantiated");
		this.data.append(key, value);
	}

	public void update(Document document) {
		this.data.putAll(document);
	}

	public Object get(String key) {
		return this.data.get(key);
	}

	public Set<Entry<String, Object>> getAll() {
		return this.data.entrySet();
	}

	public Document getDocument() {
		return this.data;
	}

	public Identifier getIdentifier() {
		return this.id;
	}
}
