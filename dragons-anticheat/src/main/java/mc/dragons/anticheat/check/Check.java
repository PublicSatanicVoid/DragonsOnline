package mc.dragons.anticheat.check;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public interface Check {
	public String getName();
	public void setup();
	public Report check(User user);
}
