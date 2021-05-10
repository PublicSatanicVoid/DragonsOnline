package mc.dragons.social.party;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.loader.AbstractLightweightLoader;
import mc.dragons.social.party.PartyLoader.Party;

public class PartyLoader extends AbstractLightweightLoader<Party> {
	private UserLoader userLoader = GameObjectType.USER.getLoader();
	private Map<Integer, Party> partyPool = new HashMap<>();
	private PartyMessageHandler messenger;
	private Dragons dragons;
	
	public PartyLoader(Dragons instance) {
		super(instance.getMongoConfig(), "party", "party");
		dragons = instance;
	}
	
	public void loadMessenger() {
		if(messenger == null) {
			messenger = new PartyMessageHandler(dragons);
		}
	}

	public class Party {
		private Document data;
		protected Party(Document data) {
			this.data = data;
		}
		
		private void save() {
			collection.updateOne(new Document("_id", getId()), new Document("$set", data));
		}
		
		public int getId() {
			return data.getInteger("_id");
		}
		
		public List<User> getMembers() {
			return data.getList("members", UUID.class).stream().map(uuid -> userLoader.loadObject(uuid)).collect(Collectors.toList());
		}
		
		public List<User> getInvites() {
			return data.getList("invites", UUID.class).stream().map(uuid -> userLoader.loadObject(uuid)).collect(Collectors.toList());
		}
		
		public void invite(User user) {
			data.getList("invites", UUID.class).add(user.getUUID());
			save();
		}
		
		public void uninvite(User user) {
			data.getList("invites", UUID.class).remove(user.getUUID());
			save();
		}
		
		public boolean hasUser(User user) {
			return getMembers().contains(user) || getOwner().equals(user);
		}
		
		public void leave(User user) {
			data.getList("members", UUID.class).remove(user.getUUID());
			save();
		}
		
		public void join(User user) {
			data.getList("members", UUID.class).add(user.getUUID());
			uninvite(user);
			save();
		}
		
		public User getOwner() {
			return userLoader.loadObject(data.get("owner", UUID.class));
		}
		
		public void setOwner(User user) {
			data.append("owner", UUID.class);
			save();
		}
		
		public long getCreatedOn() {
			return data.getLong("createdOn");
		}
	}
	
	public Party fromDocument(Document data) {
		if(data == null) return null;
		if(!data.containsKey("_id")) return null;
		int id = data.getInteger("_id");
		return partyPool.computeIfAbsent(id, unused -> new Party(data));
	}
	
	public Party newParty(User owner) {
		int id = reserveNextId();
		Document data = new Document("_id", id)
			.append("members", new ArrayList<>())
			.append("invites", new ArrayList<>())
			.append("owner", owner.getUUID())
			.append("createdOn", System.currentTimeMillis());
		collection.insertOne(data);
		return fromDocument(data);
	}
	
	public Party get(int id) {
		FindIterable<Document> party = collection.find(new Document("_id", id));
		return fromDocument(party.first());
	}
	
	public Party of(User user) {
		UUID uuid = user.getUUID();
		FindIterable<Document> party = collection.find(new Document("$or", List.of(new Document("members", new Document("$in", List.of(uuid))), new Document("owner", uuid))));
		return fromDocument(party.first());
	}
	
	public List<Party> invitesWith(User user) {
		FindIterable<Document> party = collection.find(new Document("invites", new Document("$in", List.of(user.getUUID()))));
		return party.map(d -> fromDocument(d)).into(new ArrayList<>());
	}
	
	public void kick(Party party, User member) {
		loadMessenger();
		party.leave(member);
		messenger.sendKick(party.getId(), member);
	}
	
	public void invite(Party party, User target) {
		loadMessenger();
		party.invite(target);
		messenger.sendInvite(party.getId(), target);
	}
	
	public void join(Party party, User member) {
		loadMessenger();
		party.join(member);
		party.uninvite(member);
		messenger.sendJoin(party.getId(), member);
	}
	
	public void leave(Party party, User member) {
		loadMessenger();
		party.leave(member);
		messenger.sendLeave(party.getId(), member);
	}
	
	public void unpool(int id) {
		partyPool.remove(id);
	}
	
	public void disband(Party party) {
		loadMessenger();
		messenger.sendDisband(party.getId(), party.getOwner());
		sync(() -> collection.deleteOne(new Document("_id", party.getId())), 20);
	}	
}
