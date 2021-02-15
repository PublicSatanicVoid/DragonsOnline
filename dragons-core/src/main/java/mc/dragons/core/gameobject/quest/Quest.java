package mc.dragons.core.gameobject.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

/**
 * Represents a quest in the game. There should only be
 * one instance of a quest per quest.
 * 
 * <p>A quest is defined in terms of a series of steps,
 * each of which is associated with a specific trigger,
 * such as clicking on an NPC or entering a region.
 * 
 * <p>Players can obtain skill points, items, and XP from
 * completing quests.
 * 
 * <p>The last step in a quest should be named "Complete"
 * and should represent that the player has completed the
 * quest.
 * 
 * @author Adam
 *
 */
public class Quest extends GameObject {
	public enum QuestPauseState {
		NORMAL, PAUSED, RESUMED;
	}
	
	private List<QuestStep> steps;
	private Table<User, String, NPC> referenceNames;

	public Quest(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constructing quest (" + storageManager + ", " + storageAccess + ")");
		steps = new ArrayList<>();
		referenceNames = HashBasedTable.create();
		@SuppressWarnings("unchecked")
		List<Document> rawSteps = (List<Document>) getData("steps");
		for (Document step : rawSteps) {
			steps.add(QuestStep.fromDocument(step, this));
		}
	}

	public List<Document> getStepsAsDoc() {
		return getStorageAccess().getDocument().getList("steps", Document.class);
	}

	public List<Document> getRecalculatedStepsAsDoc() {
		return steps.stream().map(step -> step.toDocument()).collect(Collectors.toList());
	}

	public int getStepIndex(QuestStep questStep) {
		return getSteps().indexOf(questStep);
	}

	public void addStep(QuestStep step) {
		steps.add(step);
		List<Document> stepsDoc = getStepsAsDoc();
		stepsDoc.add(step.toDocument());
		update(new Document("steps", stepsDoc));
	}

	public void delStep(int step) {
		List<Document> stepsDoc = getStepsAsDoc();
		stepsDoc.remove(step);
		steps.remove(step);
		update(new Document("steps", stepsDoc));
	}

	public List<QuestStep> getSteps() {
		return steps;
	}

	public void registerNPCReference(User user, NPC npc, String referenceName) {
		referenceNames.put(user, referenceName, npc);
	}

	public NPC getNPCByReference(User user, String referenceName) {
		return referenceNames.get(user, referenceName);
	}

	public String getQuestName() {
		return (String) getData("questName");
	}

	public void setQuestName(String questName) {
		setData("questName", questName);
	}

	public String getName() {
		return (String) getData("name");
	}

	public int getLevelMin() {
		return (int) getData("lvMin");
	}

	public void setLevelMin(int lvMin) {
		setData("lvMin", Integer.valueOf(lvMin));
	}

	public boolean isValid() {
		if (steps.size() == 0) {
			return false;
		}
		QuestStep finalStep = steps.get(steps.size() - 1);
		if (!finalStep.getStepName().equalsIgnoreCase("Complete")) {
			return false;
		}
		if (finalStep.getTrigger().getTriggerType() != QuestTrigger.TriggerType.INSTANT) {
			return false;
		}
		if (finalStep.getActions().size() != 0) {
			return false;
		}
		return true;
	}
}
