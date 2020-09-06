package mc.dragons.core.gameobject.npc;

import org.bson.Document;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.user.User;

public class NPCCondition {
	private static QuestLoader questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
	
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
		}
		return null;
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
		return this.type;
	}

	public boolean isInverse() {
		return this.inverse;
	}

	public Quest getQuest() {
		return this.quest;
	}

	public int getStageRequirement() {
		return this.stageRequirement;
	}

	public int getLevelRequirement() {
		return this.levelRequirement;
	}

	public double getGoldRequirement() {
		return this.goldRequirement;
	}

	public Document toDocument() {
		Document document = (new Document("type", this.type.toString())).append("inverse", Boolean.valueOf(this.inverse));
		switch (this.type) {
		case HAS_COMPLETED_QUEST:
			document.append("quest", this.quest.getName());
			break;
		case HAS_QUEST_STAGE:
			document.append("quest", this.quest.getName()).append("stage", Integer.valueOf(this.stageRequirement));
			break;
		case HAS_LEVEL:
			document.append("level", Integer.valueOf(this.levelRequirement));
			break;
		case HAS_GOLD:
			document.append("gold", Double.valueOf(this.goldRequirement));
			break;
		}
		return document;
	}

	public boolean test(User user) {
		QuestStep step, step2;
		boolean result = false;
		switch (this.type) {
		case HAS_COMPLETED_QUEST:
			step = user.getQuestProgress().get(this.quest);
			if (step == null) {
				result = false;
				break;
			}
			result = user.getQuestProgress().get(this.quest).getStepName().equals("Complete");
			break;
		case HAS_QUEST_STAGE:
			step2 = user.getQuestProgress().get(this.quest);
			if (step2 == null) {
				result = false;
				break;
			}
			result = (this.quest.getStepIndex(step2) >= this.stageRequirement);
			break;
		case HAS_LEVEL:
			result = (user.getLevel() >= this.levelRequirement);
			break;
		case HAS_GOLD:
			result = (user.getGold() >= this.goldRequirement);
			break;
		}
		return this.inverse ? (!result) : result;
	}
}
