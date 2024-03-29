package mc.dragons.social.guild;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.social.DragonsSocial;
import mc.dragons.social.guild.GuildLoader.Guild;

public class GuildMessageHandler extends MessageHandler {
	private DragonsSocial plugin;
	private GuildLoader guildLoader;
	private UserLoader userLoader;

	public GuildMessageHandler(DragonsSocial instance) {
		super(instance.getDragonsInstance(), "guild");
		plugin = instance;
		guildLoader = instance.getDragonsInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
		userLoader = GameObjectType.getLoader(UserLoader.class);
	}

	private void doLocalNotify(String guildName, String message) {
		plugin.getLogger().verbose("Guild local notify: guild=" + guildName + ", msg=" + message);
		Guild guild = guildLoader.getGuildByName(guildName);
		List<UUID> members = new ArrayList<>(guild.getMembers());
		members.add(guild.getOwner());
		guild.resync();
		members.stream().map(uuid -> userLoader.loadObject(uuid)).filter(u -> u.getPlayer() != null).forEach(u -> {
			u.updateListName();
			u.updatePrimaryNameTag(true);
			if (message != null) {
				u.getPlayer().sendMessage(message);
			}
		});
	}

	public void send(String guild, String message) {
		sendAll(new Document("guild", guild).append("message", message));
	}

	@Override
	public void receive(String serverFrom, Document data) {
		doLocalNotify(data.getString("guild"), data.containsKey("message") ? data.getString("message") : null);
	}
}
