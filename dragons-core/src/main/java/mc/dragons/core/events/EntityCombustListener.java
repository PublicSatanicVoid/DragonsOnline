package mc.dragons.core.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;

public class EntityCombustListener implements Listener {
	
	/**
	 * Prevent zombies from catching on fire.
	 * 
	 * TODO Make configurable by region and NPC flag.
	 * 
	 * @param event
	 */
	@EventHandler
	public void onCombust(EntityCombustEvent event) {
		event.setCancelled(true);
	}
}
