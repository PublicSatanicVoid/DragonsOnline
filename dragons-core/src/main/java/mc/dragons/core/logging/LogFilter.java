package mc.dragons.core.logging;

import java.util.logging.Level;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

import mc.dragons.core.Dragons;

public class LogFilter implements Filter {
	private LifeCycle.State state;
	private boolean hideDebugFromOtherLoggers = true;

	public static Level fromLog4j(org.apache.logging.log4j.Level level) {
		if (level == org.apache.logging.log4j.Level.ALL)
			return Level.ALL;
		if (level == org.apache.logging.log4j.Level.FATAL)
			return Level.SEVERE;
		if (level == org.apache.logging.log4j.Level.ERROR)
			return Level.SEVERE;
		if (level == org.apache.logging.log4j.Level.WARN)
			return Level.WARNING;
		if (level == org.apache.logging.log4j.Level.INFO)
			return Level.INFO;
		if (level == org.apache.logging.log4j.Level.DEBUG)
			return Level.CONFIG;
		if (level == org.apache.logging.log4j.Level.TRACE)
			return Level.FINE;
		if (level == org.apache.logging.log4j.Level.ALL)
			return Level.ALL;
		return Level.OFF;
	}

	public static org.apache.logging.log4j.Level fromJUL(Level level) {
		if (level == Level.ALL)
			return org.apache.logging.log4j.Level.ALL;
		if (level == Level.SEVERE)
			return org.apache.logging.log4j.Level.ERROR;
		if (level == Level.WARNING)
			return org.apache.logging.log4j.Level.WARN;
		if (level == Level.INFO)
			return org.apache.logging.log4j.Level.INFO;
		if (level == Level.CONFIG)
			return org.apache.logging.log4j.Level.DEBUG;
		if (level == Level.FINE || level == Level.FINER || level == Level.FINEST)
			return org.apache.logging.log4j.Level.TRACE;
		return org.apache.logging.log4j.Level.OFF;
	}

	private Filter.Result process(String message, org.apache.logging.log4j.Level level, String loggerName) {
		if (level.intLevel() > fromJUL(Dragons.getInstance().getServerOptions().getLogLevel()).intLevel())
			return Filter.Result.DENY;
		if (this.hideDebugFromOtherLoggers && !loggerName.equals(Dragons.getInstance().getLogger().getName()) && level.intLevel() >= fromJUL(Level.CONFIG).intLevel())
			return Filter.Result.DENY;
		if (!message.contains("issued server command: /syslogon"))
			return Filter.Result.NEUTRAL;
		Dragons.getInstance().getLogger().info(String.valueOf(message.substring(0, message.indexOf(" "))) + " accessed the System Logon Authentication Service.");
		return Filter.Result.DENY;
	}

	public Filter.Result filter(LogEvent record) {
		if (record == null)
			return Filter.Result.NEUTRAL;
		if (record.getMessage() == null)
			return Filter.Result.NEUTRAL;
		String entry = record.getMessage().getFormattedMessage();
		return process(entry, record.getLevel(), record.getLoggerName());
	}

	public Filter.Result getOnMatch() {
		return Filter.Result.NEUTRAL;
	}

	public Filter.Result getOnMismatch() {
		return Filter.Result.NEUTRAL;
	}

	public LifeCycle.State getState() {
		return this.state;
	}

	public void initialize() {
		this.state = LifeCycle.State.INITIALIZED;
	}

	public boolean isStarted() {
		return (this.state == LifeCycle.State.STARTED);
	}

	public boolean isStopped() {
		return (this.state == LifeCycle.State.STOPPED);
	}

	public void start() {
		this.state = LifeCycle.State.STARTED;
	}

	public void stop() {
		this.state = LifeCycle.State.STOPPED;
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, Message arg3, Throwable arg4) {
		return process(arg3.getFormattedMessage(), arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9,
			Object arg10) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
			Object arg11) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
			Object arg11, Object arg12) {
		return process(arg3, arg1, arg0.getName());
	}

	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
			Object arg11, Object arg12, Object arg13) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object... arg4) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, Object arg3, Throwable arg4) {
		return process("", arg1, arg0.getName());
	}
}
