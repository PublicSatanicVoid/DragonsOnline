package mc.dragons.core.logging;

import java.nio.charset.Charset;
import java.util.logging.Handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.internal.CustomForwardLogHandler;
import mc.dragons.core.logging.internal.CustomLayout;
import mc.dragons.core.logging.internal.LogFilter;
import net.minecrell.terminalconsole.TerminalConsoleAppender;

/**
 * Swaps in a better logging system that allows us to display debug-level
 * messages.
 * 
 * @author Adam
 *
 */
public class CustomLoggingProvider {
	private Dragons dragons;
	private LogFilter logFilter;
	private CustomForwardLogHandler customHandler;

	public CustomLoggingProvider(Dragons instance) {
		dragons = instance;
		logFilter = new LogFilter(instance);
		customHandler = new CustomForwardLogHandler();
	}

	/**
	 * Removes the default logging system and injects a custom logging system that
	 * allows debug-level messages and custom formatting.
	 */
	public void enableCustomLogging() {
		dragons.getLogger().info("Switching to custom logging system...");

		LogLevel.setupLog4jParallels();

		/* Setup JUL handlers */
		enableDebugLogging(java.util.logging.Logger.getLogger(""));

		/* Setup log4j appenders */
		Logger logger = (Logger) LogManager.getRootLogger();
		for (Appender appender : logger.getAppenders().values()) {
			if (appender.getName().equals("TerminalConsole")) {
				logger.removeAppender(appender);
			}
		}
		TerminalConsoleAppender appender = TerminalConsoleAppender.createAppender("TerminalConsole", logFilter, new CustomLayout(dragons, Charset.defaultCharset()), false);
		appender.initialize();
		appender.start();
		logger.addAppender(appender);
	}

	/**
	 * Enables debug logging on the given JUL logger.
	 * 
	 * @param logger
	 */
	public void enableDebugLogging(java.util.logging.Logger logger) {
		logger.setUseParentHandlers(false);
		for (Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}
		logger.addHandler(customHandler);
	}

	/**
	 * 
	 * @return The custom filter for log messages.
	 */
	public LogFilter getCustomLogFilter() {
		return logFilter;
	}
}
