package mc.dragons.core.storage;

import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;

public class Identifier {
	private Document identifierData;

	public Identifier(GameObjectType type, UUID uuid) {
		this.identifierData = (new Document("type", type.toString())).append("_id", uuid);
	}

	public Document getDocument() {
		return this.identifierData;
	}

	public GameObjectType getType() {
		return GameObjectType.get(this.identifierData.getString("type"));
	}

	public UUID getUUID() {
		return (UUID) this.identifierData.get("_id");
	}

	public String toString() {
		return String.valueOf(getType().toString()) + "#" + getUUID().toString();
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Identifier))
			return false;
		Identifier otherId = (Identifier) other;
		return (otherId.getType() == getType() && otherId.getUUID().equals(getUUID()));
	}
}
