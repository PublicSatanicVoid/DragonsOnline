package mc.dragons.core.networking;

import java.util.Arrays;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.MathUtil;
import mc.dragons.core.util.PermissionUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Handles the sending and receiving of staff alerts.
 * 
 * @author Adam
 *
 */
public class StaffAlertMessageHandler extends MessageHandler {
	public StaffAlertMessageHandler() {
		super(Dragons.getInstance(), "staffAlert");
	}
	
	/**
	 * Sends a staff alert indicating that this server is experiencing consistently low TPS.
	 * 
	 * <p>Only send this if you intend staff to receive the alert.
	 * 
	 * @implNote There is no internal rate-limiting.
	 * 
	 * @param tps
	 */
	public void sendLagMessage(double tps) {
		sendAll(new Document("permissionLevel", PermissionLevel.DEVELOPER.toString()).append("subtype", "laggyServer").append("tps", tps).append("players", Bukkit.getOnlinePlayers().size()));
	}
	
	/**
	 * Sends a staff alert notifying moderation of a new report.
	 * 
	 * @param reportId
	 * @param message A short description of the report
	 */
	public void sendReportMessage(int reportId, String message) {
		sendAll(new Document("permissionLevel", PermissionLevel.MODERATOR.toString()).append("subtype", "report").append("reportId", reportId).append("message", message));
	}
	
	/**
	 * Sends a staff alert notifying moderation of a suspicious join.
	 * 
	 * @param message A short description of the suspicious join
	 */
	public void sendSuspiciousJoinMessage(String message) {
		sendAll(new Document("permissionLevel", PermissionLevel.MODERATOR.toString()).append("subtype", "susjoin").append("message", message));
	}
	
	/**
	 * Sends a staff alert with the given message, to be displayed to
	 * all staff with permission level greater than or equal to the
	 * specified level.
	 * 
	 * @param level
	 * @param message
	 */
	public void sendGenericMessage(PermissionLevel level, String message) {
		sendAll(new Document("permissionLevel", level.toString()).append("subtype", "generic").append("message", message));
	}

	@Override
	public void receive(String serverFrom, Document data) {
		PermissionLevel level = PermissionLevel.valueOf(data.getString("permissionLevel"));
		String subtype = data.getString("subtype");
		
		TextComponent message;
		if(subtype.equals("laggyServer")) {
			message = new TextComponent("Server " + serverFrom + " is experiencing severe lag (" + MathUtil.round(data.getDouble("tps")) + "TPS, " + data.getInteger("players") + " players). Click to restart the instance!");
			message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to restart instance " + serverFrom)));
			message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/restartinstance " + serverFrom));
		}
		else if(subtype.equals("generic")) {
			message = new TextComponent("[" + serverFrom + "] " + data.getString("message"));
		}
		else if(subtype.equals("report")) {
			message = new TextComponent(ChatColor.GRAY + data.getString("message") + ChatColor.GRAY + " [" + ChatColor.UNDERLINE + "Click to View" + ChatColor.GRAY + "]");
			message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to view report #" + data.getInteger("reportId"))));
			message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewreport " + data.getInteger("reportId")));
		}
		else if(subtype.equals("susjoin")) {
			message = new TextComponent(data.getString("message"));
		}
		else {
			message = new TextComponent("[" + serverFrom + "] Unrecognized staff alert message: " + data.toJson());
			level = PermissionLevel.DEVELOPER;
		}
		
		final PermissionLevel fLevel = level;
		
		UserLoader.allUsers().stream().filter(u -> PermissionUtil.verifyActivePermissionLevel(u, fLevel, false)).forEach(u -> {
			if(subtype.equals("report") && (!PermissionUtil.verifyActiveProfileFlag(u, SystemProfileFlag.MODERATION, false) || !u.getData().getEmbedded(Arrays.asList("modnotifs", "reports"), true))) return;
			if(subtype.equals("susjoin") && (!PermissionUtil.verifyActiveProfileFlag(u, SystemProfileFlag.MODERATION, false) || !u.getData().getEmbedded(Arrays.asList("modnotifs", "susjoin"), true))) return;
			u.getPlayer().spigot().sendMessage(new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "Staff Alert: " + ChatColor.RESET), message);
		});
	}
	
	
}
