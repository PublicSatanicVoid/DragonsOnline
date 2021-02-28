package mc.dragons.replay;

import java.util.Date;
import java.util.Map;

public class ReplayEntry {
	private Date begin;
	private Date end;
	
	private Map<Long, ReplayEvent> events; // in ticks since start
}
