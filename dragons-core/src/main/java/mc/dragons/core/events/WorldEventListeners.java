package mc.dragons.core.events;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.util.StringUtil;

public class WorldEventListeners implements Listener {
	private Logger LOGGER;
	
	public WorldEventListeners(Dragons instance) {
		LOGGER = instance.getLogger();
	}

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		this.LOGGER
				.finest("Leaves decay event on " + event.getBlock().getType() + " at " + StringUtil.locToString(event.getBlock().getLocation()) + " [" + event.getBlock().getWorld().getName() + "]");
		event.setCancelled(true);
	}

	@EventHandler
	public void onWeather(WeatherChangeEvent e) {
		this.LOGGER.finest("Weather change event in world " + e.getWorld().getName());
		e.setCancelled(e.toWeatherState());
	}

	@EventHandler
	public void onCropTrample(PlayerInteractEvent e) {
		this.LOGGER.finest("Player interact event in world " + e.getPlayer().getWorld().getName());
		if (e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType() == Material.SOIL) {
			this.LOGGER.finest(" - It's a crop trample event! Cancelling.");
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityCropTrample(EntityInteractEvent event) {
		this.LOGGER.finest("Entity interact event in world " + event.getEntity().getWorld());
		if (event.getBlock().getType() == Material.CROPS || event.getBlock().getType() == Material.SOIL)
			event.setCancelled(true);
	}
}
