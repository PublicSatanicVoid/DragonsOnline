package mc.dragons.social;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import mc.dragons.core.storage.impl.MongoConfig;

public class GuildLoader {
	
	public static class Guild {
		private int id;
		private String name;
		private UUID owner;
		private String description;
		private int xp;
		private List<UUID> members;
		private Date createdOn;
		
		public Guild(int id, String name, UUID owner, String description, int xp, List<UUID> members, Date createdOn) {
			this.id = id;
			this.name = name;
			this.owner = owner;
			this.description = description;
			this.xp = xp;
			this.members = members;
			this.createdOn = createdOn;
		}
		
		public int getId() { return id; }
		public String getName() { return name; }
		public UUID getOwner() { return owner; }
		public String getDescription() { return description; }
		public int getXP() { return xp; }
		public List<UUID> getMembers() { return members; }
		public Date getCreatedOn() { return createdOn; }
		
		public void save() { GuildLoader.updateGuild(this); }
		
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
		
		public Document toDocument() {
			return new Document("_id", id).append("name", name).append("owner", owner).append("description", description)
					.append("xp", xp).append("members", members).append("createdOn", createdOn.toInstant().getEpochSecond());
		}
		
		public static Guild fromDocument(Document document) {
			if(document == null) return null;
			int id = document.getInteger("_id");
			if(guildPool.containsKey(id)) return guildPool.get(id);
			Guild guild = new Guild(document.getInteger("_id"), document.getString("name"), document.get("owner", UUID.class),
					document.getString("description"), document.getInteger("xp"), document.getList("members", UUID.class),
					new Date(Instant.ofEpochSecond(document.getLong("createdOn")).toEpochMilli()));
			guildPool.put(id, guild);
			return guild;
		}
	}
	
	private static MongoDatabase database;
	private static MongoCollection<Document> guildCollection;
	
	private static Map<Integer, Guild> guildPool = new HashMap<>();
	
	private static final String GUILD_COLLECTION = "guilds";
	
	static {
		database = MongoConfig.getDatabase();
		guildCollection = database.getCollection(GUILD_COLLECTION);
	}

	public static List<Guild> asGuilds(FindIterable<Document> guilds) {
		List<Document> result = new ArrayList<>();
		for(Document guild : guilds) {
			result.add(guild);
		}
		return asGuilds(result);
	}
	
	public static List<Guild> asGuilds(List<Document> tasks) {
		return tasks.stream().map(doc -> Guild.fromDocument(doc)).sorted((a, b) -> a.getId() - b.getId()).collect(Collectors.toList());
	}
	
	public static List<Guild> getAllGuilds() {
		return asGuilds(guildCollection.find());
	}
	
	public static List<Guild> getAllGuildsWith(UUID uuid) {
		return asGuilds(guildCollection.find(new Document("$or", Arrays.asList(new Document("members", uuid), new Document("owner", uuid)))));
	}
	
	public static Guild getGuildById(int id) {
		return guildPool.computeIfAbsent(id, guildId -> Guild.fromDocument(guildCollection.find(new Document("_id", guildId)).first()));
	}
	
	public static void updateGuild(Guild update) {
		guildCollection.updateOne(new Document("_id", update.getId()), new Document("$set", update.toDocument()));
	}
	
	public static Guild addGuild(UUID owner, String name) {
		Date date = Date.from(Instant.now());
		int id = MongoConfig.getCounter().reserveNextId("guilds");
		Guild guild = new Guild(id, name, owner, "No description set", 0, new ArrayList<>(), date);
		guildCollection.insertOne(guild.toDocument());
		guildPool.put(guild.getId(), guild);
		return guild;
	}
	
	public static void deleteGuild(int id) {
		guildCollection.deleteOne(new Document("_id", id));
		guildPool.remove(id);
	}
	
	public static int getLatestGuildId() {
		return MongoConfig.getCounter().getCurrentId("guilds");
	}

}
