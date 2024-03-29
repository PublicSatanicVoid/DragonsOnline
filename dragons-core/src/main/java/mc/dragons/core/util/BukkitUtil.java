package mc.dragons.core.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.DragonsLogger;

/**
 * Utilities for simplifying cumbersome Bukkit or Spigot API tasks.
 * 
 * @author Adam
 *
 */
public class BukkitUtil {
	private static Dragons dragons = Dragons.getInstance();
	private static DragonsLogger LOGGER = dragons.getLogger();
	private static int N_ROLLING_ASYNC_THREADS = 5; // More threads = reduced likelihood of pile-ups but increased
													// number of database connections (and threads, obviously)

	// The price of usability is static abuse
	// Just a little
	
	@SuppressWarnings("unchecked")
	private static List<Runnable>[] rollingQueues = new List[N_ROLLING_ASYNC_THREADS];
	private static List<Runnable> rollingSync = Collections.synchronizedList(new LinkedList<>());
	private static int rollingAsyncIndex = 0;
	private static Thread[] rollingAsyncThreads = new Thread[N_ROLLING_ASYNC_THREADS];

	static {
		for(int i = 0; i < N_ROLLING_ASYNC_THREADS; i++) {
			rollingQueues[i] = Collections.synchronizedList(new LinkedList<>());
		}
		for (int i = 1; i <= N_ROLLING_ASYNC_THREADS; i++) {
			final int id = i-1;
			rollingAsyncThreads[i-1] = new Thread("Rolling Async Thread #" + i) {
				private final List<Runnable> queue = rollingQueues[id];
				@Override
				public void run() {
					while (true) {
						while (!queue.isEmpty()) {
							try {
								queue.get(0).run();
							} catch (Exception e) {
								LOGGER.warning(
										"Exception occurred in rolling async thread (ignored): " + e.getMessage());
								e.printStackTrace();
							}
							queue.remove(0);
						}
					}
				}
			};
			rollingAsyncThreads[i-1].start();
		}
	}

	public static void initRollingSync() {
		syncPeriodic(() -> {
			while (!rollingSync.isEmpty()) {
				try {
					rollingSync.get(0).run();
				} catch (Exception e) {
					LOGGER.warning("Exception occurred in rolling sync task (ignored): " + e.getMessage());
					e.printStackTrace();
				}
				rollingSync.remove(0);
			}
		}, 1);
	}

	public static void async(Runnable runnable) {
		async(runnable, 1);
	}

	public static void async(Runnable runnable, int delayTicks) {
		LOGGER.verbose("await(" + runnable + ", " + delayTicks + ")");
		// Get around "plugin tried to register task while disabled"
		// when we try to run async stuff after DragonsCore is disabled
		if (!dragons.isEnabled()) {
			new Thread() {
				@Override
				public void run() {
					try {
						sleep(50L * delayTicks);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runnable.run();
				}
			}.start();
			return;
		}
		Bukkit.getScheduler().runTaskLaterAsynchronously(dragons, runnable, delayTicks);
	}

	/**
	 * Asynchronously gets a value and then supplies it to a synchronous consumer.
	 * 
	 * @param <T>           The type that is returned by the async supplier
	 * @param asyncSupplier A thread-safe supplier
	 * @param syncConsumer  A synchronous consumer
	 */
	public static <T> void await(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
		LOGGER.verbose("await(" + asyncSupplier + ", " + syncConsumer + ")");
		async(() -> {
			T value = asyncSupplier.get();
			sync(() -> {
				syncConsumer.accept(value);
			});
		});
	}

	/**
	 * Runs in a continuously executing separate thread.
	 * 
	 * <p>
	 * Useful for things like MongoDB where we want multithreaded capability but
	 * opening a new thread for <i>every query</i> results in opening a new
	 * connection, which is ridiculous.
	 * 
	 * <p>
	 * Allows async tasks that use `ThreadLocal`s to re-use those data values across
	 * iterations.
	 * 
	 * @param runnable
	 */
	public static void rollingAsync(Runnable runnable) {
		synchronized(rollingQueues) {
			rollingQueues[rollingAsyncIndex++].add(runnable);
			if(rollingAsyncIndex >= N_ROLLING_ASYNC_THREADS) {
				rollingAsyncIndex = 0;
			}
		}
		// LOGGER.verbose("rollingAsync(" + runnable + ")");
	}

	public static BukkitTask sync(Runnable runnable) {
		LOGGER.verbose("sync(" + runnable + ")");
		return Bukkit.getScheduler().runTask(dragons, runnable);
	}

	public static BukkitTask sync(Runnable runnable, int delayTicks) {
		LOGGER.verbose("sync(" + runnable + ", " + delayTicks + ")");
		return Bukkit.getScheduler().runTaskLater(dragons, runnable, delayTicks);
	}

	public static void rollingSync(Runnable runnable) {
		LOGGER.verbose("rollingSync(" + runnable + ")");
		rollingSync.add(runnable);
	}

	public static BukkitTask syncPeriodic(Runnable runnable, int periodTicks) {
		LOGGER.verbose("syncPeriodic(" + runnable + ", " + periodTicks + ")");
		return Bukkit.getScheduler().runTaskTimer(dragons, runnable, 0L, periodTicks);
	}

	public static BukkitTask syncPeriodic(Runnable runnable, int delayTicks, int periodTicks) {
		LOGGER.verbose("syncPeriodic(" + runnable + ", " + delayTicks + ", " + periodTicks + ")");
		return Bukkit.getScheduler().runTaskTimer(dragons, runnable, delayTicks, periodTicks);
	}
}
