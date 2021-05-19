package mc.dragons.tools.moderation.analysis;

import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.tools.moderation.WrappedUser;

public class IPAnalysisUtil {
	private static UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	public static Set<User> scanAlts(StorageManager storageManager, User target) {
		Set<User> alts = storageManager.getAllStorageAccess(GameObjectType.USER, new Document("ipHistory", new Document("$in", target.getIPHistory())))
				.stream()
				.map(storageAccess -> userLoader.loadObject(storageAccess))
				.sorted((u, v) -> WrappedUser.of(u).getPunishmentHistory().size() - WrappedUser.of(v).getPunishmentHistory().size())
				.collect(Collectors.toSet());
		alts.remove(target);
		return alts;
	}
}
