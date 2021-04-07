package mc.dragons.core.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.bson.Document;

import com.mongodb.client.MongoCollection;

import mc.dragons.core.Dragons;

/**
 * The custom log4j filter to enable debug-level logging
 * and hide potentially sensitive information from the logs.
 * 
 * This operates at the log4j level rather than the JUL level.
 * To be clear, log messages in Bukkit are routed as follows:
 * 	1) PluginLogger	- Bukkit
 * 	2) java.util.Logger - JUL
 * 	3) org.apache.logging.log4j.core.Logger - log4j
 * 		*We operate here, at a lower level of abstraction
 * 	4) Appenders to log file and console
 * 		*We replace the default appender with Minecrell's
 * 		 TerminalConsoleAppender
 * 	5) Platform-dependent code
 * 
 * @author Adam
 *
 */
public class LogFilter implements Filter {
	private LifeCycle.State state;
	private boolean hideDebugFromOtherLoggers = true;
	
	private MongoCollection<Document> log = Dragons.getInstance().getMongoConfig().getDatabase().getCollection("server_logs");
	private UUID logEntryUUID = UUID.randomUUID();
	private Document logIdentifier = new Document("_id", logEntryUUID);
	
	public LogFilter() {
		log.insertOne(new Document("_id", logEntryUUID).append("instance", Dragons.getInstance().getServerName()).append("logs", new ArrayList<>()));
	}
	
	public UUID getLogEntryUUID() {
		return logEntryUUID;
	}
	
	public static Level fromLog4j(org.apache.logging.log4j.Level level) {
		if (level == org.apache.logging.log4j.Level.ALL) {
			return Level.ALL;
		}
		if (level == org.apache.logging.log4j.Level.FATAL) {
			return Level.SEVERE;
		}
		if (level == org.apache.logging.log4j.Level.ERROR) {
			return Level.SEVERE;
		}
		if (level == org.apache.logging.log4j.Level.WARN) {
			return Level.WARNING;
		}
		if (level == org.apache.logging.log4j.Level.INFO) {
			return Level.INFO;
		}
		if (level == org.apache.logging.log4j.Level.DEBUG) {
			return Level.CONFIG;
		}
		if (level == org.apache.logging.log4j.Level.TRACE) {
			return Level.FINE;
		}
		if (level == org.apache.logging.log4j.Level.ALL) {
			return Level.ALL;
		}
		return Level.OFF;
	}

	public static org.apache.logging.log4j.Level fromJUL(Level level) {
		if (level == Level.ALL) {
			return org.apache.logging.log4j.Level.ALL;
		}
		if (level == Level.SEVERE) {
			return org.apache.logging.log4j.Level.ERROR;
		}
		if (level == Level.WARNING) {
			return org.apache.logging.log4j.Level.WARN;
		}
		if (level == Level.INFO) {
			return org.apache.logging.log4j.Level.INFO;
		}
		if (level == Level.CONFIG) {
			return org.apache.logging.log4j.Level.DEBUG;
		}
		if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
			return org.apache.logging.log4j.Level.TRACE;
		}
		return org.apache.logging.log4j.Level.OFF;
	}

	private Filter.Result process(String message, org.apache.logging.log4j.Level level, String loggerName) {
		if (level.intLevel() > fromJUL(Dragons.getInstance().getServerOptions().getLogLevel()).intLevel()) {
			return Filter.Result.DENY;
		}
		if (hideDebugFromOtherLoggers && !loggerName.equals(Dragons.getInstance().getLogger().getName()) && level.intLevel() >= fromJUL(Level.CONFIG).intLevel()) {
			return Filter.Result.DENY;
		}
		if (!message.contains("issued server command: /syslogon")) {
			log.updateOne(logIdentifier, new Document("$push", new Document("logs", new Document("loggerName", loggerName)
					.append("level", level.toString())
					.append("ts", Instant.now().getEpochSecond())
					.append("message", message))));
			return Filter.Result.NEUTRAL;
		}
		Dragons.getInstance().getLogger().info(String.valueOf(message.substring(0, message.indexOf(" "))) + " accessed the System Logon Authentication Service.");
		return Filter.Result.DENY;
	}

	@Override
	public Filter.Result filter(LogEvent record) {
		if (record == null) {
			return Filter.Result.NEUTRAL;
		}
		if (record.getMessage() == null) {
			return Filter.Result.NEUTRAL;
		}
		String entry = record.getMessage().getFormattedMessage();
		return process(entry, record.getLevel(), record.getLoggerName());
	}

	@Override
	public Filter.Result getOnMatch() {
		return Filter.Result.NEUTRAL;
	}

	@Override
	public Filter.Result getOnMismatch() {
		return Filter.Result.NEUTRAL;
	}

	@Override
	public LifeCycle.State getState() {
		return state;
	}

	@Override
	public void initialize() {
		state = LifeCycle.State.INITIALIZED;
	}

	@Override
	public boolean isStarted() {
		return state == LifeCycle.State.STARTED;
	}

	@Override
	public boolean isStopped() {
		return state == LifeCycle.State.STOPPED;
	}

	@Override
	public void start() {
		state = LifeCycle.State.STARTED;
	}

	@Override
	public void stop() {
		state = LifeCycle.State.STOPPED;
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, Message arg3, Throwable arg4) {
		return process(arg3.getFormattedMessage(), arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9,
			Object arg10) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
			Object arg11) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
	public Filter.Result filter(Logger arg0, org.apache.logging.log4j.Level arg1, Marker arg2, String arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
			Object arg11, Object arg12) {
		return process(arg3, arg1, arg0.getName());
	}

	@Override
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
