package mc.dragons.core.gameobject.quest;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import mc.dragons.core.gameobject.user.User;

public class QuestStep {
	private QuestTrigger trigger;
	private List<QuestAction> actions;
	private String stepName;
	private Quest quest;

	public QuestStep(String stepName, QuestTrigger trigger, List<QuestAction> actions, Quest quest) {
		this.stepName = stepName;
		this.trigger = trigger;
		this.actions = actions;
		this.quest = quest;
	}

	public static QuestStep fromDocument(Document step, Quest quest) {
		return new QuestStep(step.getString("stepName"), QuestTrigger.fromDocument((Document) step.get("trigger"), quest),
				(List<QuestAction>) step.getList("actions", Document.class).stream().map(d -> QuestAction.fromDocument(d, quest)).collect(Collectors.toList()), quest);
	}

	public Document toDocument() {
		return (new Document("stepName", this.stepName)).append("trigger", this.trigger.toDocument()).append("actions", this.actions.stream().map(a -> a.toDocument()).collect(Collectors.toList()));
	}

	public QuestTrigger getTrigger() {
		return this.trigger;
	}

	public void setTrigger(QuestTrigger trigger) {
		this.trigger = trigger;
		int stepIndex = this.quest.getStepIndex(this);
		List<Document> steps = this.quest.getStepsAsDoc();
		((Document) steps.get(stepIndex)).append("trigger", trigger.toDocument());
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addAction(QuestAction action) {
		this.actions.add(action);
		int stepIndex = this.quest.getStepIndex(this);
		List<Document> steps = this.quest.getStepsAsDoc();
		List<Document> actions = ((Document) steps.get(stepIndex)).getList("actions", Document.class);
		actions.add(action.toDocument());
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addDialogue(int actionIndex, String dialogue) {
		((QuestAction) this.actions.get(actionIndex)).getDialogue().add(dialogue);
		List<Document> steps = this.quest.getStepsAsDoc();
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public boolean removeDialogue(int actionIndex, int dialogueIndex) {
		List<String> dialogue = ((QuestAction) this.actions.get(actionIndex)).getDialogue();
		if (dialogue == null)
			return false;
		if (dialogue.size() >= dialogueIndex)
			return false;
		dialogue.remove(dialogueIndex);
		List<Document> steps = this.quest.getStepsAsDoc();
		this.quest.getStorageAccess().update(new Document("steps", steps));
		return true;
	}

	public void addBranchPoint(QuestTrigger trigger, QuestAction action) {
		this.trigger.getBranchPoints().put(trigger, action);
		int stepIndex = this.quest.getStepIndex(this);
		List<Document> steps = this.quest.getStepsAsDoc();
		((Document) ((Document) steps.get(stepIndex)).get("trigger", Document.class)).getList("branchPoints", Document.class)
				.add((new Document("trigger", trigger.toDocument())).append("action", action.toDocument()));
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addChoice(int actionIndex, String choiceText, int goToStage) {
		((QuestAction) this.actions.get(actionIndex)).getChoices().put(choiceText, Integer.valueOf(goToStage));
		List<Document> steps = this.quest.getRecalculatedStepsAsDoc();
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void deleteAction(int actionIndex) {
		this.actions.remove(actionIndex);
		int stepIndex = this.quest.getStepIndex(this);
		List<Document> steps = this.quest.getStepsAsDoc();
		((Document) steps.get(stepIndex)).getList("actions", Document.class).remove(actionIndex);
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public List<QuestAction> getActions() {
		return this.actions;
	}

	public String getStepName() {
		return this.stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
		int stepIndex = this.quest.getStepIndex(this);
		List<Document> steps = this.quest.getStepsAsDoc();
		((Document) steps.get(stepIndex)).append("stepName", stepName);
		this.quest.getStorageAccess().update(new Document("steps", steps));
	}

	public boolean executeActions(User user) {
		return executeActions(user, 0);
	}

	public boolean executeActions(User user, int beginIndex) {
		user.debug(" - Executing actions beginning at " + beginIndex);
		boolean shouldUpdateStage = true;
		for (int i = beginIndex; i < this.actions.size(); i++) {
			QuestAction action = this.actions.get(i);
			user.debug("   - Action type " + action.getActionType());
			QuestAction.QuestActionResult result = action.execute(user);
			if (result.wasStageModified()) {
				shouldUpdateStage = false;
			} else {
				user.updateQuestAction(this.quest, i + 1);
			}
			if (result.shouldPause()) {
				user.debug("   - Paused action execution after index " + i);
				shouldUpdateStage = false;
				int resumeIndex = i + 1;
				user.onDialogueComplete(u -> {
					u.updateQuestAction(quest, resumeIndex); // TODO: Do we need this? (139-142)
				});
				break;
			}
		}
		return shouldUpdateStage;
	}
}
