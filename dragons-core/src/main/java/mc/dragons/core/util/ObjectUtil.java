package mc.dragons.core.util;

import java.util.function.Supplier;

public class ObjectUtil {
	public static <T, V> T get(V test, Supplier<T> ifPresent, Supplier<T> ifAbsent) {
		if(test == null) return ifAbsent.get();
		return ifPresent.get();
	}
	
	public static <T, V> T get(V test, Supplier<T> ifPresent, T ifAbsent) {
		if(test == null) return ifAbsent;
		return ifPresent.get();
	}
}
