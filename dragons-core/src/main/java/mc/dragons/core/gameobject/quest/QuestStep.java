package mc.dragons.core.gameobject.quest;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.util.PermissionUtil;

/**
 * A named stage of a quest. Once a user gets to this stage,
 * the game waits for them to meet the trigger condition, and
 * then the associated actions are executed and the user moves
 * on to the next stage.
 * 
 * @author Adam
 *
 */
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
				step.getList("actions", Document.class).stream().map(d -> QuestAction.fromDocument(d, quest)).collect(Collectors.toList()), quest);
	}

	public Document toDocument() {
		return new Document("stepName", stepName).append("trigger", trigger.toDocument()).append("actions", actions.stream().map(a -> a.toDocument()).collect(Collectors.toList()));
	}

	public QuestTrigger getTrigger() {
		return trigger;
	}

	public void setTrigger(QuestTrigger trigger) {
		this.trigger = trigger;
		int stepIndex = quest.getStepIndex(this);
		List<Document> steps = quest.getStepsAsDoc();
		steps.get(stepIndex).append("trigger", trigger.toDocument());
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addAction(QuestAction action) {
		actions.add(action);
		int stepIndex = quest.getStepIndex(this);
		List<Document> steps = quest.getStepsAsDoc();
		List<Document> actions = steps.get(stepIndex).getList("actions", Document.class);
		actions.add(action.toDocument());
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addDialogue(int actionIndex, String dialogue) {
		actions.get(actionIndex).getDialogue().add(dialogue);
		List<Document> steps = quest.getStepsAsDoc();
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public boolean removeDialogue(int actionIndex, int dialogueIndex) {
		List<String> dialogue = actions.get(actionIndex).getDialogue();
		if (dialogue == null) {
			return false;
		}
		if (dialogue.size() >= dialogueIndex) {
			return false;
		}
		dialogue.remove(dialogueIndex);
		List<Document> steps = quest.getStepsAsDoc();
		quest.getStorageAccess().update(new Document("steps", steps));
		return true;
	}

	public void addBranchPoint(QuestTrigger trigger, QuestAction action) {
		this.trigger.getBranchPoints().put(trigger, action);
		int stepIndex = quest.getStepIndex(this);
		List<Document> steps = quest.getStepsAsDoc();
		steps.get(stepIndex).get("trigger", Document.class).getList("branchPoints", Document.class)
				.add(new Document("trigger", trigger.toDocument()).append("action", action.toDocument()));
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void addChoice(int actionIndex, String choiceText, int goToStage) {
		actions.get(actionIndex).getChoices().put(choiceText, Integer.valueOf(goToStage));
		List<Document> steps = quest.getRecalculatedStepsAsDoc();
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public void deleteAction(int actionIndex) {
		actions.remove(actionIndex);
		int stepIndex = quest.getStepIndex(this);
		List<Document> steps = quest.getStepsAsDoc();
		steps.get(stepIndex).getList("actions", Document.class).remove(actionIndex);
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public List<QuestAction> getActions() {
		return actions;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
		int stepIndex = quest.getStepIndex(this);
		List<Document> steps = quest.getStepsAsDoc();
		steps.get(stepIndex).append("stepName", stepName);
		quest.getStorageAccess().update(new Document("steps", steps));
	}

	public boolean executeActions(User user) {
		return executeActions(user, 0);
	}

	public boolean executeActions(User user, int beginIndex) {
		if(quest.isLocked() && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false)) return false;
		user.debug(" - Executing actions beginning at " + beginIndex);
		boolean shouldUpdateStage = true;
		for (int i = beginIndex; i < actions.size(); i++) {
			QuestAction action = actions.get(i);
			user.debug("   - Action type " + action.getActionType());
			QuestAction.QuestActionResult result = action.execute(user);
			if (result.wasStageModified()) {
				shouldUpdateStage = false;
			} else {
				user.updateQuestAction(quest, i + 1);
			}
			if (result.shouldPause()) {
				user.debug("   - Paused action execution after index " + i);
				shouldUpdateStage = false;
				int resumeIndex = i + 1;
				user.onDialogueComplete(u -> {
					u.updateQuestAction(quest, resumeIndex); // TODO: Do we need this?
				});
				break;
			}
		}
		return shouldUpdateStage;
	}
}
