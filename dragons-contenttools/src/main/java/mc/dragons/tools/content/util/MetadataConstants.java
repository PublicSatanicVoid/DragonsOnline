package mc.dragons.tools.content.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageAccess;

/**
 * Utilities for managing game object metadata.
 * Metadata is information about the creation and
 * revision history of a game object.
 * 
 * @author Adam
 *
 */
public class MetadataConstants {
	
	private static final UserLoader userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
	
	public static final String METADATA_NAMESPACE = "x";
	
	public static final String CREATEDBY_TAG = "createdBy";
	public static final String CREATEDON_TAG = "createdOn";
	public static final String REVISIONS_TAG = "revisions";
	public static final String LASTREVISEDBY_TAG = "lastRevisedBy";
	public static final String LASTREVISEDON_TAG = "lastRevisedOn";
	
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
	
	public static final String getDateFormatNow() {
		return DATE_FORMATTER.format(Date.from(Instant.now()));
	}
	
	/**
	 * Add a metadata tag to the specified game object.
	 * Initializes based on current datetime and specified
	 * creator.
	 * 
	 * @param obj
	 * @param creator
	 */
	public static final void addBlankMetadata(GameObject obj, User creator) {
		if(creator == null) return; // console, probably
		
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = new Document();
		metadata.append(CREATEDBY_TAG, creator.getUUID().toString());
		metadata.append(CREATEDON_TAG, getDateFormatNow());
		metadata.append(REVISIONS_TAG, 0);
		metadata.append(LASTREVISEDBY_TAG, creator.getUUID().toString());
		metadata.append(LASTREVISEDON_TAG, getDateFormatNow());
		storageAccess.set(METADATA_NAMESPACE, metadata);
	}
	
	/**
	 * Log a revision to the specified game object, performed
	 * by the specified user.
	 * 
	 * @param obj
	 * @param user
	 */
	public static final void incrementRevisionCount(GameObject obj, User user) {
		if(user == null) return; // console, probably
		
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = storageAccess.getDocument().get(METADATA_NAMESPACE, new Document());
		metadata.append(REVISIONS_TAG, 1 + metadata.getInteger(REVISIONS_TAG, 0));
		metadata.append(LASTREVISEDBY_TAG, user.getUUID().toString());
		metadata.append(LASTREVISEDON_TAG, getDateFormatNow());
		storageAccess.set(METADATA_NAMESPACE, metadata);
	}
	
	/**
	 * Return the raw document of metadata associated with the specified
	 * game object.
	 * 
	 * @param obj
	 * @return The metadata document, or an empty document if none is found.
	 */
	public static final Document getMetadata(GameObject obj) {
		return (Document) obj.getData().getOrDefault(METADATA_NAMESPACE, new Document());
	}
	
	/**
	 * Display all metadata associated with the specified game object to
	 * the specified command sender.
	 * 
	 * @param to
	 * @param obj
	 */
	public static final void displayMetadata(CommandSender to, GameObject obj) {
		Document metadata = getMetadata(obj);
		if(metadata.isEmpty()) {
			to.sendMessage(ChatColor.DARK_GRAY + "No metadata to display.");
			return;
		}
		to.sendMessage(ChatColor.DARK_GRAY + "Object Metadata");
		String createdBy = metadata.getString(MetadataConstants.CREATEDBY_TAG);
		String createdOn = metadata.getString(MetadataConstants.CREATEDON_TAG);
		Integer revisions = metadata.getInteger(MetadataConstants.REVISIONS_TAG);
		String revisedBy = metadata.getString(MetadataConstants.LASTREVISEDBY_TAG);
		String revisedOn = metadata.getString(MetadataConstants.LASTREVISEDON_TAG);
		if(createdBy != null) {
			to.sendMessage(ChatColor.GRAY + "Created By: " + ChatColor.GREEN + userLoader.loadObject(UUID.fromString(createdBy)).getName());
		}
		if(createdOn != null) {
			to.sendMessage(ChatColor.GRAY + "Created On: " + ChatColor.GREEN + createdOn);
		}
		if(revisions != null) {
			to.sendMessage(ChatColor.GRAY + "Revision Count: " + ChatColor.GREEN + revisions + (createdBy == null ? " or more" : ""));
		}
		if(revisedBy != null) {
			to.sendMessage(ChatColor.GRAY + "Last Revised By: " + ChatColor.GREEN + userLoader.loadObject(UUID.fromString(revisedBy)).getName());
		}
		if(revisedOn != null) {
			to.sendMessage(ChatColor.GRAY + "Last Revised On: " + ChatColor.GREEN + revisedOn);
		}
	}
}
