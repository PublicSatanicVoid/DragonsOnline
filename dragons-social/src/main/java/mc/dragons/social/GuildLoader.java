package mc.dragons.social;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.core.storage.mongo.MongoConfig;
import mc.dragons.social.GuildLoader.Guild;

/**
 * Loads guilds from MongoDB. Guilds are pooled locally to reduce network usage.
 * 
 * @author Adam
 *
 */
public class GuildLoader extends AbstractLightweightLoader<Guild> {
	
	public static enum GuildAccessLevel {
		ALL("Public"),
		REQUEST("Request-to-join"),
		INVITE("Invite-only"),
		UNLISTED("Unlisted");
		
		private String friendlyName;
		
		GuildAccessLevel(String friendlyName) {
			this.friendlyName = friendlyName;
		}
		
		public String friendlyName() { return friendlyName; }
	}
	
	/**
	 * A guild is a named group of users. Users can belong to multiple guilds.
	 * Guilds are uniquely and stably identified by an integral ID, and are
	 * uniquely but possibly unstably identified by their name, which must not
	 * contain spaces.
	 * 
	 * @author Adam
	 *
	 */
	public static class Guild {
		private static GuildLoader guildLoader = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(GuildLoader.class);
		
		private int id;
		private String name;
		private UUID owner;
		private String description;
		private int xp;
		private List<UUID> members;
		private List<UUID> blacklist;
		private List<UUID> pending;
		private List<UUID> invited;
		private GuildAccessLevel accessLevel;
		private Date createdOn;
		
		public Guild(int id, String name, UUID owner, String description, int xp, 
				List<UUID> members, List<UUID> blacklist, List<UUID> pending, List<UUID> invited, GuildAccessLevel accessLevel, Date createdOn) {
			this.id = id;
			this.name = name;
			this.owner = owner;
			this.description = description;
			this.xp = xp;
			this.members = members;
			this.blacklist = blacklist;
			this.pending = pending;
			this.invited = invited;
			this.accessLevel = accessLevel;
			this.createdOn = createdOn;
		}
		
		public int getId() { return id; }
		public String getName() { return name; }
		public UUID getOwner() { return owner; }
		public String getDescription() { return description; }
		public int getXP() { return xp; }
		public List<UUID> getMembers() { return members; }
		public List<UUID> getBlacklist() { return blacklist; }
		public List<UUID> getPending() { return pending; }
		public List<UUID> getInvited() { return invited; }
		public GuildAccessLevel getAccessLevel() { return accessLevel; }
		public Date getCreatedOn() { return createdOn; }
		
		public void save() { guildLoader.updateGuild(this); }
		
		public void setDescription(String description) {
			this.description = description;
			save();
		}
		
		public void setXP(int xp) {
			this.xp = xp;
			save();
		}
		
		public void addXP(int xp) {
			this.xp += xp;
			save();
		}
		
		public void setAccessLevel(GuildAccessLevel accessLevel) {
			this.accessLevel = accessLevel;
			save();
		}
		
		public void setOwner(UUID owner) {
			this.owner = owner;
			save();
		}
		
		public Document toDocument() {
			return new Document("_id", id).append("name", name).append("owner", owner).append("description", description)
					.append("xp", xp).append("members", members).append("blacklist", blacklist).append("pending", pending)
					.append("invited", invited).append("accessLevel", accessLevel.toString())
					.append("createdOn", createdOn.toInstant().getEpochSecond());
		}
		
		public static Guild fromDocument(Document document) {
			if(document == null) return null;
			int id = document.getInteger("_id");
			if(guildLoader.guildPool.containsKey(id)) return guildLoader.guildPool.get(id);
			Guild guild = new Guild(id, document.getString("name"), document.get("owner", UUID.class),
					document.getString("description"), document.getInteger("xp"), document.getList("members", UUID.class),
					document.getList("blacklist", UUID.class), document.getList("pending", UUID.class), 
					document.getList("invited", UUID.class), GuildAccessLevel.valueOf(document.getString("accessLevel")), 
					new Date(Instant.ofEpochSecond(document.getLong("createdOn")).toEpochMilli()));
			guildLoader.guildPool.put(id, guild);
			return guild;
		}
	}
	
	private Map<Integer, Guild> guildPool = new HashMap<>();
	private static final String GUILD_COLLECTION = "guilds";
	
	public GuildLoader(MongoConfig config) {
		super(config, "guilds", GUILD_COLLECTION);
	}

	public List<Guild> asGuilds(FindIterable<Document> guilds) {
		List<Document> result = new ArrayList<>();
		for(Document guild : guilds) {
			result.add(guild);
		}
		return asGuilds(result);
	}
	
	public List<Guild> asGuilds(List<Document> guilds) {
		return guilds.stream().map(doc -> Guild.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public List<Guild> getAllGuilds() {
		return asGuilds(collection.find());
	}
	
	public List<Guild> getAllGuildsWith(UUID uuid) {
		return asGuilds(collection.find(new Document("$or", Arrays.asList(new Document("members", uuid), new Document("owner", uuid)))));
	}
	
	public Guild getGuildByName(String name) {
		for(Guild guild : guildPool.values()) {
			if(guild.getName().equalsIgnoreCase(name)) {
				return guild;
			}
		}
		FindIterable<Document> result = collection.find(new Document("name", name));
		if(result.first() == null) return null;
		return Guild.fromDocument(result.first());
	}
	
	public Guild getGuildById(int id) {
		return guildPool.computeIfAbsent(id, guildId -> Guild.fromDocument(collection.find(new Document("_id", guildId)).first()));
	}
	
	public void updateGuild(Guild update) {
		collection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public Guild addGuild(UUID owner, String name) {
		name = name.replaceAll(Pattern.quote(" "), "");
		Date date = Date.from(Instant.now());
		int id = reserveNextId();
		Guild guild = new Guild(id, name, owner, "No description set", 0, new ArrayList<>(), 
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), GuildAccessLevel.INVITE, date);
		collection.insertOne(guild.toDocument());
		guildPool.put(guild.getId(), guild);
		return guild;
	}
	
	public void deleteGuild(int id) {
		collection.deleteOne(new Document("_id", id));
		guildPool.remove(id);
	}
	
	public int getLatestGuildId() {
		return getCurrentMaxId();
	}

}
