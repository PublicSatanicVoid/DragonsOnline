package mc.dragons.social;

import org.bson.Document;
import org.bukkit.Location;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;

public class SocialUserHook implements UserHook {
	
	@Override
	public void onInitialize(User user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onVerifiedJoin(User user) {
		
	}

	@Override
	public void onUpdateState(User user, Location cachedLocation) {
		// TODO Auto-generated method stub
		
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
