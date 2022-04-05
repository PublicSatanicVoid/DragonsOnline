package mc.dragons.core.events;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldEventListeners implements Listener {
	
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
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityCropTrample(EntityInteractEvent event) {
		if (event.getBlock().getType() == Material.WHEAT) {
			event.setCancelled(true);
		}
	}
}
