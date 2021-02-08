package mc.dragons.core.storage;

/**
 * An incremental numbering provider.
 * Each counter is identified by a name, and
 * is incremented independently.
 * 
 * @author Adam
 *
 */
public interface Counter {
	
	/**
	 * 
	 * @param counter
	 * @return The current maximum ID on the given counter.
	 */
	int getCurrentId(String counter);
	
	/**
	 * Reserves the next available ID on the given counter.
	 * 
	 * @param counter
	 * @return The new current maximum ID on the given counter.
	 */
	int reserveNextId(String counter);
}
