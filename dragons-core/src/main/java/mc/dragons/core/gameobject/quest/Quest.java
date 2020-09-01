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

public class Quest extends GameObject {
	private List<QuestStep> steps;

	private Table<User, String, NPC> referenceNames;

	public Quest(StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.fine("Constructing quest (" + storageManager + ", " + storageAccess + ")");
		this.steps = new ArrayList<>();
		this.referenceNames = HashBasedTable.create();
		@SuppressWarnings("unchecked")
		List<Document> rawSteps = (List<Document>) getData("steps");
		for (Document step : rawSteps)
			this.steps.add(QuestStep.fromDocument(step, this));
	}

	public List<Document> getStepsAsDoc() {
		return getStorageAccess().getDocument().getList("steps", Document.class);
	}

	public List<Document> getRecalculatedStepsAsDoc() {
		return this.steps.stream().map(step -> step.toDocument()).collect(Collectors.toList());
	}

	public int getStepIndex(QuestStep questStep) {
		return getSteps().indexOf(questStep);
	}

	public void addStep(QuestStep step) {
		this.steps.add(step);
		List<Document> stepsDoc = getStepsAsDoc();
		stepsDoc.add(step.toDocument());
		update(new Document("steps", stepsDoc));
	}

	public void delStep(int step) {
		List<Document> stepsDoc = getStepsAsDoc();
		stepsDoc.remove(step);
		this.steps.remove(step);
		update(new Document("steps", stepsDoc));
	}

	public List<QuestStep> getSteps() {
		return this.steps;
	}

	public void registerNPCReference(User user, NPC npc, String referenceName) {
		this.referenceNames.put(user, referenceName, npc);
	}

	public NPC getNPCByReference(User user, String referenceName) {
		return this.referenceNames.get(user, referenceName);
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
		if (this.steps.size() == 0)
			return false;
		QuestStep finalStep = this.steps.get(this.steps.size() - 1);
		if (!finalStep.getStepName().equalsIgnoreCase("Complete"))
			return false;
		if (finalStep.getTrigger().getTriggerType() != QuestTrigger.TriggerType.INSTANT)
			return false;
		if (finalStep.getActions().size() != 0)
			return false;
		return true;
	}
}
