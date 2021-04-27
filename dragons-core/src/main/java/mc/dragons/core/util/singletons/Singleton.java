package mc.dragons.core.util.singletons;

/**
 * Identifies a class that has only one instance.
 * 
 * <p>A minimal example of a singleton using this interface:
 * 
 * <pre>
  public class Example implements Singleton {
	private Example() {
		// Construction logic for singleton instance
	}
	
	public static Example getInstance() {
		return Singletons.getInstance(Example.class, () -> new Example());
	}
  }
 * </pre>
 * 
 * replacing <code>Example</code> with your singleton class name.
 * 
 * @author Adam
 *
 */
public interface Singleton {
	public static <T extends Singleton> T getInstance() {
		throw new RuntimeException("getInstance() must be overridden by subclass");
	}
}
