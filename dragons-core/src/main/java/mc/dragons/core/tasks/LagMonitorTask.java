package mc.dragons.core.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import mc.dragons.core.Dragons;
import org.bukkit.scheduler.BukkitRunnable;

public class LagMonitorTask extends BukkitRunnable {
	public static final double TPS_RECORD_LENGTH = 3000.0D;

	public static final double TPS_WARN_THRESHOLD = 16.0D;

	public static Logger LOGGER = Dragons.getInstance().getLogger();

	private List<Double> tpsRecord = new ArrayList<>();

	public List<Double> getTPSRecord() {
		return this.tpsRecord;
	}

	public void run() {
		try {
			while (true) {
				Thread.sleep(1000L);
				double tps = LagMeter.getRoundedTPS();
				if (this.tpsRecord.size() >= 3000.0D)
					this.tpsRecord.remove(0);
				this.tpsRecord.add(Double.valueOf(tps));
				if (tps <= 16.0D)
					LOGGER.warning("TPS is unusually low! (" + tps + ")");
			}
		} catch (Exception exception) {
			return;
		}
	}
}
