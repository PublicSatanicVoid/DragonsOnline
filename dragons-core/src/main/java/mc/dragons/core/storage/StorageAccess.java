package mc.dragons.core.storage;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;

/**
 * Represents a single document of stored data in a key-value format.
 * Depending on the backend, this may be persisted or not.
 * A typical use case is controlling the data of a single game object.
 * 
 * @author Adam
 *
 */
public interface StorageAccess {
	/**
	 * Updates the given key-value pair, or appends it 
	 * if it does not currently exist in this StorageAccess.
	 * 
	 * @param key
	 * @param value
	 */
	void set(String key, Object value);
	
	/**
	 * Updates the key-value pairs given in the document,
	 * or appends them if they do not currently exist in
	 * this StorageAccess.
	 * 
	 * @param document
	 */
	void update(Document document);
	
	/**
	 * Deletes the key-value pair with the specified key.
	 * 
	 * @param key
	 */
	void delete(String key);
	
	/**
	 * 
	 * @param key
	 * @return The value of the given key.
	 */
	Object get(String key);
	
	/**
	 * 
	 * @param <T> The type of the given key.
	 * @param key
	 * @param clazz
	 * @return The value of the given key.
	 */
	<T> T get(String key, Class<? extends T> clazz);
	
	/**
	 * Pulls the value of the given key from the database,
	 * ensuring synchronization.
	 * 
	 * @param <T> The type of the given key.
	 * @param key
	 * @param clazz
	 * @return The synced value of the given key.
	 */
	<T> T pull(String key, Class<? extends T> clazz);
	
	/**
	 * 
	 * @return All data in this StorageAccess as a set of key-value pairs.
	 */
	Set<Entry<String, Object>> getAll();
	
	/**
	 * 
	 * @return All data in this StorageAccess as a document.
	 */
	Document getDocument();
	
	/**
	 * 
	 * @return The unique object identifier for the data controlled by this StorageAccess.
	 */
	Identifier getIdentifier();
}
