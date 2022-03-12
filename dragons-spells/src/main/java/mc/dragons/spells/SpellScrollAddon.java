package mc.dragons.spells;

import org.bson.Document;

import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.user.User;

public class SpellScrollAddon extends ItemAddon {

	@Override
	public String getName() {
		return "SpellScroll";
	}

	@Override
	public void initialize(GameObject gameObject) { /* default */ }
	
	@Override
	public void initialize(User user, Item item) { /* default */ }

	@Override
	public void onCombo(User user, String combo) { /* default */ }

	@Override
	public void onPrepareCombo(User user, String combo) { /* default */ }

	@Override
	public void onEnable() { /* default */ }

	@Override
	public void onCreateStorageAccess(Document data) { /* default */ }

}
