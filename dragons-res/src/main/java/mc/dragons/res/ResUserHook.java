package mc.dragons.res;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.core.storage.StorageUtil;

public class ResUserHook implements UserHook {

	@Override
	public void onInitialize(User user) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onVerifiedJoin(User user) {
		// TODO Auto-generated method stub
		Document saved = user.getData().get("resExitTo", Document.class);
		if(saved == null) {
			return;
		}
		Location to = StorageUtil.docToLoc(saved);
		user.getPlayer().teleport(to);
		user.getStorageAccess().set("resExitTo", null);
	}

	@Override
	public void onUpdateState(User user, Location cachedLocation) {
//		if(!cachedLocation.getWorld().getName().equals("res_temp") && user.getPlayer().getWorld().getName().equals("res_temp")) {
//			List<Residence> owned = ResLoader.getAllResidencesOf(user.getName());
//			if(owned.size() == 0) {
//				user.getPlayer().sendMessage(ChatColor.RED + "You don't have any residences :cry:");
//				return;
//			}
//			ResLoader.goToResidence(user, owned.get(0).getId());
//		}
	}

	@Override
	public void onAutoSave(User user, Document autoSaveData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onQuit(User user) {
		// TODO Auto-generated method stub
		
	}

}
