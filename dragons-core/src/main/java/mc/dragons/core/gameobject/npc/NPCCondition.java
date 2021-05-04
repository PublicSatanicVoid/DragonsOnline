package mc.dragons.core.gameobject.npc;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;

/**
 * A condition, or a predicate on the user who initiated it, which must be fulfilled for
 * one or more associated behavioral actions of the targeted NPC to be performed.
 * 
 * @author Adam
 *
 */
public class NPCCondition {
	private static QuestLoader questLoader = GameObjectType.QUEST.getLoader();
	
	private NPCConditionType type;
	private boolean inverse;

	private Quest quest;

	private int stageRequirement;
	private int levelRequirement;
	private double goldRequirement;

	public enum NPCConditionType {
		HAS_COMPLETED_QUEST, HAS_QUEST_STAGE, HAS_LEVEL, HAS_GOLD;
	}

	public static NPCCondition fromDocument(Document document) {
		NPCConditionType type = NPCConditionType.valueOf(document.getString("type"));
		switch (type) {
		case HAS_COMPLETED_QUEST:
			return hasCompletedQuest(questLoader.getQuestByName(document.getString("quest")), document.getBoolean("inverse").booleanValue());
		case HAS_QUEST_STAGE:
			return hasQuestStage(questLoader.getQuestByName(document.getString("quest")), document.getInteger("stage").intValue(), document.getBoolean("inverse").booleanValue());
		case HAS_LEVEL:
			return hasLevel(document.getInteger("level").intValue(), document.getBoolean("inverse").booleanValue());
		case HAS_GOLD:
			return hasGold(document.getDouble("gold").doubleValue(), document.getBoolean("inverse").booleanValue());
		default:
			return null;
		}
	}

	public static NPCCondition hasCompletedQuest(Quest quest, boolean inverse) {
		NPCCondition cond = new NPCCondition();
		cond.type = NPCConditionType.HAS_COMPLETED_QUEST;
		cond.quest = quest;
		cond.inverse = inverse;
		return cond;
	}

	public static NPCCondition hasQuestStage(Quest quest, int stage, boolean inverse) {
		NPCCondition cond = new NPCCondition();
		cond.type = NPCConditionType.HAS_QUEST_STAGE;
		cond.quest = quest;
		cond.stageRequirement = stage;
		cond.inverse = inverse;
		return cond;
	}

	public static NPCCondition hasLevel(int levelReq, boolean inverse) {
		NPCCondition cond = new NPCCondition();
		cond.type = NPCConditionType.HAS_LEVEL;
		cond.levelRequirement = levelReq;
		cond.inverse = inverse;
		return cond;
	}

	public static NPCCondition hasGold(double goldReq, boolean inverse) {
		NPCCondition cond = new NPCCondition();
		cond.type = NPCConditionType.HAS_GOLD;
		cond.goldRequirement = goldReq;
		cond.inverse = inverse;
		return cond;
	}

	public NPCConditionType getType() {
		return type;
	}

	public boolean isInverse() {
		return inverse;
	}

	public Quest getQuest() {
		return quest;
	}

	public int getStageRequirement() {
		return stageRequirement;
	}

	public int getLevelRequirement() {
		return levelRequirement;
	}

	public double getGoldRequirement() {
		return goldRequirement;
	}

	public Document toDocument() {
		Document document = new Document("type", type.toString()).append("inverse", Boolean.valueOf(inverse));
		switch (type) {
		case HAS_COMPLETED_QUEST:
			document.append("quest", quest.getName());
			break;
		case HAS_QUEST_STAGE:
			document.append("quest", quest.getName()).append("stage", Integer.valueOf(stageRequirement));
			break;
		case HAS_LEVEL:
			document.append("level", Integer.valueOf(levelRequirement));
			break;
		case HAS_GOLD:
			document.append("gold", Double.valueOf(goldRequirement));
			break;
		}
		return document;
	}

	/**
	 * 
	 * @param user
	 * @return Whether this condition is fulfilled if triggered by the specified user.
	 */
	public boolean test(User user) {
		QuestStep step;
		boolean result = false;
		switch (type) {
		case HAS_COMPLETED_QUEST:
			step = user.getQuestProgress().get(quest);
			if (step == null) {
				result = false;
				break;
			}
			result = user.getQuestProgress().get(quest).getStepName().equals("Complete");
			break;
		case HAS_QUEST_STAGE:
			step = user.getQuestProgress().get(quest);
			if (step == null) {
				result = false;
				break;
			}
			result = quest.getStepIndex(step) >= stageRequirement;
			break;
		case HAS_LEVEL:
			result = user.getLevel() >= levelRequirement;
			break;
		case HAS_GOLD:
			result = user.getGold() >= goldRequirement;
			break;
		}
		return inverse ? !result : result;
	}
}
