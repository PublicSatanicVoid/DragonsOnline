package mc.dragons.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;

import com.google.common.base.Preconditions;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestAction;
import mc.dragons.core.gameobject.quest.QuestAction.QuestActionType;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.quest.QuestTrigger;
import mc.dragons.core.gameobject.quest.QuestTrigger.TriggerType;

public class QuestBuilder {
	public class B {
		private String shortName;
		private String questName;
		private int lvMin;
		private List<O> objectives;
		private Map<String, O> namedObjectives;
		
		protected B() {
			this.objectives = new ArrayList<>();
			this.namedObjectives = new HashMap<>();
		}
		
		public B setShortName(String shortName) {
			this.shortName = shortName;
			return this;
		}
		
		public B setQuestName(String questName) {
			this.questName = questName;
			return this;
		}
		
		public B setLvMin(int lvMin) {
			this.lvMin = lvMin;
			return this;
		}
		
		public B addObjective(O objective) {
			objectives.add(objective);
			if(objective.internalName != null) {
				namedObjectives.put(objective.internalName, objective);
			}
			return this;
		}
		
		public Quest build() {
			Preconditions.checkNotNull(shortName, "Short name must not be null");
			Preconditions.checkNotNull(questName, "Quest name must not be null");
			Preconditions.checkNotNull(objectives, "Objective name must not be null");
			Preconditions.checkState(objectives.size() > 0, "Must have at least one objective");
			Quest quest = GameObjectType.QUEST.<Quest, QuestLoader>getLoader().registerNew(shortName, questName, lvMin);
			for(int i = 0; i < objectives.size(); i++) {
				O objective = objectives.get(i);
				quest.addStep(objective.build(quest, i, namedObjectives));
			}
			return quest;
		}
	}
	
	public class O {
		private String internalName;
		private String objectiveName;
		private TriggerType triggerType;
		private Document triggerData;
		private List<A> actions;
		
		protected O() {
			actions = new ArrayList<>();
		}
		
		public O setInternalName(String internalName) {
			this.internalName = internalName;
			return this;
		}
		
		public O setObjectiveName(String objectiveName) {
			this.objectiveName = objectiveName;
			return this;
		}
		
		public O setTriggerType(TriggerType triggerType) {
			this.triggerType = triggerType;
			return this;
		}
		
		public O setTriggerParams(Document triggerData) {
			this.triggerData = triggerData;
			return this;
		}
		
		public O addAction(A action) {
			actions.add(action);
			return this;
		}
		
		// Be cautious about using `quest` as its stages may be incomplete
		protected QuestStep build(Quest quest, int stageNo, Map<String, O> namedObjectives) {
			QuestTrigger questTrigger = QuestTrigger.fromDocument(triggerData.append("type", triggerType.toString()), quest);
			List<QuestAction> actions = this.actions.stream().map(a -> a.build(quest)).collect(Collectors.toList());
			return new QuestStep(objectiveName, questTrigger, actions, quest);
		}
		
	}
	
	public class A {
		private QuestActionType actionType;
		private Document actionData;
		
		protected A() { }
		
		public A setActionType(QuestActionType actionType) {
			this.actionType = actionType;
			return this;
		}
		
		public A setActionParams(Document actionData) {
			this.actionData = actionData;
			return this;
		}
		
		protected QuestAction build(Quest quest) {
			return QuestAction.fromDocument(actionData.append("type", actionType.toString()), quest);
		}
		
	}
	
	public B newQuest() { return new B(); }
	public O newObjective() { return new O(); }
	public A newAction() { return new A(); }
	
	public Quest test() {
		Quest compiled = newQuest()
				.setShortName("testshort")
				.setQuestName("test long")
				.setLvMin(10)
				.addObjective(newObjective()
						.setObjectiveName("Talk to Joe")
						.setInternalName("opening")
						.setTriggerType(TriggerType.CLICK_NPC)
						.setTriggerParams(new Document("npcClass", "Joe")))
				.build();
		
		// TODO test that the quest was compiled correctly
		
		return compiled;
	}
}
