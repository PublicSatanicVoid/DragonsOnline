package mc.dragons.social.friend;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.DragonsLogger;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.core.util.StringUtil;
import mc.dragons.social.DragonsSocial;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class FriendMessageHandler extends MessageHandler {
	private DragonsLogger LOGGER;

	public FriendMessageHandler(DragonsSocial instance) {
		super(instance.getDragonsInstance(), "friend");
		LOGGER = instance.getLogger();
	}

	public void pushRequest(User from, User to) {
		send(new Document("action", "request").append("from", from.getUUID()).append("fromName", from.getName()).append("to", to.getUUID()), to.getServer());
	}

	public void pushAccept(User from, User to) {
		send(new Document("action", "accept").append("from", from.getUUID()).append("fromName", from.getName()).append("to", to.getUUID()), to.getServer());
	}

	public void pushIgnore(User from, User to) {
		send(new Document("action", "deny").append("from", from.getUUID()).append("fromName", from.getName()).append("to", to.getUUID()), to.getServer());
	}

	public void pushRemove(User from, User to) {
		send(new Document("action", "remove").append("from", from.getUUID()).append("fromName", from.getName()).append("to", to.getUUID()), to.getServer());
	}

	@Override
	public void receive(String serverFrom, Document data) {
		String action = data.getString("action");
		UUID toUUID = data.get("to", UUID.class);
		Player to = Bukkit.getPlayer(toUUID);
		if (to == null) {
			LOGGER.notice("Could not deliver friend message locally: User with UUID " + toUUID + " is not online on this server");
			return;
		}
		User toUser = UserLoader.fromPlayer(to);
		UUID fromUUID = data.get("from", UUID.class);
		String from = data.getString("fromName");
		boolean local = serverFrom.equals(toUser.getServer());
		toUser.safeResyncData();
		if (action.equalsIgnoreCase("request")) {
			to.sendMessage(ChatColor.LIGHT_PURPLE + from + " sent you a friend request!");
			TextComponent accept = StringUtil.clickableHoverableText(ChatColor.GREEN + " [ACCEPT] ", "/friend " + from, ChatColor.GREEN + "Accept friend request");
			TextComponent reject = StringUtil.clickableHoverableText(ChatColor.GRAY + "[IGNORE]", "/friend ignore " + from, ChatColor.GRAY + "Ignore friend request");
			to.spigot().sendMessage(accept, reject);
			if (!local) {
				FriendUtil.getFriendsIncomingUUID(toUser).add(fromUUID);
			}
		} else if (action.equalsIgnoreCase("accept")) {
			to.sendMessage(FriendCommand.SUCCESS_PREFIX + ChatColor.LIGHT_PURPLE + from + " accepted your friend request!");
			if (!local) {
				FriendUtil.getFriendsUUID(toUser).add(fromUUID);
				FriendUtil.getFriendsOutgoingUUID(toUser).remove(fromUUID);
			}
		} else if (action.equalsIgnoreCase("deny")) {
//			to.sendMessage(ChatColor.LIGHT_PURPLE + from + " denied your friend request.");
			if (!local) {
				FriendUtil.getFriendsOutgoingUUID(toUser).remove(fromUUID);
			}
		} else if (action.equalsIgnoreCase("remove")) {
			to.sendMessage(ChatColor.LIGHT_PURPLE + from + " removed you as a friend.");
			if (!local) {
				FriendUtil.getFriendsUUID(toUser).remove(fromUUID);
			}
		}
	}
}
