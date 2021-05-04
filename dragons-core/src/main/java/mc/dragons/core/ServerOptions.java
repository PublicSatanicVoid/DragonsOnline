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

	/**
	 * Period in ticks between auto-saves.
	 * 
	 * @implNote This automatically re-schedules the task.
	 * 
	 * @param period
	 */
	public void setAutoSavePeriodTicks(int period) {
		autoSavePeriodTicks = period;
		dragons.getAutoSaveRunnable().cancel();
		AutoSaveTask task = new AutoSaveTask(dragons);
		dragons.setAutoSaveRunnable(task);
		task.runTaskTimer(dragons, 0L, period);
		LOGGER.config("Set auto-save period to " + period + " ticks");
	}

	/**
	 * Period in ticks between auto-saves.
	 * 
	 * @return
	 */
	public int getAutoSavePeriodTicks() {
		return autoSavePeriodTicks;
	}

	/**
	 * Whether auto-saving is enabled.
	 * 
	 * @param enabled
	 */
	public void setAutoSaveEnabled(boolean enabled) {
		autoSaveEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " auto-saving");
	}

	/**
	 * Whether auto-saving is enabled.
	 * 
	 * @return
	 */
	public boolean isAutoSaveEnabled() {
		return autoSaveEnabled;
	}

	/**
	 * Period in ticks between custom spawn waves.
	 * 
	 * @implNote This automatically re-schedules the task.
	 * 
	 * @param rate
	 */
	public void setCustomSpawnRate(int rate) {
		customSpawnRate = rate;
		dragons.getSpawnEntityRunnable().cancel();
		SpawnEntityTask task = new SpawnEntityTask(dragons);
		dragons.setSpawnEntityRunnable(task);
		task.runTaskTimer(dragons, 0L, rate);
		LOGGER.config("Custom spawn rate set to " + rate + "t.");
	}

	/**
	 * Period in ticks between custom spawn waves.
	 * 
	 * @return
	 */
	public int getCustomSpawnRate() {
		return customSpawnRate;
	}
	
	/**
	 * Extra buffer around players to account for mob counts,
	 * to avoid over-spawning.
	 * 
	 * @param margin
	 */
	public void setCustomSpawnMargin(int margin) {
		customSpawnMargin = margin;
		LOGGER.config("Custom spawn margin set to " + margin + "m.");
	}
	
	/**
	 * Extra buffer around players to account for mob counts,
	 * to avoid over-spawning.
	 * 
	 * @return
	 */
	public int getCustomSpawnMargin() {
		return customSpawnMargin;
	}

	/**
	 * Whether custom mob spawning is enabled.
	 * 
	 * @implNote This will not affect vanilla spawning behavior,
	 * which is determined by world game rules.
	 * 
	 * @param enabled
	 */
	public void setCustomSpawningEnabled(boolean enabled) {
		customSpawningEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " custom spawning");
	}

	/**
	 * Whether custom mob spawning is enabled.
	 * 
	 * @return
	 */
	public boolean isCustomSpawningEnabled() {
		return customSpawningEnabled;
	}

	/**
	 * Multiplier for custom mob drop chances. 1 is normal rate.
	 * 
	 * @param multiplier
	 */
	public void setDropChanceMultiplier(double multiplier) {
		dropChanceMultiplier = multiplier;
		LOGGER.config("Drop chance multiplier set to " + multiplier + "x");
	}
	
	/**
	 * Multiplier for custom mob drop chances. 1 is normal rate.
	 * @return
	 */
	public double getDropChanceMultiplier() {
		return dropChanceMultiplier;
	}
	
	/**
	 * Waiting period before respawning, in seconds.
	 * 
	 * @param seconds
	 */
	public void setDeathCountdown(int seconds) {
		deathCountdown = seconds;
		LOGGER.config("Default death countdown set to " + seconds + "s");
	}

	/**
	 * Waiting period before respawning, in seconds.
	 * 
	 * @return
	 */
	public int getDeathCountdown() {
		return deathCountdown;
	}

	/**
	 * Period between game verification, in ticks.
	 * 
	 * @implNote This automatically re-schedules the task.
	 * 
	 * @param rate
	 */
	public void setVerifyIntegritySweepRate(int rate) {
		verifyIntegritySweepRate = rate;
		dragons.getVerifyGameIntegrityRunnable().cancel();
		VerifyGameIntegrityTask task = new VerifyGameIntegrityTask(dragons);
		dragons.setVerifyGameIntegrityRunnable(task);
		task.runTaskTimer(dragons, 0L, rate);
		LOGGER.config("Game verification sweep rate set to " + rate + "t.");
	}

	/**
	 * Period between game verification, in ticks.
	 * @return
	 */
	public int getVerifyIntegritySweepRate() {
		return verifyIntegritySweepRate;
	}

	/**
	 * Whether periodic game verification is enabled.
	 * 
	 * @param enabled
	 */
	public void setVerifyIntegrityEnabled(boolean enabled) {
		verifyIntegrityEnabled = enabled;
		LOGGER.config((enabled ? "Enabled" : "Disabled") + " game environment verification");
	}

	/**
	 * Whether periodic game verification is enabled.
	 * 
	 * @return
	 */
	public boolean isVerifyIntegrityEnabled() {
		return verifyIntegrityEnabled;
	}

	/**
	 * Base walk speed before item modifiers are applied.
	 * 
	 * @param speed
	 */
	public void setDefaultWalkSpeed(double speed) {
		defaultWalkSpeed = speed;
		LOGGER.config("Default walk speed set to " + speed);
	}

	/**
	 * Base walk speed before item modifiers are applied.
	 * 
	 * @return
	 */
	public double getDefaultWalkSpeed() {
		return defaultWalkSpeed;
	}

	/**
	 * Global log level for all plugins.
	 * 
	 * @param level
	 */
	public void setLogLevel(Level level) {
		logLevel = level;
		LOGGER.setLevel(level);
		Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Plugin::getLogger).forEach(logger -> logger.setLevel(level));
		LOGGER.info("Log level changed to " + level);
	}

	/**
	 * Global log level for all plugins.
	 * 
	 * @return
	 */
	public Level getLogLevel() {
		return logLevel;
	}
}
