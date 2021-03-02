package mc.dragons.replay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ReplayEntry {
	private Date begin;
	private Date end;
	
	private Map<Long, List<ReplayEvent>> events; // in ticks since start

	public ReplayInstance getNewReplay() {
		return new ReplayInstance(this);
	}
	
	public Date getBegin() {
		return begin;
	}
	
	public Date getEnd() {
		return end;
	}
	
	public List<ReplayEvent> getEvents(int tickOffset) {
		return events.getOrDefault(tickOffset, new ArrayList<>());
	}
}
