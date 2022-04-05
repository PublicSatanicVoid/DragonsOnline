package mc.dragons.dev.addon;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.EntityUtil;

public class BoopStickAddon extends ItemAddon {
	private static final double BOOP_FACTOR = 10.0;
	private static final double BOOP_COUNT = 10;
	private static final String BOOP_MESSAGE = "&7>&8> &5&kff&r &d&l%FROM% &5&lused the &d&lBoop Stick &5&lon &d&l%TO%&5&l!!! &5&kff &8<&7<";
	
	private Dragons instance;
	
	public BoopStickAddon(Dragons plugin) {
		instance = plugin;
	}
	
	@Override
	public String getName() {
		return "SS.BoopStick";
	}
	
	@Override
	public void onRightClick(User user) {
		Player player = user.getPlayer();
		Entity target = EntityUtil.getTarget(player);
		if(target == null || !(target instanceof Player) || UserLoader.fromPlayer((Player) target) == null || !UserLoader.fromPlayer((Player) target).getRank().isStaff()) {
			player.sendMessage(ChatColor.RED + "Right-click a staff member to boop them!");
			return;
		}
		Player pTarget = (Player) target;
		pTarget.setVelocity(pTarget.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(BOOP_FACTOR));
		for(int i = 0; i < BOOP_COUNT; i++) {
			sync(() -> pTarget.setVelocity(new Vector(Math.random() * BOOP_FACTOR, Math.random() * BOOP_FACTOR, Math.random() * BOOP_FACTOR)), 20 * (i + 1));
		}
		sync(() -> player.getInventory().remove(player.getInventory().getItemInMainHand()));
		instance.getInternalMessageHandler().broadcastRawMsg(
				BOOP_MESSAGE.replaceAll(Pattern.quote("%FROM%"), player.getName()).replaceAll(Pattern.quote("%TO%"), pTarget.getName()));
	}
	
	@Override
	public void onPrepareCombo(User user, String combo) { /* do nothing */ }

	@Override
	public void onCombo(User user, String combo) { /* do nothing */ }

	@Override
	public void initialize(User user, Item item) { /* do nothing */ }

}
