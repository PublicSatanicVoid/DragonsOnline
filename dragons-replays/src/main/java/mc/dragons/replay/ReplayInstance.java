package mc.dragons.replay;

import java.util.List;

import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;

public class ReplayInstance {
	
	private ReplayEntry replayEntry;
	private List<User> viewing;
	private int currentTick;
	private boolean started;

	public ReplayInstance(ReplayEntry replayEntry) {
		this.replayEntry = replayEntry;
	}
	
	public void viewReplay(User user) {
		
	}
	
	
	public void play() {
		if(started) return;
		started = true;
		currentTick = 0;
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(Dragons.getInstance(), () -> {
			for(ReplayEvent event : replayEntry.getEvents(currentTick)) {
				event.render(this);
			}
			currentTick++;
		}, 20L, 1L);
	}
	
	public List<User> getViewing() {
		return viewing;
	}
	
	public void terminate() {
		started = false;
	}
}
