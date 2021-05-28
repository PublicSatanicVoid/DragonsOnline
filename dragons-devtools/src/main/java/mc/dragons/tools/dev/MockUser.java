package mc.dragons.tools.dev;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class MockUser extends User implements CommandSender {
	private static DragonsLogger LOGGER = Dragons.getInstance().getLogger();
	
	public MockUser(MockPlayer mockPlayer, StorageManager storageManager, StorageAccess storageAccess) {
		super(mockPlayer, storageManager, storageAccess);
		this.joined = true;
	}

	@Override
	public boolean isPermissionSet(String name) {
		return true;
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return true;
	}

	@Override
	public boolean hasPermission(String name) {
		return true;
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return true;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return null;
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		// Do nothing
	}

	@Override
	public void recalculatePermissions() {
		// Do nothing
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return new HashSet<>();
	}

	@Override
	public boolean isOp() {
		return true;
	}

	@Override
	public void setOp(boolean value) {
		// Do nothing
	}

	@Override
	public void sendMessage(String message) {
		LOGGER.debug("MOCK USER: " + getName() + " received message " + message);
	}

	@Override
	public void sendMessage(String[] messages) {
		for(String message : messages) sendMessage(message);
	}

	@Override
	public void sendMessage(UUID sender, String message) {
		sendMessage(message);
	}

	@Override
	public void sendMessage(UUID sender, String[] messages) {
		sendMessage(messages);
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public Spigot spigot() {
		return null;
	}
}
