package mc.dragons.res;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.res.ResPointLoader.ResPoint;

public class ResUserHook implements UserHook {
	private ResPointLoader resPointLoader;
	
	public ResUserHook(Dragons instance) {
		resPointLoader = instance.getLightweightLoaderRegistry().getLoader(ResPointLoader.class);
	}
	
	@Override
	public void onVerifiedJoin(User user) {
		// Update residence holograms
		Bukkit.getScheduler().runTaskLater(Dragons.getInstance(), () -> {
			for(ResPoint resPoint : resPointLoader.getAllResPoints()) {
				resPointLoader.updateResHologramOn(user, resPoint);
			}
		}, 20L);
		
		// Exit residence if re-joining, to avoid phasing issues
		Document saved = user.getData().get("resExitTo", Document.class);
		if(saved == null) {
			return;
		}
		Location to = StorageUtil.docToLoc(saved);
		user.getPlayer().teleport(to);
		user.getStorageAccess().set("resExitTo", null);
		
	}
}
