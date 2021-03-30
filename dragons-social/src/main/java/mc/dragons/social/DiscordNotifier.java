package mc.dragons.social;

import mc.dragons.core.util.HttpUtil;

public class DiscordNotifier {
	private String webhookUrl;
	private boolean enabled;
	
	public DiscordNotifier(String webhookUrl) {
		this.webhookUrl = webhookUrl;
		this.enabled = true;
	}
	
	public void sendNotification(String message) {
		if(!enabled) return;
		HttpUtil.post(webhookUrl, "content=" + message);
	}
	
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
