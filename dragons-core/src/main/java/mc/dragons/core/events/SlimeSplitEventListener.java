package mc.dragons.core.events;

import static mc.dragons.core.util.BukkitUtil.rollingSync;

import java.util.function.Consumer;

import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SlimeSplitEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.util.HologramUtil;
import mc.dragons.core.util.StringUtil;

public class SlimeSplitEventListener implements Listener {
	private DragonsLogger LOGGER;
	
	public SlimeSplitEventListener(Dragons instance) {
		LOGGER = instance.getLogger();
	}
	
	@EventHandler
	public void onSlimeSplit(SlimeSplitEvent event) {
		Slime slime = event.getEntity();
		LOGGER.trace("Slime split event on " + StringUtil.entityToString(slime));
		if(slime.hasMetadata(HologramUtil.KEY_CLICKABLE_SLIME)) {
			LOGGER.trace("- It's a clickable slime");
			event.setCancelled(true);
			HologramUtil.clickableSlime(slime.getCustomName(), slime.getLocation(), user -> {});
			for(Consumer<User> handler : PlayerEventListeners.getRightClickHandlers(slime)) {	
				LOGGER.trace("- Re-adding handler " + handler + " from plugin " + handler);
				rollingSync(() -> PlayerEventListeners.addRightClickHandler(slime, handler));
			}
			event.setCancelled(true);
		}
	}
}
