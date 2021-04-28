package mc.dragons.core.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;

/**
 * Periodically polls LagMeter to construct a history of TPS
 * which can be queried for performance monitoring.
 * 
 * @author Adam
 *
 */
public class LagMonitorTask extends BukkitRunnable {
	public static final double TPS_RECORD_LENGTH = 3000;
	public static final double TPS_WARN_THRESHOLD = 16.0D;
	public static final double TPS_NETWORK_WARN_THRESHOLD = 12.0D;
	public static final double TPS_NETWORK_WARN_DECAY = 0.8;
	public static final double TPS_NETWORK_MIN_FRAMES = 5;
	public static final double TPS_NETWORK_RESET_FRAMES_BELOW_ACCUMULATOR = 0.3;
	public static final int MIN_NETWORK_WARN_INTERVAL = 5 * 60 * 1000; // 5 minutes
	
	public static Logger LOGGER = Dragons.getInstance().getLogger();

	private List<Double> tpsRecord = new ArrayList<>();
	private long lastNetworkWarn = 0L;
	private double networkAccumulator = 0.0;
	private int frames = 0;
	
	public List<Double> getTPSRecord() {
		return tpsRecord;
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				Thread.sleep(1000L);
				double tps = LagMeter.getRoundedTPS();
				if (tpsRecord.size() >= TPS_RECORD_LENGTH) {
					tpsRecord.remove(0);
				}
//				LOGGER.debug("TPS Monitor: " + tps + ", Acc=" + networkAccumulator + ", Frames=" + frames);
				tpsRecord.add(tps);
				if (tps <= TPS_WARN_THRESHOLD) {
					LOGGER.warning("TPS is unusually low! (" + tps + ")");
				}
				frames++;
				if(tps <= TPS_NETWORK_WARN_THRESHOLD) {
					networkAccumulator = tps;
					long now = System.currentTimeMillis();
					if(now - lastNetworkWarn < MIN_NETWORK_WARN_INTERVAL || frames < TPS_NETWORK_MIN_FRAMES) continue;
					Dragons.getInstance().getStaffAlertHandler().sendLagMessage(tps);
					lastNetworkWarn = now;
				}
				else {
					networkAccumulator *= TPS_NETWORK_WARN_DECAY;
				}
				if(networkAccumulator < TPS_NETWORK_RESET_FRAMES_BELOW_ACCUMULATOR) {
					frames = 0;
				}
			}
		} catch (Exception exception) {
			LOGGER.warning("Exception occurred in lag monitor task (ignored): " + exception.getClass().getCanonicalName() + " (" + exception.getMessage() + ")");
			exception.printStackTrace();
		}
	}
}
