package mc.dragons.core.addon;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;

import mc.dragons.core.gameobject.user.User;

/**
 * Item addons are bound to a specific item class.
 * In addition to the standard properties of addons,
 * item addons can also implement combos, which are
 * sequences of clicks performed while holding the item
 * which execute specific functionality.
 * 
 * Other behavior may also be implemented downstream,
 * but the combo functionality itself is provided by
 * DragonsCore to be used in other modules (e.g.
 * DragonsSpells).
 * 
 * @author Adam
 *
 */
public abstract class ItemAddon implements Addon {
	private static final int MAX_COMBO_TIME_MS = 2000;
	private static final int COMBO_LENGTH = 3;
	
	private Map<User, String> combos;
	private Map<User, Long> comboStartTimes;

	@Override
	public final AddonType getType() {
		return AddonType.ITEM;
	}

	protected ItemAddon() {
		combos = new HashMap<>();
		comboStartTimes = new HashMap<>();
	}

	public void onLeftClick(User user) {
		user.debug("ItemAddon received left click");
		if (comboStartTimes.containsKey(user)) {
			if (System.currentTimeMillis() - comboStartTimes.get(user).longValue() > MAX_COMBO_TIME_MS) {
				resetCombo(user);
				return;
			}
			combos.put(user, String.valueOf(combos.get(user)) + "L");
			onPrepareCombo(user, combos.get(user));
			if (combos.get(user).length() == 3) {
				onCombo(user, combos.get(user));
				resetCombo(user);
			}
		}
	}

	public void onRightClick(User user) {
		user.debug("ItemAddon received right click");
		if (!comboStartTimes.containsKey(user)) {
			comboStartTimes.put(user, Long.valueOf(System.currentTimeMillis()));
		}
		if (System.currentTimeMillis() - comboStartTimes.get(user).longValue() > MAX_COMBO_TIME_MS) {
			resetCombo(user);
		}
		combos.put(user, String.valueOf(combos.getOrDefault(user, "")) + "R");
		onPrepareCombo(user, combos.get(user));
		if (combos.get(user).length() == COMBO_LENGTH) {
			onCombo(user, combos.get(user));
			resetCombo(user);
		}
	}

	protected void resetCombo(User user) {
		combos.remove(user);
		comboStartTimes.remove(user);
	}

	protected String comboActionBarString(String combo) {
		String result = "";
		int i = 0;
		for (; i < combo.toCharArray().length; i++) {
			result = String.valueOf(result) + ChatColor.LIGHT_PURPLE + combo.charAt(i) + "   ";
		}
		for (; i < COMBO_LENGTH; i++) {
			result = String.valueOf(result) + ChatColor.DARK_PURPLE + ChatColor.MAGIC + "_" + ChatColor.RESET + "   ";
		}
		return result.trim();
	}

	public abstract void onPrepareCombo(User user, String combo);

	public abstract void onCombo(User user, String combo);
}
