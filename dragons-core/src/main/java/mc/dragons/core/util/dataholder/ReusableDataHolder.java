package mc.dragons.core.util.dataholder;

/**
 * Utility class intended mainly for passing data through
 * chained async/sync calls where direct assignment is
 * illegal under some circumstances.
 * 
 * @author Adam
 *
 */
public class ReusableDataHolder<T> {
	private T value;
	
	public ReusableDataHolder(T value) {
		this.value = value;
	}
	
	public synchronized T get() {
		return value;
	}
	
	public synchronized void set(T value) {
		this.value = value;
	}
	
	public synchronized void clear() {
		value = null;
	}
}
