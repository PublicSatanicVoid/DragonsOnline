package mc.dragons.replay;

/**
 * A single event within a replay.
 * 
 * @author Adam
 *
 */
public interface ReplayEvent {
	
	/**
	 * Render the event in the replay.
	 */
	public void render(ReplayInstance instance);
}
