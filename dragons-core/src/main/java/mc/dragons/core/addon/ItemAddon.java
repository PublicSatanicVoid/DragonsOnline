package mc.dragons.core.addon;

import java.util.HashMap;
import java.util.Map;
import mc.dragons.core.gameobject.user.User;
import org.bukkit.ChatColor;

public abstract class ItemAddon implements Addon {
	private static final int MAX_COMBO_TIME_MS = 2000;

	private static final int COMBO_LENGTH = 3;

	private Map<User, String> combos;

	private Map<User, Long> comboStartTimes;

	public final AddonType getType() {
		return AddonType.ITEM;
	}

	protected ItemAddon() {
		this.combos = new HashMap<>();
		this.comboStartTimes = new HashMap<>();
	}

	public void onLeftClick(User user) {
		user.debug("ItemAddon received left click");
		if (this.comboStartTimes.containsKey(user)) {
			if (System.currentTimeMillis() - ((Long) this.comboStartTimes.get(user)).longValue() > MAX_COMBO_TIME_MS) {
				resetCombo(user);
				return;
			}
			this.combos.put(user, String.valueOf(this.combos.get(user)) + "L");
			onPrepareCombo(user, this.combos.get(user));
			if (((String) this.combos.get(user)).length() == 3) {
				onCombo(user, this.combos.get(user));
				resetCombo(user);
			}
		}
	}

	public void onRightClick(User user) {
		user.debug("ItemAddon received right click");
		if (!this.comboStartTimes.containsKey(user))
			this.comboStartTimes.put(user, Long.valueOf(System.currentTimeMillis()));
		if (System.currentTimeMillis() - ((Long) this.comboStartTimes.get(user)).longValue() > MAX_COMBO_TIME_MS)
			resetCombo(user);
		this.combos.put(user, String.valueOf(this.combos.getOrDefault(user, "")) + "R");
		onPrepareCombo(user, this.combos.get(user));
		if (((String) this.combos.get(user)).length() == COMBO_LENGTH) {
			onCombo(user, this.combos.get(user));
			resetCombo(user);
		}
	}

	protected void resetCombo(User user) {
		this.combos.remove(user);
		this.comboStartTimes.remove(user);
	}

	protected String comboActionBarString(String combo) {
		String result = "";
		int i = 0;
		for (; i < (combo.toCharArray()).length; i++)
			result = String.valueOf(result) + ChatColor.LIGHT_PURPLE + combo.charAt(i) + "   ";
		for (; i < COMBO_LENGTH; i++)
			result = String.valueOf(result) + ChatColor.DARK_PURPLE + ChatColor.MAGIC + "_" + ChatColor.RESET + "   ";
		return result.trim();
	}

	public abstract void onPrepareCombo(User paramUser, String paramString);

	public abstract void onCombo(User paramUser, String paramString);
}
