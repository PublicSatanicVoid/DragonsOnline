package mc.dragons.spells;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserHook;
import mc.dragons.spells.spells.SpellRegistry;
import mc.dragons.spells.util.SpellUtil;

public class SpellUserHook implements UserHook {
	private SpellRegistry registry;
	
	public SpellUserHook(DragonsSpells instance) {
		registry = instance.getSpellRegistry();
	}
	
	@Override
	public void onVerifiedJoin(User user) {
		SpellUtil.updateAllSpellItems(registry, user.getPlayer());
	}
}
