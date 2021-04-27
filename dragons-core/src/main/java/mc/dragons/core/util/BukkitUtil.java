package mc.dragons.core.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Utilities for simplifying cumbersome Bukkit or Spigot API tasks.
 * 
 * @author Adam
 *
 */
public class BukkitUtil {
	private static Dragons dragons = Dragons.getInstance();
	
	// The price of usability is static abuse
	// Just a little
	private static List<Runnable> rolling = Collections.synchronizedList(new LinkedList<>());
	private static Thread rollingAsyncThread = new Thread("Rolling Async Thread") {
		@Override
		public void run() {
			while(true) {
				while(!rolling.isEmpty()) {
					try {
						rolling.get(0).run();
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					rolling.remove(0);
				}
			}
		}
	};
	
	static {
		rollingAsyncThread.start();
	}
	
	public static void async(Runnable runnable) {
		async(runnable, 1);
	}
	
	public static void async(Runnable runnable, int delay) {
		// Get around "plugin tried to register task while disabled"
		// when we try to run async stuff after DragonsCore is disabled
		if(!dragons.isEnabled()) {
			new Thread() {
				@Override
				public void run() {
					try {
						sleep(50L * delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runnable.run();
				}
			}.start();
			return;
		}
		Bukkit.getScheduler().runTaskLaterAsynchronously(dragons, runnable, delay);
	}
	
	/**
	 * Asynchronously gets a value and then supplies it to a
	 * synchronous consumer.
	 * 
	 * @param <T> The type that is returned by the async supplier
	 * @param asyncSupplier A thread-safe supplier
	 * @param syncConsumer A synchronous consumer
	 */
	public static <T> void await(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
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
	 * <p>Useful for things like MongoDB where we want
	 * multithreaded capability but opening a new thread
	 * for <i>every query</i> results in opening a new
	 * connection, which is ridiculous.
	 * 
	 * @param runnable
	 */
	public static void rollingAsync(Runnable runnable) {
		rolling.add(runnable);
	}
	
	public static void sync(Runnable runnable) {
		Bukkit.getScheduler().runTask(dragons, runnable);
	}
	
	public static void sync(Runnable runnable, int delay) {
		Bukkit.getScheduler().runTaskLater(dragons, runnable, delay);
	}
	
	public static void sendActionBar(Player player, String message) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
	}
}
