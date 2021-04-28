package mc.dragons.core;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.tasks.AutoSaveTask;
import mc.dragons.core.tasks.SpawnEntityTask;
import mc.dragons.core.tasks.VerifyGameIntegrityTask;

/**
 * Settings for this server instance. May be changed locally
 * through the /serveroptions command.
 * 
 * @author Adam
 *
 */
public class ServerOptions {
	private Dragons dragons;
	private DragonsLogger LOGGER;

	private int autoSavePeriodTicks;
	private boolean autoSaveEnabled;

	private int customSpawnMargin;
	private int customSpawnRate;
	private boolean customSpawningEnabled;
	
	private double dropChanceMultiplier;

	private int deathCountdown;

	private int verifyIntegritySweepRate;
	private boolean verifyIntegrityEnabled;

	private double defaultWalkSpeed;

	private Level logLevel;

	public ServerOptions(Dragons instance) {
		dragons = instance;
		LOGGER = dragons.getLogger();
		
		autoSavePeriodTicks = 6000;
		autoSaveEnabled = true;
		customSpawnMargin = 25;
		customSpawnRate = 100;
		customSpawningEnabled = true;
		dropChanceMultiplier = 1.0;
		deathCountdown = 10;
		verifyIntegritySweepRate = 1200;
		verifyIntegrityEnabled = true;
		defaultWalkSpeed = 0.2D;
		logLevel = Level.INFO;
	}

	public void setAutoSavePeriodTicks(int period) {
		autoSavePeriodTicks = period;
		Dragons.getInstance().getAutoSaveRunnable().cancel();
		AutoSaveTask task = new AutoSaveTask(dragons);
		Dragons.getInstance().setAutoSaveRunnable(task);
		task.runTaskTimer(dragons, 0L, period);
		LOGGER.config("Set auto-save period to " + period + " ticks");
	}

	public int getAutoSavePeriodTicks() {
		return autoSavePeriodTicks;
	}

	public void setAutoSaveEnabled(boolean enabled) {
		autoSaveEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " auto-saving");
	}

	public boolean isAutoSaveEnabled() {
		return autoSaveEnabled;
	}

	public void setCustomSpawnRate(int rate) {
		customSpawnRate = rate;
		dragons.getSpawnEntityRunnable().cancel();
		SpawnEntityTask task = new SpawnEntityTask(dragons);
		dragons.setSpawnEntityRunnable(task);
		task.runTaskTimer(dragons, 0L, rate);
		LOGGER.config("Custom spawn rate set to " + rate + "t.");
	}

	public int getCustomSpawnRate() {
		return customSpawnRate;
	}
	
	public void setCustomSpawnMargin(int margin) {
		customSpawnMargin = margin;
		LOGGER.config("Custom spawn margin set to " + margin + "m.");
	}
	
	public int getCustomSpawnMargin() {
		return customSpawnMargin;
	}

	public void setCustomSpawningEnabled(boolean enabled) {
		customSpawningEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " custom spawning");
	}

	public boolean isCustomSpawningEnabled() {
		return customSpawningEnabled;
	}

	public void setDropChanceMultiplier(double multiplier) {
		dropChanceMultiplier = multiplier;
		LOGGER.config("Drop chance multiplier set to " + multiplier + "x");
	}
	
	public double getDropChanceMultiplier() {
		return dropChanceMultiplier;
	}
	
	public void setDeathCountdown(int seconds) {
		deathCountdown = seconds;
		LOGGER.config("Default death countdown set to " + seconds + "s");
	}

	public int getDeathCountdown() {
		return deathCountdown;
	}

	public void setVerifyIntegritySweepRate(int rate) {
		verifyIntegritySweepRate = rate;
		dragons.getVerifyGameIntegrityRunnable().cancel();
		VerifyGameIntegrityTask task = new VerifyGameIntegrityTask(dragons);
		dragons.setVerifyGameIntegrityRunnable(task);
		task.runTaskTimer(dragons, 0L, rate);
		LOGGER.config("Game verification sweep rate set to " + rate + "t.");
	}

	public int getVerifyIntegritySweepRate() {
		return verifyIntegritySweepRate;
	}

	public void setVerifyIntegrityEnabled(boolean enabled) {
		verifyIntegrityEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " game environment verification");
	}

	public boolean isVerifyIntegrityEnabled() {
		return verifyIntegrityEnabled;
	}

	public void setDefaultWalkSpeed(double speed) {
		defaultWalkSpeed = speed;
		LOGGER.config("Default walk speed set to " + speed);
	}

	public double getDefaultWalkSpeed() {
		return defaultWalkSpeed;
	}

	public void setLogLevel(Level level) {
		logLevel = level;
		LOGGER.setLevel(level);
		Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Plugin::getLogger).forEach(logger -> logger.setLevel(level));
		LOGGER.info("Log level changed to " + level);
	}

	public Level getLogLevel() {
		return logLevel;
	}
}
