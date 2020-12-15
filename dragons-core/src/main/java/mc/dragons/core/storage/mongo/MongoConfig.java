package mc.dragons.core.storage.mongo;

import org.bson.UuidRepresentation;
import org.bukkit.configuration.ConfigurationSection;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import mc.dragons.core.Dragons;
import mc.dragons.core.storage.Counter;

public class MongoConfig {
	public static final String GAMEOBJECTS_COLLECTION = "gameobjects";
	public static final String SYSPROFILES_COLLECTION = "sysprofiles";
	public static final String FEEDBACK_COLLECTION = "feedback";
	public static final String WARP_COLLECTION = "warps";
	public static final String COUNTER_COLLECTION = "counters";
	public static final String CHANGELOG_COLLECTION = "changelogs";

	private MongoDatabase database;
	private Counter counter;

	public MongoConfig(Dragons instance) {
		ConfigurationSection dbConfig = instance.getConfig().getConfigurationSection("db.mongo");
		
		String user = dbConfig.getString("user");
		String password = dbConfig.getString("password");
		String host = dbConfig.getString("host");
		int port = dbConfig.getInt("port");
		String authDB = dbConfig.getString("auth_db");
		String database = dbConfig.getString("database");
		
		ConnectionString connectionString = new ConnectionString("mongodb://" + user + ":" + password + "@" + host + ":" + port + "/?authSource=" + authDB);
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).uuidRepresentation(UuidRepresentation.STANDARD).build();
		MongoClient client = MongoClients.create(settings);
		
		this.database = client.getDatabase(database);
		this.counter = new MongoCounter(this);
	}
		

	public MongoDatabase getDatabase() {
		return database;
	}

	public Counter getCounter() {
		return counter;
	}
}
