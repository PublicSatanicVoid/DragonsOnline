package mc.dragons.core.logging;

import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;

import mc.dragons.core.Dragons;
import net.minecrell.terminalconsole.TerminalConsoleAppender;

/**
 * Swaps in a better logging system that allows us to display debug-level messages.
 * 
 * @author Adam
 *
 */
public class CustomLoggingProvider {
	private Dragons dragons;
	private LogFilter logFilter;
	
	public CustomLoggingProvider(Dragons instance) {
		dragons = instance;
		logFilter = new LogFilter(instance);
	}
	
	/**
	 * Removes the default logging system and injects a custom logging system that
	 * allows debug-level messages and custom formatting.
	 */
	public void enableCustomLogging() {
		dragons.getLogger().info("Switching to custom logging system...");
		Logger logger = (Logger) LogManager.getRootLogger();
		for (Appender appender : logger.getAppenders().values()) {
			if (appender.getName().equals("TerminalConsole")) {
				logger.removeAppender(appender);
			}
		}
		TerminalConsoleAppender appender = TerminalConsoleAppender.createAppender("TerminalConsole", logFilter, new CustomLayout(Charset.defaultCharset()), false);
		appender.initialize();
		appender.start();
		logger.addAppender(appender);
	}
	
	/**
	 * 
	 * @return The custom filter for log messages.
	 */
	public LogFilter getCustomLogFilter() {
		return logFilter;
	}
}
