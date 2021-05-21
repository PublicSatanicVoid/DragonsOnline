package mc.dragons.core.logging.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mc.dragons.core.logging.LogLevel;

/**
 * Overrides OBC's ForwardLogHandler.
 * 
 * @author Adam
 *
 */
public class CustomForwardLogHandler extends ConsoleHandler {
	private Map<String, Logger> cachedLoggers = new ConcurrentHashMap<>();

	private Logger getLogger(String name) {
		Logger logger = this.cachedLoggers.get(name);
		if (logger == null) {
			logger = LogManager.getLogger(name);
			this.cachedLoggers.put(name, logger);
		}
		return logger;
	}

	@Override
	public void publish(LogRecord record) {
		Logger logger = getLogger(String.valueOf(record.getLoggerName()));
		Throwable exception = record.getThrown();
		Level level = record.getLevel();
		String message = getFormatter().formatMessage(record);
		logger.log(LogLevel.fromJUL(level), message, exception);
	}

	@Override
	public void flush() { /* does nothing */ }

	@Override
	public void close() throws SecurityException { /* does nothing */ }
}
