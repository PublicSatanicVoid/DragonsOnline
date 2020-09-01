package mc.dragons.res;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.res.ResPointLoader.ResPoint;

public class ResUserHook implements UserHook {

	private ResPointLoader resPointLoader;
	
	public ResUserHook() {
		resPointLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ResPointLoader.class);
	}
	
	@Override
	public void onInitialize(User user) { }

	@Override
	public void onVerifiedJoin(User user) {
		for(ResPoint resPoint : resPointLoader.getAllResPoints()) {
			resPointLoader.updateResHologramOn(user, resPoint);
		}
		Document saved = user.getData().get("resExitTo", Document.class);
		if(saved == null) {
			return;
		}
		Location to = StorageUtil.docToLoc(saved);
		user.getPlayer().teleport(to);
		user.getStorageAccess().set("resExitTo", null);

	}

	@Override
	public void onUpdateState(User user, Location cachedLocation) { }

	@Override
	public void onAutoSave(User user, Document autoSaveData) { }

	@Override
	public void onQuit(User user) { }

}
