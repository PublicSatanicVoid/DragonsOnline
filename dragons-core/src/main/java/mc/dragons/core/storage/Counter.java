package mc.dragons.core.storage;

public interface Counter {
	int getCurrentId(String counter);
	int reserveNextId(String counter);
}
