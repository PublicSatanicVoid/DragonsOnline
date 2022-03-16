package mc.dragons.anticheat.check.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import mc.dragons.anticheat.DragonsAntiCheat;
import mc.dragons.anticheat.check.Check;
import mc.dragons.anticheat.check.CheckType;
import mc.dragons.anticheat.check.ViolationData;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.tasks.LagMeter;
import mc.dragons.core.util.MathUtil;

public class FastPackets extends Check {
	private static final double VL_LAGBACK_THRESHOLD = 5;
	private static final double VL_REPORT_THRESHOLD = 25;
	private static final double VL_DECAY = 0.95;
	private static final int PPS_INTEGRATION_WINDOW_SEC = 5;
	private static final double TPS_DISABLEBELOW = 16;

	private Map<Player, List<Long>> packetLog = new HashMap<>();
	
	public FastPackets(DragonsAntiCheat plugin) {
		super(plugin);
		setup();
	}

	@Override
	public CheckType getType() {
		return CheckType.MOVING;
	}

	@Override
	public String getName() {
		return "FastPackets";
	}
	
	public double getPPS(Player player) {
		List<Long> packets = packetLog.get(player);
		if(packets == null) return 0;
		return 1000.0 * (double) packets.size() / (packets.get(packets.size() - 1) - packets.get(0));
	}

	@Override
	public void setup() {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, new PacketType[] { 
				PacketType.Play.Client.FLYING, 
				PacketType.Play.Client.POSITION, 
				PacketType.Play.Client.POSITION_LOOK, 
				PacketType.Play.Client.LOOK, 
				PacketType.Play.Client.BOAT_MOVE, 
				PacketType.Play.Client.VEHICLE_MOVE
			}) 
		{
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();
				packetLog.computeIfAbsent(player, p -> new ArrayList<>()).add(System.currentTimeMillis());
				List<Long> packets = packetLog.get(player);
				if(LagMeter.getEstimatedTPS() >= TPS_DISABLEBELOW && packets.size() > 20 * 2) {
					User user = UserLoader.fromPlayer(player);
					ViolationData vdata = ViolationData.of(FastPackets.this, user);
					double pps = 1000.0 * (double) packets.size() / (packets.get(packets.size() - 1) - packets.get(0));
					if(pps > 22) {
						vdata.raiseVl(VL_REPORT_THRESHOLD, () -> new Document("pps", pps));
						if(vdata.vl > VL_LAGBACK_THRESHOLD) {
							if(FastPackets.this.plugin.isDebug()) {
								FastPackets.this.plugin.debug(player, "FastPackets | Lagback (" + MathUtil.round(pps) + "pps, " + MathUtil.round(vdata.vl) + "vl)");
							}
							MoveData.of(user).rubberband();
						}
					}
					else {
						vdata.vl *= VL_DECAY;
					}
				}
				if(packets.size() > 20 * PPS_INTEGRATION_WINDOW_SEC) {
					packets.removeIf(time -> time < System.currentTimeMillis() - PPS_INTEGRATION_WINDOW_SEC * 1000);
				}
			}
		});
	}

	@Override
	public boolean check(User user) {
		ViolationData vdata = ViolationData.of(this, user);
		return vdata.vl < VL_LAGBACK_THRESHOLD;
	}
}
