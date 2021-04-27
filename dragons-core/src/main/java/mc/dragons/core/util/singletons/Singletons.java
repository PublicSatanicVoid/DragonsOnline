package mc.dragons.core.util.singletons;

import java.util.function.Supplier;

public class Singletons {
	static ClassLocal<Object> singletons = new ClassLocal<>();

	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> clazz, Supplier<T> supplierIfAbsent) {
		T singleton = (T) singletons.get(clazz);
		if(singleton == null) {
			T constructed = (T) supplierIfAbsent.get();
			singletons.setIfAbsent(clazz, constructed);
			singleton = constructed;
		}
		return singleton;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(Class<T> clazz) {
		return (T) singletons.get(clazz);
	}
}
