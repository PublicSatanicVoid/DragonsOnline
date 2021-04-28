package mc.dragons.core.util.singletons;

import mc.dragons.core.exception.DragonsInternalException;

public class SingletonReInstantiationException extends DragonsInternalException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2969846490523260508L;

	public SingletonReInstantiationException(Class<?> clazz) {
		super("Cannot re-instantiate a singleton instance (" + clazz.getName() + ")", LOGGER.newCID());
	}
}
