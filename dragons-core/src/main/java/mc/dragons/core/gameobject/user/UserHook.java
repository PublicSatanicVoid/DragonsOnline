package mc.dragons.core.gameobject.user;

import org.bson.Document;
import org.bukkit.Location;

public interface UserHook {
	void onInitialize(User user);
	void onVerifiedJoin(User user);
	void onUpdateState(User user, Location location);
	void onAutoSave(User user, Document document);
	void onQuit(User user);
}
