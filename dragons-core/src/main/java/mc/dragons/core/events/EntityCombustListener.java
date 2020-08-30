package mc.dragons.core.events;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;

public class EntityCombustListener implements Listener {
	@EventHandler
	public void onCombust(EntityCombustEvent event) {
		if (event.getEntityType() == EntityType.ZOMBIE)
			event.setCancelled(true);
	}
}
