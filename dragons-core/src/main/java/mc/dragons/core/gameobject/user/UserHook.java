package mc.dragons.core.gameobject.user;

import org.bson.Document;
import org.bukkit.Location;

public interface UserHook {
	void onInitialize(User paramUser);

	void onVerifiedJoin(User paramUser);

	void onUpdateState(User paramUser, Location paramLocation);

	void onAutoSave(User paramUser, Document paramDocument);

	void onQuit(User paramUser);
}
