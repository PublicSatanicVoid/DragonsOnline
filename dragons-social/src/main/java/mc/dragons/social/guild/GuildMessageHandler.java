package mc.dragons.social.guild;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.Bukkit;

import mc.dragons.core.Dragons;
import mc.dragons.core.networking.MessageHandler;
import mc.dragons.social.guild.GuildLoader.Guild;
 
public class GuildMessageHandler extends MessageHandler {
	private GuildLoader guildLoader;
	
	public GuildMessageHandler() {
		super(Dragons.getInstance(), "guild");
		guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
	}

	private void doLocalNotify(String guildName, String message) {
		Dragons.getInstance().getLogger().finest("Guild local notify: guild=" + guildName + ", msg=" + message);
		Guild guild = guildLoader.getGuildByName(guildName);
		List<UUID> members = new ArrayList<>(guild.getMembers());
		members.add(guild.getOwner());
		members.stream()
			.map(Bukkit::getPlayer)
			.filter(Objects::nonNull)
			.forEach(p -> 
				p.sendMessage(message));
	}
	
	public void send(String guild, String message) {
		sendAll(new Document("guild", guild).append("message", message));
	}
	
	@Override
	public void receive(String serverFrom, Document data) {
		doLocalNotify(data.getString("guild"), data.getString("message"));
	}
}
