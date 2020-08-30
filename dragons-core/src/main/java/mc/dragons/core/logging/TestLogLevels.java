package mc.dragons.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.logging.log4j.LogManager;
import org.bukkit.plugin.Plugin;

public class TestLogLevels {
	private static final Level[] LEVEL_VALUES = new Level[] { Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL };

	public static void onEnable(Plugin plugin) {
		Logger pluginLogger = plugin.getLogger();
		Logger rootLogger = Logger.getLogger("");
		Logger log4jRootLogger = (Logger) LogManager.getRootLogger();
		Logger pluginLog4jLogger = (Logger) LogManager.getLogger(pluginLogger.getName());
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		System.out.println("Setting plugin logger level to: " + Level.ALL);
		pluginLogger.setLevel(Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		System.out.println("Setting plugin log4j logger level to: " + Level.ALL);
		pluginLog4jLogger.setLevel(Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		System.out.println("Setting root logger level to: " + Level.ALL);
		rootLogger.setLevel(Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		System.out.println("Setting log4j root logger level to: " + Level.ALL);
		log4jRootLogger.setLevel(Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		setAllLogLevels(pluginLogger, Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
		setAllLog4JLogLevels(pluginLog4jLogger, Level.ALL);
		printAllLoggers(pluginLogger);
		printAllLog4jLoggers(pluginLog4jLogger);
		logOnAllLevels(pluginLogger);
	}

	private static void printAllLoggers(Logger logger) {
		System.out.println("------ Loggers overview");
		Logger current = logger;
		while (current != null) {
			System.out.println("Logger '" + current.getName() + "' has level " + current.getLevel());
			current = current.getParent();
		}
		System.out.println("------");
	}

	private static void printAllLog4jLoggers(Logger logger) {
		System.out.println("------ Log4j loggers overview");
		Logger current = logger;
		while (current != null) {
			System.out.println("Log4j Logger '" + current.getName() + "' has level " + current.getLevel());
			current = current.getParent();
		}
		System.out.println("------");
	}

	private static void setAllLogLevels(Logger logger, Level level) {
		System.out.println("------ Setting all logger levels");
		Logger current = logger;
		while (current != null) {
			System.out.println("Setting level of Logger '" + current.getName() + "' to level " + level);
			current.setLevel(level);
			current = current.getParent();
		}
		System.out.println("------");
	}

	private static void setAllLog4JLogLevels(Logger logger, Level level) {
		System.out.println("------ Setting all log4j logger levels");
		Logger current = logger;
		while (current != null) {
			System.out.println("Setting level of log4j Logger '" + current.getName() + "' to level " + level);
			current.setLevel(level);
			current = current.getParent();
		}
		System.out.println("------");
	}

	private static void logOnAllLevels(Logger logger) {
		System.out.println("------ Logging on all levels");
		byte b;
		int i;
		Level[] arrayOfLevel;
		for (i = (arrayOfLevel = LEVEL_VALUES).length, b = 0; b < i;) {
			Level level = arrayOfLevel[b];
			logger.log(level, "A message with level " + level.getName());
			b++;
		}
		System.out.println("------");
	}
}
