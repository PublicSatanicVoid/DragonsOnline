package mc.dragons.tools.content.command.gameobject;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;
import net.md_5.bungee.api.chat.TextComponent;

public class ObjectMetadataCommand extends DragonsCommandExecutor {

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
		
		MetadataConstants.displayMetadata(sender, gameObject);
		return true;
	}

}
