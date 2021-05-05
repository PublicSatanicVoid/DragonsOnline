package mc.dragons.dev.notifier;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.core.util.HttpUtil;
import mc.dragons.dev.DragonsDev;
import mc.dragons.dev.tasks.TaskLoader.Task;

public class DiscordNotifier {
	private static FileConfiguration config = JavaPlugin.getPlugin(DragonsDev.class).getConfig();
	
	public enum DiscordRole {
		DEVELOPER(config.getString("roleid-dev")),
		TASK_MANAGER(config.getString("roleid-tm")),
		BUILDER(config.getString("roleid-builder")),
		GAME_MASTER(config.getString("roleid-gm"));
		
		private String id;
		
		DiscordRole(String id) {
			this.id = id;
		}
		
		public String getId() {
			return id;
		}
	};
	
	private String webhookUrl;
	private boolean enabled;
	
	public DiscordNotifier(String webhookUrl) {
		this.webhookUrl = webhookUrl;
		this.enabled = true;
	}
	
	public String mentionRole(DiscordRole role) {
		return "<@" + role.getId() + ">";
	}
	
	public String mentionRoles(DiscordRole... roles) {
		return Arrays.stream(roles).map(r -> mentionRole(r)).reduce((a, b) -> a + " " + b).get();
	}
	
	public void sendNotification(String message) {
		if(!enabled) return;
		HttpUtil.post(webhookUrl, Map.of("content", message));
	}
	
	public void sendTaskNotification(Task task, String message) {
		sendNotification(message + " " + mentionRoles(task.getNotifyRoles()));
	}
	
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
