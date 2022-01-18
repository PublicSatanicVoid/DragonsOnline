package mc.dragons.anticheat.check;

import java.util.function.Supplier;

import org.bson.Document;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.tools.moderation.report.ReportLoader;
import mc.dragons.tools.moderation.report.ReportLoader.Report;

public class ViolationData {
	public static int REPORT_SPAM_THRESHOLD_MS = 30 * 1000;
	
	private static ReportLoader reportLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(ReportLoader.class);
	private static Table<Check, User, ViolationData> violationData = HashBasedTable.create();
	
	public Check check;
	public User user;
	public double vl;
	public long lastReported = 0L;
	
	public static ViolationData of(Check check, User user) {
		if(!violationData.contains(check, user)) {
			violationData.put(check, user, new ViolationData(check, user));
		}
		return violationData.get(check, user);
	}
	
	public ViolationData(Check check, User user) {
		this.check = check;
		this.user = user;
	}
	
	public Report generateReport(Document data) {
		return reportLoader.fileInternalReport(user, new Document("check", check.getName()).append("vl", vl).append("data", data));
	}
	
	public void raiseVl(double reportThreshold, Supplier<Document> data) {
		user.debug("AC: Raise VL on " + check.getName());
		vl++;
		if(vl > reportThreshold && System.currentTimeMillis() - lastReported > REPORT_SPAM_THRESHOLD_MS) {
			lastReported = System.currentTimeMillis();
			generateReport(data.get());
		}
	}
}
