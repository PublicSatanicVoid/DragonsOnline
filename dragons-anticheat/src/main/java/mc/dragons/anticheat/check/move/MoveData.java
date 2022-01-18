package mc.dragons.anticheat.check.move;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import mc.dragons.core.gameobject.user.User;

public class MoveData {
	public static int MAX_MOVE_HISTORY_DEPTH = 20;
	public static long RUBBERBAND_DELAY_MS = 500;
	public static int MIN_VALID_MOVES_TO_SET_VALID_LOCATION = 20;
	
	private static Map<User, MoveData> moveData = new HashMap<>();

	public User user;
	
	public List<MoveEntry> moveHistory;
	
	public Location lastValidLocation;
	public long lastRubberband;
	public int validMoves;
	
	public static class MoveEntry {
		public long time;
		public Location from;
		public Location to;
		
		public MoveEntry(Location from, Location to) {
			time = System.currentTimeMillis();
			this.from = from;
			this.to = to;
		}
		
		public Vector getDelta() {
			return to.toVector().subtract(from.toVector());
		}
	}
	
	public static MoveData of(User user) {
		return moveData.computeIfAbsent(user, u -> new MoveData(user));
	}
	
	public MoveData(User user) {
		moveHistory = new ArrayList<>();
		this.user = user;
	}
	
	public void trimMoveHistory() {
		while(moveHistory.size() > MAX_MOVE_HISTORY_DEPTH) {
			moveHistory.remove(0);
		}
	}
	
	public void rubberband() {
		long now = System.currentTimeMillis();
		if(now - lastRubberband < RUBBERBAND_DELAY_MS) return;
		lastRubberband = now;
		sync(() -> user.getPlayer().teleport(lastValidLocation));
	}
	
	public void invalidMove() {
		validMoves = 0;
	}
	
	public void validMove() {
		validMoves++;
		if(validMoves >= MIN_VALID_MOVES_TO_SET_VALID_LOCATION) {
			lastValidLocation = user.getPlayer().getLocation();
		}
	}
}
