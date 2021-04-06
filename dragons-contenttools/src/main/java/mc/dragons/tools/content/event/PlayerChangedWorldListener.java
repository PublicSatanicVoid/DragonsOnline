package mc.dragons.tools.content.event;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

public class PlayerChangedWorldListener implements Listener {
	
	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent e) {
		User user = UserLoader.fromPlayer(e.getPlayer());
		Floor floor = FloorLoader.fromLocation(e.getPlayer().getLocation());
		boolean isAdminWorld = user.getSystemProfile() != null && user.getSystemProfile().getLocalAdminFloors().contains(floor);
		if(e.getPlayer().getGameMode() != GameMode.ADVENTURE && !isAdminWorld && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, false)) {
			e.getPlayer().sendMessage(ChatColor.RED + "You are not authorized for this gamemode in this world!");
			e.getPlayer().setGameMode(GameMode.ADVENTURE);
		}
		else if(e.getPlayer().getGameMode() != GameMode.ADVENTURE && !floor.isGMLocked() && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.ADMIN, false)) {
			e.getPlayer().sendMessage(ChatColor.RED + "This floor is currently edit-locked and cannot be edited.");
			e.getPlayer().setGameMode(GameMode.ADVENTURE);
		}
	}
}
