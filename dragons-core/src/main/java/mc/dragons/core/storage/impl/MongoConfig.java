package mc.dragons.core.storage.impl;

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
	
	private String USER;
	private String PASSWORD;
	private String HOST;
	private int PORT;
	private String AUTH_DB;
	private String DATABASE;

	private MongoDatabase database;
	private Counter counter;

	public MongoConfig(Dragons instance) {
		ConfigurationSection dbConfig = instance.getConfig().getConfigurationSection("db.mongo");
		
		USER = dbConfig.getString("user");
		PASSWORD = dbConfig.getString("password");
		HOST = dbConfig.getString("host");
		PORT = dbConfig.getInt("port");
		AUTH_DB = dbConfig.getString("auth_db");
		DATABASE = dbConfig.getString("database");
		ConnectionString connectionString = new ConnectionString("mongodb://" + USER + ":" + PASSWORD + "@" + HOST + ":" + PORT + "/?authSource=" + AUTH_DB);
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).uuidRepresentation(UuidRepresentation.STANDARD).build();
		MongoClient client = MongoClients.create(settings);
		database = client.getDatabase(DATABASE);
		counter = new MongoCounter(this);
	}
		

	public MongoDatabase getDatabase() {
		return database;
	}

	public Counter getCounter() {
		return counter;
	}
}
