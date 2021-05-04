package mc.dragons.tools.content.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.logging.correlation.CorrelationLogger;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.StringUtil;

/**
 * Utilities for managing game object metadata.
 * Metadata is information about the creation and
 * revision history of a game object.
 * 
 * @author Adam
 *
 */
public class MetadataConstants {
	
	private static final UserLoader userLoader = GameObjectType.USER.getLoader();
	private static final CorrelationLogger CORRELATION = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(CorrelationLogger.class);
	
	public static final String METADATA_NAMESPACE = "x";
	
	public static final String CREATEDBY_TAG = "createdBy";
	public static final String CREATEDON_TAG = "createdOn";
	public static final String REVISIONS_TAG = "revisions";
	public static final String LASTREVISEDBY_TAG = "lastRevisedBy";
	public static final String LASTREVISEDON_TAG = "lastRevisedOn";
	public static final String LASTPUSHEDON_TAG = "lastPushedOn";
	public static final String LASTPUSHEDREV_TAG = "lastPushedRev";
	public static final String AUDITLOG_TAG = "auditLog";
	
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
	
	public static final String getDateFormatNow() {
		return DATE_FORMATTER.format(new Date());
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
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = new Document();
		
		if(creator != null) {
			metadata.append(LASTREVISEDBY_TAG, creator.getUUID().toString());
			metadata.append(CREATEDBY_TAG, creator.getUUID().toString());
		}
		metadata.append(AUDITLOG_TAG, new ArrayList<>());
		metadata.append(CREATEDON_TAG, getDateFormatNow());
		metadata.append(REVISIONS_TAG, 0);
		metadata.append(LASTREVISEDON_TAG, getDateFormatNow());
		storageAccess.set(METADATA_NAMESPACE, metadata);
	}
	
	/**
	 * Log a revision to the specified game object, performed
	 * by the specified user.
	 * 
	 * This method, if used on its own to signify a change to
	 * this object, will break revertibility.
	 * 
	 * @param obj
	 * @param user
	 */
	public static final void incrementRevisionCount(GameObject obj, User user) {
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = storageAccess.getDocument().get(METADATA_NAMESPACE, new Document());
		metadata.append(REVISIONS_TAG, 1 + metadata.getInteger(REVISIONS_TAG, 0));

		if(user != null) {
			metadata.append(LASTREVISEDBY_TAG, user.getUUID().toString());
		}
		metadata.append(LASTREVISEDON_TAG, getDateFormatNow());
		storageAccess.set(METADATA_NAMESPACE, metadata);
	}
	
	/**
	 * Log a revision to the specified game object, performed
	 * by the specified user.
	 * 
	 * Must use this method to guarantee revertibility.
	 * 
	 * @param obj
	 * @param user
	 * @param base
	 * @param line
	 */
	public static final void logRevision(GameObject obj, User user, Document base, String line) {
		incrementRevisionCount(obj, user);
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = storageAccess.getDocument().get(METADATA_NAMESPACE, new Document());
		List<Document> auditLog = metadata.getList(AUDITLOG_TAG, Document.class);
		if(auditLog == null) auditLog = new ArrayList<>();
		Document baseSafe = Document.parse(base.toJson());
		baseSafe.remove(METADATA_NAMESPACE);
		Document newSafe = Document.parse(obj.getData().toJson());
		newSafe.remove(METADATA_NAMESPACE);
		auditLog.add(StorageUtil.getDelta(newSafe, baseSafe).append("by", user == null ? null : user.getUUID().toString()).append("on", getDateFormatNow()).append("desc", line));
		storageAccess.set(METADATA_NAMESPACE, metadata);
	}
	
	/**
	 * Log a revision to the specified game object, performed
	 * by the specified user.
	 * 
	 * This method, if used on its own to signify a change to
	 * this object, will break revertibility.
	 * 
	 * @param obj
	 * @param user
	 * @param line
	 */
	public static final void logRevision(GameObject obj, User user, String line) {
		incrementRevisionCount(obj, user);
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = storageAccess.getDocument().get(METADATA_NAMESPACE, new Document());
		List<Document> auditLog = metadata.getList(AUDITLOG_TAG, Document.class);
		if(auditLog == null) auditLog = new ArrayList<>();
		auditLog.add(new Document("+", new Document()).append("-", new ArrayList<>()).append("by", user == null ? null : user.getUUID().toString()).append("on", getDateFormatNow()).append("desc", line));
	}
	
	/**
	 * Log the specified game object being pushed to production staging.
	 * 
	 * @param obj
	 */
	public static final void logPush(GameObject obj) {
		StorageAccess storageAccess = obj.getStorageAccess();
		Document metadata = storageAccess.getDocument().get(METADATA_NAMESPACE, new Document());
		Integer revision = metadata.getInteger(REVISIONS_TAG, 0);
		revision++;
		metadata.append(LASTPUSHEDON_TAG, getDateFormatNow());
		metadata.append(LASTPUSHEDREV_TAG, revision);
		metadata.append(REVISIONS_TAG, revision);
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
		String createdBy = metadata.getString(CREATEDBY_TAG);
		String createdOn = metadata.getString(CREATEDON_TAG);
		Integer revisions = metadata.getInteger(REVISIONS_TAG);
		String revisedBy = metadata.getString(LASTREVISEDBY_TAG);
		String revisedOn = metadata.getString(LASTREVISEDON_TAG);
		String pushedOn = metadata.getString(LASTPUSHEDON_TAG);
		Integer pushRevision = metadata.getInteger(LASTPUSHEDREV_TAG);
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
		if(pushedOn != null) {
			to.sendMessage(ChatColor.GRAY + "Last Pushed On: " + ChatColor.GREEN + pushedOn);
		}
		if(pushRevision != null && revisions != null) {
			if(revisions.equals(pushRevision)) {
				to.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.GREEN + "Matched in production");
			}
			else if(revisions > pushRevision) {
				to.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.YELLOW + "Changes made since last push");
			}
			else {
				to.sendMessage(ChatColor.GRAY + "Sync Status: " + ChatColor.RED + "Behind production (SYNC ERROR)");
				UUID cid = CORRELATION.registerNewCorrelationID();
				CORRELATION.log(cid, Level.WARNING, "Game object " + obj.getIdentifier().toString() + " has production changes not reflected in preproduction (Prod=" + pushRevision + " > Preprod=" + revisions + ")");
				to.sendMessage(ChatColor.RED + "Please report this issue immediately. " + StringUtil.toHdFont("Correlation ID: " + cid));
			}
		}
	}
}
