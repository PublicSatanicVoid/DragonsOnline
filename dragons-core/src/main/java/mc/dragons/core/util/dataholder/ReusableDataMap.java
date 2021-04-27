package mc.dragons.core.util.dataholder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class intended mainly for passing data through
 * chained async/sync calls where direct assignment is
 * illegal under some circumstances.
 * 
 * @author Adam
 *
 */
public class ReusableDataMap {
	private Map<String, Object> data = Collections.synchronizedMap(new HashMap<>());
	
	public <T> void set(String key, T value) {
		data.put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) data.get(key);
	}
	
	public void clear() {
		data.clear();
	}
}
