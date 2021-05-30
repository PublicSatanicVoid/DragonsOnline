package mc.dragons.tools.content.command.gameobject;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.ObjectUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.AuditLogLoader;
import mc.dragons.tools.content.AuditLogLoader.AuditLogEntry;
import net.md_5.bungee.api.chat.TextComponent;

public class ObjectMetadataCommand extends DragonsCommandExecutor {
	private AuditLogLoader AUDIT_LOG = dragons.getLightweightLoaderRegistry().getLoader(AuditLogLoader.class);
	private String UNKNOWN = ChatColor.RED + "Unavailable";
	
	public static TextComponent getClickableMetadataLink(GameObjectType type, UUID uuid) {
		return StringUtil.clickableHoverableText(ChatColor.GRAY + "[Click to View Metadata]", "/objmeta " + type + " " + uuid, "Click to view metadata about this game object");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/objmeta <gameobject type> <gameobject uuid>");
			return true;
		}
		
		GameObjectType type = StringUtil.parseEnum(sender, GameObjectType.class, args[0]);
		UUID uuid = parseUUID(sender, args[1]);
		if(type == null || uuid == null) return true;
		
		GameObject gameObject = type.getLoader().loadObject(dragons.getPersistentStorageManager().getStorageAccess(type, uuid));
		if(gameObject == null) {
			sender.sendMessage(ChatColor.RED + "No game object was found matching these criteria!");
			return true;
		}
		
//		MetadataConstants.displayMetadata(sender, gameObject);
		List<AuditLogEntry> entries = AUDIT_LOG.getEntries(gameObject.getIdentifier());
		int count = entries.size();
		if(count == 0) {
			sender.sendMessage(ChatColor.RED + "There is no metadata for this game object!");
			return true;
		}
		AuditLogEntry first = entries.get(0);
		AuditLogEntry last = entries.get(entries.size() - 1);
		AuditLogEntry lastPush = AUDIT_LOG.getLastPush(gameObject.getIdentifier());
		if(first != null && first.getLine().equals("Created")) {
			sender.sendMessage(ChatColor.GRAY + "Created By: " + ChatColor.GREEN + ObjectUtil.get(first.getBy(), () -> first.getBy().getName(), UNKNOWN));
			sender.sendMessage(ChatColor.GRAY + "Created On: " + ChatColor.GREEN + ObjectUtil.get(first.getDate(), () -> StringUtil.DATE_FORMAT.format(first.getDate()), UNKNOWN));
		}
		sender.sendMessage(ChatColor.GRAY + "Revision Count: " + ChatColor.GREEN + count);
		if(last != null) {
			sender.sendMessage(ChatColor.GRAY + "Last Revised By: " + ChatColor.GREEN + ObjectUtil.get(last.getBy(), () -> last.getBy().getName(), UNKNOWN));
			sender.sendMessage(ChatColor.GRAY + "Last Revised On: " + ChatColor.GREEN + ObjectUtil.get(last.getDate(), () -> StringUtil.DATE_FORMAT.format(last.getDate()), UNKNOWN));
		}
		if(lastPush != null) {
			sender.sendMessage(ChatColor.GRAY + "Last Pushed On: " + ChatColor.GREEN + StringUtil.DATE_FORMAT.format(lastPush.getDate()));
		}
		if(last == null || lastPush == null) {
			sender.sendMessage(ChatColor.GRAY + "Sync Status: " + UNKNOWN);
		}
		else if(last.getId() == lastPush.getId()) {
			sender.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.GREEN + "Matched in production");
		}
		else if(last.getId() > lastPush.getId()) {
			sender.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.YELLOW + "Changes made since last push");
		}
		else {
			sender.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.RED + "Behind production (SYNC ERROR)");
		}
		
		return true;
	}
}
