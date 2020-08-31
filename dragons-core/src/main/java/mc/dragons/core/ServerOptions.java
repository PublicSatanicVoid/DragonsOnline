package mc.dragons.core;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.logging.LogFilter;
import mc.dragons.core.tasks.AutoSaveTask;
import mc.dragons.core.tasks.SpawnEntityTask;
import mc.dragons.core.tasks.VerifyGameIntegrityTask;

public class ServerOptions {
	private Logger LOGGER;

	private org.apache.logging.log4j.core.Logger pluginLogger;

	private int autoSavePeriodTicks;

	private boolean autoSaveEnabled;

	private int customSpawnRate;

	private boolean customSpawningEnabled;

	private int deathCountdown;

	private int verifyIntegritySweepRate;

	private boolean verifyIntegrityEnabled;

	private double defaultWalkSpeed;

	private Level logLevel;

	public ServerOptions(org.apache.logging.log4j.core.Logger pluginLogger) {
		this.pluginLogger = pluginLogger;
		this.LOGGER = Dragons.getInstance().getLogger();
		this.autoSavePeriodTicks = 6000;
		this.autoSaveEnabled = true;
		this.customSpawnRate = 100;
		this.customSpawningEnabled = true;
		this.deathCountdown = 10;
		this.verifyIntegritySweepRate = 1200;
		this.verifyIntegrityEnabled = true;
		this.defaultWalkSpeed = 0.2D;
		this.logLevel = Level.INFO;
	}

	public void setAutoSavePeriodTicks(int period) {
		this.autoSavePeriodTicks = period;
		Dragons.getInstance().getAutoSaveRunnable().cancel();
		AutoSaveTask task = new AutoSaveTask(Dragons.getInstance());
		Dragons.getInstance().setAutoSaveRunnable((BukkitRunnable) task);
		task.runTaskTimer((Plugin) Dragons.getInstance(), 0L, period);
		this.LOGGER.config("Set auto-save period to " + period + " ticks");
	}

	public int getAutoSavePeriodTicks() {
		return this.autoSavePeriodTicks;
	}

	public void setAutoSaveEnabled(boolean enabled) {
		this.autoSaveEnabled = enabled;
		this.LOGGER.config(String.valueOf(enabled ? "Enabled" : "Disabled") + " auto-saving");
	}

	public boolean isAutoSaveEnabled() {
		return this.autoSaveEnabled;
	}

	public void setCustomSpawnRate(int rate) {
		this.customSpawnRate = rate;
		Dragons.getInstance().getSpawnEntityRunnable().cancel();
		SpawnEntityTask task = new SpawnEntityTask(Dragons.getInstance());
		Dragons.getInstance().setSpawnEntityRunnable((BukkitRunnable) task);
		task.runTaskTimer((Plugin) Dragons.getInstance(), 0L, rate);
		this.LOGGER.config("Custom spawn rate set to " + rate + "s.");
	}

	public int getCustomSpawnRate() {
		return this.customSpawnRate;
	}

	public void setCustomSpawningEnabled(boolean enabled) {
		this.customSpawningEnabled = enabled;
		this.LOGGER.config(String.valueOf(enabled ? "Enabled" : "Disabled") + " custom spawning");
	}

	public boolean isCustomSpawningEnabled() {
		return this.customSpawningEnabled;
	}

	public void setDeathCountdown(int seconds) {
		this.deathCountdown = seconds;
		this.LOGGER.config("Default death countdown set to " + seconds + "s");
	}

	public int getDeathCountdown() {
		return this.deathCountdown;
	}

	public void setVerifyIntegritySweepRate(int rate) {
		this.verifyIntegritySweepRate = rate;
		Dragons.getInstance().getVerifyGameIntegrityRunnable().cancel();
		VerifyGameIntegrityTask task = new VerifyGameIntegrityTask(Dragons.getInstance());
		Dragons.getInstance().setVerifyGameIntegrityRunnable(task);
		task.runTaskTimer((Plugin) Dragons.getInstance(), 0L, rate);
		this.LOGGER.config("Game verification sweep rate set to " + rate + "s.");
	}

	public int getVerifyIntegritySweepRate() {
		return this.verifyIntegritySweepRate;
	}

	public void setVerifyIntegrityEnabled(boolean enabled) {
		this.verifyIntegrityEnabled = enabled;
		this.LOGGER.config(String.valueOf(enabled ? "Enabled" : "Disabled") + " game environment verification");
	}

	public boolean isVerifyIntegrityEnabled() {
		return this.verifyIntegrityEnabled;
	}

	public void setDefaultWalkSpeed(double speed) {
		this.defaultWalkSpeed = speed;
		this.LOGGER.config("Default walk speed set to " + speed);
	}

	public double getDefaultWalkSpeed() {
		return this.defaultWalkSpeed;
	}

	public void setLogLevel(Level level) {
		this.logLevel = level;
		this.LOGGER.setLevel(level);
		this.pluginLogger.setLevel(LogFilter.fromJUL(level));
		Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Plugin::getLogger).forEach(logger -> logger.setLevel(level));
		this.LOGGER.info("Log level changed to " + level);
	}

	public Level getLogLevel() {
		return this.logLevel;
	}
}
