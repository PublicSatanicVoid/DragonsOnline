package mc.dragons.replay;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.bson.Document;

public class ReplayEventDeserializer {
	
	private Map<String, Class<? extends ReplayEvent>> constructors;
	
	/**
	 * Convert a BSON document of a replayable event
	 * into an in-memory ReplayEvent.
	 * 
	 * @param data
	 * @return the deserialized ReplayEvent
	 */
	public ReplayEvent deserialize(Document data) {
		try {
			return constructors.get(data.getString("type")).getConstructor(Document.class).newInstance(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
