package mc.dragons.core.events;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

public class WorldEventListeners implements Listener {
	private DragonsLogger LOGGER;
	
	public WorldEventListeners(Dragons instance) {
		LOGGER = instance.getLogger();
	}

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onWeather(WeatherChangeEvent e) {
		e.setCancelled(e.toWeatherState());
	}

	@EventHandler
	public void onCropTrample(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType() == Material.WHEAT) {
			LOGGER.verbose("Cancelled a crop trample in world " + e.getPlayer().getWorld().getName());
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityCropTrample(EntityInteractEvent event) {
		if (event.getBlock().getType() == Material.WHEAT) {
			LOGGER.verbose("Cancelled an entity crop trample in world " + event.getEntity().getWorld());
			event.setCancelled(true);
		}
	}
}
