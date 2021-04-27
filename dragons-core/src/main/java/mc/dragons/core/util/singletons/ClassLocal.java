package mc.dragons.core.util.singletons;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores separate copies of an object per-class.
 * 
 * <p>Particularly useful for static subclassing.
 * 
 * @author Adam
 *
 * @param <T> The object type that is stored per-class.
 */
public class ClassLocal<T> {
	private Map<Class<?>, T> values = new HashMap<>();
	
	public void set(Class<?> clazz, T value) {
		values.put(clazz, value);
	}
	
	public void setIfAbsent(Class<?> clazz, T value) {
		if(!values.containsKey(clazz)) {
			set(clazz, value);
		}
	}
	
	public T get(Class<?> clazz) {
		return values.get(clazz);
	}
}
