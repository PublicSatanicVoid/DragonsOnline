package mc.dragons.anticheat.event;

import static mc.dragons.core.util.MathUtil.round;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;

public class MoveListener implements Listener {
	private DragonsAntiCheat plugin;
	
	private static final double IMPULSE_STRAIGHT = 0.350001;
	private static final double IMPULSE_DIAGONAL = 0.357147;
	
	private static final double EPSILON_RAD = rad(1.0);

	private List<String> log = new ArrayList<>();
	private boolean logEnabled = false;
	
	
	private static double rad(double deg) {
		return deg * Math.PI / 180;
	}
	
	private static boolean in(double x, double a, double b) {
		return a <= x && x <= b || b <= x && x <= a;
	}
	
	private static enum MoveType { STRAIGHT, DIAGONAL };
	
	
	public void enableLog() {
		logEnabled = true;
	}
	
	public void disableLog() {
		logEnabled = false;
	}
	
	public void clearLog() {
		log.clear();
	}
	
	public void dumpLog() {
		Bukkit.getLogger().info("=== BEGIN ANTICHEAT LOG DUMP ===");
		log.forEach(entry -> Bukkit.getLogger().info(entry));
		Bukkit.getLogger().info("=== END ANTICHEAT LOG DUMP ===");
	}
	
	private void log(String entry) {
		if(!logEnabled) return;
		log.add(entry);
	}
	
	public String getKeyCombo(double angle) {
		String combo = "";
		if(in(angle, rad(-90) + EPSILON_RAD, rad(90) - EPSILON_RAD)) combo += "W";
		else if(!in(Math.abs(angle), rad(90) - EPSILON_RAD, rad(90) + EPSILON_RAD)) combo += "S";
		
		if(!in(angle, -EPSILON_RAD, EPSILON_RAD) && !in(Math.abs(angle), rad(180) - EPSILON_RAD, rad(180) + EPSILON_RAD)) {
			if(angle > 0) combo += "D";
			else combo += "A";
		}
		
		return combo;
	}
	
	public double getImpulse(MoveType moveType) {
		switch(moveType) {
		case STRAIGHT: return IMPULSE_STRAIGHT;
		case DIAGONAL: return IMPULSE_DIAGONAL;
		default: return 0.0;
		}
	}
	
	public class MoveEntry {
		private double dx;
		private double dy;
		private double dz;
		private float pitch;
		private float yaw;
		public MoveEntry(double dx, double dy, double dz, float pitch, float yaw) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.pitch = pitch;
			this.yaw = yaw;
		}
		public double dxz() {
			return Math.sqrt(dx * dx + dz * dz);
		}
		public double dx() { return dx; }
		public double dy() { return dy; }
		public double dz() { return dz; }
		public double pitch() { return pitch; }
		public double yaw() { return yaw; }
	}
	
	
	public class MoveContext {
		private List<MoveEntry> moveHistory = new ArrayList<>();
		
		public MoveContext() {}
		
		public void addEntry(MoveEntry entry) { moveHistory.add(entry); }
		
		public double calculateRecentFactor() {
			if(moveHistory.size() < 3) return 0.0;
			double d0 = moveHistory.get(moveHistory.size() - 3).dxz();
			double d1 = moveHistory.get(moveHistory.size() - 2).dxz();
			double d2 = moveHistory.get(moveHistory.size() - 1).dxz();
			
			return (d1 - d2) / (d0 - d1);
		}
		
		public double calculateRecentImpulse() {
			if(moveHistory.size() < 3) return 0.0;
			double d0 = moveHistory.get(moveHistory.size() - 3).dxz();
			double d1 = moveHistory.get(moveHistory.size() - 2).dxz();
			double d2 = moveHistory.get(moveHistory.size() - 1).dxz();
			return (d0 * d2 - d1 * d1) / (d1 - d2);
		}
	}
	
	private Map<User, MoveContext> contextMap = new HashMap<>();
	
	public MoveListener(DragonsAntiCheat instance) {
		plugin = instance;
	}
	
	private double getSignedAngleXZ(Vector u, Vector v) {
		return Math.atan2(v.getZ(), v.getX()) - Math.atan2(u.getZ(), u.getX());
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		
		Location from = event.getFrom().clone();
		Location to = event.getTo().clone();
		
		if(from == null) return;
		if(from.getWorld() != to.getWorld()) return;

		Player player = event.getPlayer();
		User user = UserLoader.fromPlayer(player);

		MoveContext context = contextMap.computeIfAbsent(user, u -> new MoveContext());
				
		Vector eyes = player.getEyeLocation().getDirection();
		Vector move = to.subtract(from).toVector();
		double dx = move.getX();
		double dy = move.getY();
		double dz = move.getZ();
		
		float pitch = player.getEyeLocation().getPitch();
		float yaw = player.getEyeLocation().getYaw();
		
		double prev_a = context.calculateRecentFactor();
		double prev_b = context.calculateRecentImpulse();
		
		context.moveHistory.add(new MoveEntry(dx, dy, dz, pitch, yaw));
		
		double dxz = Math.sqrt(dx * dx + dz * dz);
		
		double a = context.calculateRecentFactor();
		double b = context.calculateRecentImpulse();
		
		double da = a - prev_a;
		double db = b - prev_b;
		
		double moveAngle = getSignedAngleXZ(eyes, move);
		
		plugin.debug(player, round(moveAngle) + " " + getKeyCombo(moveAngle) + " | " + /* round(dx, 5) + ", " + round(dz, 5) + " | " + */ round(dy, 5) + ", " + round(dxz, 5) + " | a=" + round(a, 5) + ", b=" + round(b, 5) + " | da=" + round(da, 5) + ", db=" + round(db, 5));
		log(moveAngle + "," + dx + "," + dy + "," + dz + "," + a + "," + b + "," + da + "," + db);
	}
	
}
