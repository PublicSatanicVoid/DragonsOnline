package mc.dragons.social.party;

import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.social.party.PartyLoader.Party;
import net.md_5.bungee.api.ChatColor;

public class PartyMessageHandler extends MessageHandler {
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	private PartyLoader partyLoader;
	private String server;
	
	public PartyMessageHandler(Dragons instance) {
		super(instance, "party");
		partyLoader = instance.getLightweightLoaderRegistry().getLoader(PartyLoader.class);
		server = instance.getServerName();
	}

	public void sendJoin(int id, User member) {
		sendAll(new Document("action", "join").append("party", id).append("member", member.getUUID()));
	}
	
	public void sendLeave(int id, User member) {
		sendAll(new Document("action", "leave").append("party", id).append("member", member.getUUID()));
	}
	
	public void sendKick(int id, User member) {
		sendAll(new Document("action", "kick").append("party", id).append("member", member.getUUID()));
	}
	
	public void sendDisband(int id, User owner) {
		sendAll(new Document("action", "disband").append("party", id).append("member", owner.getUUID()));
	}
	
	public void sendInvite(int id, User target) {
		send(new Document("action", "invite").append("party", id).append("member", target.getUUID()), target.getServer());
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		String message = "";
		String action = data.getString("action");
		int id = data.getInteger("party");
		Party party = partyLoader.get(id);
		User user = userLoader.loadObject(data.get("member", UUID.class));
		user.safeResyncData();
		boolean local = server.equals(serverFrom);
		boolean targetOnly = false;
		switch(action) {
		case "join":
			message = user.getName() + " joined the party!";
			if(!local) partyLoader.join(party, user, false);
			break;
		case "leave":
			message = user.getName() + " left the party!";
			if(!local) partyLoader.leave(party, user, false);
			break;
		case "kick":
			message = user.getName() + " was kicked from the party!";
			if(user.getPlayer() != null) {
				user.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "You were kicked from " + party.getOwner().getName() + "'s party!");
			}
			if(!local) partyLoader.leave(party, user, false);
			break;
		case "disband":
			message = user.getName() + " disbanded the party!";
			if(!local) partyLoader.unpool(id);
			break;
		case "invite":
			message = party.getOwner().getName() + " invited you to their party! To accept, do /party " + party.getOwner().getName();
			targetOnly = true;
			break;
		default:
			break;
		}
		
		String formatted = ChatColor.YELLOW + message;
		if(targetOnly) {
			if(user.getPlayer() != null) {
				user.getPlayer().sendMessage(formatted);
			}
		}
		else {
			Bukkit.getOnlinePlayers().stream().map(p -> UserLoader.fromPlayer(p)).filter(u -> party.hasUser(u)).forEach(u -> u.getPlayer().sendMessage(formatted));
		}
	}
}
