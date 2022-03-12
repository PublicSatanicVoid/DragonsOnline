package mc.dragons.core.addon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageManager;

/**
 * Item addons are bound to a specific item class.
 * In addition to the standard properties of addons,
 * item addons can also implement combos, which are
 * sequences of clicks performed while holding the item
 * which execute specific functionality.
 * 
 * <p>Other behavior may also be implemented downstream,
 * but the combo functionality itself is provided by
 * DragonsCore to be used in other modules (e.g.
 * DragonsSpells).
 * 
 * @author Adam
 *
 */
public abstract class ItemAddon implements Addon {
	public static final int MAX_COMBO_TIME_MS = 2000;
	public static final int COMBO_LENGTH = 3;
	
	private Map<User, String> combos;
	private Map<User, Long> comboStartTimes;

	private ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
	private StorageManager storageManager = Dragons.getInstance().getPersistentStorageManager();
	private DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	@Override
	public final AddonType getType() {
		return AddonType.ITEM;
	}

	protected ItemAddon() {
		combos = new HashMap<>();
		comboStartTimes = new HashMap<>();
	}
	
	@Override
	public void apply() {
		storageManager.getAllStorageAccess(GameObjectType.ITEM_CLASS, new Document("addons", new Document("$in", List.of(getName()))))
			.stream()
			.map(storageAccess -> itemClassLoader.loadObject(storageAccess))
			.forEach(itemClass -> {
				LOGGER.debug("Applying item add-on " + getName() + " to item class " + itemClass.getClassName());
				itemClass.reloadAddons();
			});
	}

	/**
	 * Call this whenever a user left-clicks while holding this item.
	 * 
	 * @param user
	 */
	public void onLeftClick(User user) {
		user.debug("ItemAddon received left click");
		if (comboStartTimes.containsKey(user)) {
			if (System.currentTimeMillis() - comboStartTimes.get(user) > MAX_COMBO_TIME_MS) {
				resetCombo(user);
				return;
			}
			combos.put(user, combos.get(user) + "L");
			onPrepareCombo(user, combos.get(user));
			if (combos.get(user).length() == COMBO_LENGTH) {
				onCombo(user, combos.get(user));
				resetCombo(user);
			}
		}
	}

	/**
	 * Call this whenever a user right-clicks while holding this item.
	 * 
	 * @param user
	 */
	public void onRightClick(User user) {
		user.debug("ItemAddon received right click");
		if (!comboStartTimes.containsKey(user)) {
			comboStartTimes.put(user, System.currentTimeMillis());
		}
		if (System.currentTimeMillis() - comboStartTimes.get(user)> MAX_COMBO_TIME_MS) {
			resetCombo(user);
		}
		combos.put(user, combos.getOrDefault(user, "") + "R");
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
			result += "" + ChatColor.LIGHT_PURPLE + combo.charAt(i) + "   ";
		}
		for (; i < COMBO_LENGTH; i++) {
			result += "" + ChatColor.DARK_PURPLE + ChatColor.MAGIC + "_" + ChatColor.RESET + "   ";
		}
		return result.trim();
	}

	/**
	 * Called whenever the user prepares a combo.
	 * 
	 * @param user
	 * @param combo The in-progress combo, e.g. "R", "RL", "RLL"
	 */
	public abstract void onPrepareCombo(User user, String combo);

	/**
	 * Called whenever the user executes a combo.
	 * 
	 * @param user
	 * @param combo The full combo, e.g. "RLL"
	 */
	public abstract void onCombo(User user, String combo);
	
	/**
	 * Called whenever the item is initialized for a user.
	 * Called after super.initialize(Item).
	 * 
	 * <p>May not be called every time an item is initialized,
	 * e.g. if no user is associated. There may also be a delay
	 * from when the item is constructed.
	 * 
	 * @param user
	 * @param item
	 */
	public abstract void initialize(User user, Item item);
}
