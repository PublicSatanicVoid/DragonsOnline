package mc.dragons.core.storage;

import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;

/**
 * Uniquely identifies a StorageAccess, or the object
 * whose data is controlled by it.
 * 
 * @author Adam
 *
 */
public class Identifier {
	private Document identifierData;

	public Identifier(GameObjectType type, UUID uuid) {
		identifierData = new Document("type", type.toString()).append("_id", uuid);
	}

	public Document getDocument() {
		return identifierData;
	}

	public GameObjectType getType() {
		return GameObjectType.get(identifierData.getString("type"));
	}

	public UUID getUUID() {
		return (UUID) identifierData.get("_id");
	}

	@Override
	public String toString() {
		return getType().toString() + "#" + getUUID().toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!(other instanceof Identifier)) {
			return false;
		}
		Identifier otherId = (Identifier) other;
		return otherId.getType() == getType() && otherId.getUUID().equals(getUUID());
	}
}
