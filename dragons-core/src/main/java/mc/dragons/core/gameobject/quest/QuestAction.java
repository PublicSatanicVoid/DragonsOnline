package mc.dragons.core.gameobject.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.PathfindingUtil;
import mc.dragons.core.util.PermissionUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class QuestAction {
	private static NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
	private static ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
	private static NPCLoader npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
	private static ItemLoader itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();

	private Quest quest;
	private QuestActionType action;
	private String npcClassShortName;
	private NPCClass npcClass;
	private boolean phased;
	private String npcReferenceName;
	private List<String> dialogue;
	private Location to;
	private int xpAmount;
	private int stage;
	private ItemClass itemClass;
	private int quantity;
	private boolean notify;
	private PotionEffectType effectType;
	private int duration;
	private int amplifier;
	private int waitTime;
	private Map<String, Integer> choices;

	public static class QuestActionResult {
		private boolean stageModified;

		private boolean shouldPause;

		public QuestActionResult(boolean stageModified, boolean shouldPause) {
			this.stageModified = stageModified;
			this.shouldPause = shouldPause;
		}

		public boolean wasStageModified() {
			return stageModified;
		}

		public boolean shouldPause() {
			return shouldPause;
		}
	}

	public enum QuestActionType {
		TELEPORT_PLAYER,
		SPAWN_NPC, 
		TELEPORT_NPC, 
		PATHFIND_NPC, 
		BEGIN_DIALOGUE,
		GIVE_XP, 
		GOTO_STAGE,
		TAKE_ITEM,
		GIVE_ITEM, 
		ADD_POTION_EFFECT,
		REMOVE_POTION_EFFECT,
		COMPLETION_HEADER,
		WAIT, 
		CHOICES;
	}

	public static QuestAction fromDocument(Document action, Quest quest) {
		QuestAction questAction = new QuestAction();
		questAction.action = QuestActionType.valueOf(action.getString("type"));
		if (questAction.action == QuestActionType.TELEPORT_PLAYER) {
			questAction.to = StorageUtil.docToLoc(action.get("tpTo", Document.class));
		} else if (questAction.action == QuestActionType.SPAWN_NPC) {
			questAction.npcClass = null;
			questAction.npcClassShortName = action.getString("npcClass");
			questAction.npcReferenceName = action.getString("npcReferenceName");
			questAction.phased = action.getBoolean("phased").booleanValue();
			questAction.to = StorageUtil.docToLoc(action.get("location", Document.class));
		} else if (questAction.action == QuestActionType.BEGIN_DIALOGUE) {
			questAction.npcClass = null;
			questAction.npcClassShortName = action.getString("npcClass");
			questAction.dialogue = action.getList("dialogue", String.class);
		} else if (questAction.action == QuestActionType.GIVE_XP) {
			questAction.xpAmount = action.getInteger("xp").intValue();
		} else if (questAction.action == QuestActionType.GOTO_STAGE) {
			questAction.stage = action.getInteger("stage").intValue();
			questAction.notify = action.getBoolean("notify").booleanValue();
		} else if (questAction.action == QuestActionType.TELEPORT_NPC) {
			questAction.npcReferenceName = action.getString("npcReferenceName");
			questAction.to = StorageUtil.docToLoc(action.get("tpTo", Document.class));
		} else if (questAction.action == QuestActionType.PATHFIND_NPC) {
			questAction.npcReferenceName = action.getString("npcReferenceName");
			questAction.to = StorageUtil.docToLoc(action.get("tpTo", Document.class));
			questAction.stage = action.getInteger("stage").intValue();
		} else if (questAction.action == QuestActionType.TAKE_ITEM) {
			questAction.itemClass = itemClassLoader.getItemClassByClassName(action.getString("itemClass"));
			questAction.quantity = action.getInteger("quantity").intValue();
		} else if (questAction.action == QuestActionType.GIVE_ITEM) {
			questAction.itemClass = itemClassLoader.getItemClassByClassName(action.getString("itemClass"));
			questAction.quantity = action.getInteger("quantity").intValue();
		} else if (questAction.action == QuestActionType.ADD_POTION_EFFECT) {
			questAction.effectType = PotionEffectType.getByName(action.getString("effectType"));
			questAction.duration = action.getInteger("duration").intValue();
			questAction.amplifier = action.getInteger("amplifier").intValue();
		} else if (questAction.action == QuestActionType.REMOVE_POTION_EFFECT) {
			questAction.effectType = PotionEffectType.getByName(action.getString("effectType"));
		} else if (questAction.action == QuestActionType.WAIT) {
			questAction.waitTime = action.getInteger("waitTime").intValue();
		} else if (questAction.action == QuestActionType.CHOICES) {
			questAction.choices = new LinkedHashMap<>();
			for (Document choice : action.getList("choices", Document.class)) {
				questAction.choices.put(choice.getString("choice"), choice.getInteger("stage"));
			}
		}
		questAction.quest = quest;
		return questAction;
	}
	
	/* No need to create subclasses for each action type; this works fine */

	public static QuestAction teleportPlayerAction(Quest quest, Location to) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.TELEPORT_PLAYER;
		action.to = to;
		action.quest = quest;
		return action;
	}

	public static QuestAction spawnNPCAction(Quest quest, NPCClass npcClass, Location loc, String referenceName, boolean phased) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.SPAWN_NPC;
		action.npcClass = npcClass;
		action.quest = quest;
		action.npcReferenceName = referenceName;
		action.to = loc;
		action.phased = phased;
		return action;
	}

	public static QuestAction teleportNPCAction(Quest quest, String referenceName, Location to) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.TELEPORT_NPC;
		action.npcReferenceName = referenceName;
		action.to = to;
		action.quest = quest;
		return action;
	}

	public static QuestAction pathfindNPCAction(Quest quest, String referenceName, Location to, int gotoStage) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.PATHFIND_NPC;
		action.npcReferenceName = referenceName;
		action.to = to;
		action.quest = quest;
		action.stage = gotoStage;
		return action;
	}

	public static QuestAction beginDialogueAction(Quest quest, NPCClass npcClass, List<String> dialogue) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.BEGIN_DIALOGUE;
		action.npcClass = npcClass;
		action.dialogue = dialogue;
		action.quest = quest;
		return action;
	}

	public static QuestAction giveXPAction(Quest quest, int xpAmount) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.GIVE_XP;
		action.xpAmount = xpAmount;
		action.quest = quest;
		return action;
	}

	public static QuestAction goToStageAction(Quest quest, int stage, boolean notify) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.GOTO_STAGE;
		action.stage = stage;
		action.quest = quest;
		action.notify = notify;
		return action;
	}

	public static QuestAction takeItemAction(Quest quest, ItemClass itemClass, int quantity) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.TAKE_ITEM;
		action.itemClass = itemClass;
		action.quantity = quantity;
		action.quest = quest;
		return action;
	}

	public static QuestAction giveItemAction(Quest quest, ItemClass itemClass, int quantity) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.GIVE_ITEM;
		action.itemClass = itemClass;
		action.quantity = quantity;
		action.quest = quest;
		return action;
	}

	public static QuestAction addPotionEffectAction(Quest quest, PotionEffectType effectType, int duration, int amplifier) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.ADD_POTION_EFFECT;
		action.effectType = effectType;
		action.duration = duration;
		action.amplifier = amplifier;
		action.quest = quest;
		return action;
	}

	public static QuestAction removePotionEffectAction(Quest quest, PotionEffectType effectType) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.REMOVE_POTION_EFFECT;
		action.effectType = effectType;
		action.quest = quest;
		return action;
	}

	public static QuestAction completionHeaderAction(Quest quest) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.COMPLETION_HEADER;
		action.quest = quest;
		return action;
	}

	public static QuestAction waitAction(Quest quest, int seconds) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.WAIT;
		action.waitTime = seconds;
		action.quest = quest;
		return action;
	}

	public static QuestAction choicesAction(Quest quest, Map<String, Integer> choices) {
		QuestAction action = new QuestAction();
		action.action = QuestActionType.CHOICES;
		action.choices = choices;
		action.quest = quest;
		return action;
	}

	public Document toDocument() {
		List<Document> choices;
		Document document = new Document("type", action.toString());
		switch (action) {
		case TELEPORT_PLAYER:
			document.append("tpTo", StorageUtil.locToDoc(to));
			break;
		case BEGIN_DIALOGUE:
			npcClassDeferredLoad();
			document.append("dialogue", dialogue).append("npcClass", npcClass.getClassName());
			break;
		case SPAWN_NPC:
			npcClassDeferredLoad();
			document.append("npcClass", npcClass.getClassName()).append("phased", Boolean.valueOf(phased)).append("npcReferenceName", npcReferenceName).append("location",
					StorageUtil.locToDoc(to));
			break;
		case GIVE_XP:
			document.append("xp", Integer.valueOf(xpAmount));
			break;
		case GOTO_STAGE:
			document.append("stage", Integer.valueOf(stage)).append("notify", Boolean.valueOf(notify));
			break;
		case TELEPORT_NPC:
		case PATHFIND_NPC:
			document.append("npcReferenceName", npcReferenceName).append("tpTo", StorageUtil.locToDoc(to)).append("stage", Integer.valueOf(stage));
			break;
		case TAKE_ITEM:
		case GIVE_ITEM:
			document.append("itemClass", itemClass.getClassName()).append("quantity", Integer.valueOf(quantity));
			break;
		case ADD_POTION_EFFECT:
			document.append("duration", Integer.valueOf(duration)).append("amplifier", Integer.valueOf(amplifier));
		case REMOVE_POTION_EFFECT:
			document.append("effectType", effectType.getName());
			break;
		case WAIT:
			document.append("waitTime", Integer.valueOf(waitTime));
			break;
		case CHOICES:
			choices = new ArrayList<>();
			for (Entry<String, Integer> choice : this.choices.entrySet()) {
				choices.add(new Document("choice", choice.getKey()).append("stage", choice.getValue()));
			}
			document.append("choices", choices);
			break;
		case COMPLETION_HEADER:
			break;
		default:
			break;
		}
		return document;
	}

	private void npcClassDeferredLoad() {
		if (npcClass == null) {
			npcClass = npcClassLoader.getNPCClassByClassName(npcClassShortName);
		}
	}

	public QuestActionType getActionType() {
		return action;
	}

	public NPCClass getNPCClass() {
		npcClassDeferredLoad();
		return npcClass;
	}

	public boolean isPhased() {
		return phased;
	}

	public String getNPCReferenceName() {
		return npcReferenceName;
	}

	public List<String> getDialogue() {
		return dialogue;
	}

	public Location getLocation() {
		return to;
	}

	public int getXPAmount() {
		return xpAmount;
	}

	public int getGotoStage() {
		return stage;
	}

	public ItemClass getItemClass() {
		return itemClass;
	}

	public int getQuantity() {
		return quantity;
	}

	public PotionEffectType getEffectType() {
		return effectType;
	}

	public int getDuration() {
		return duration;
	}

	public int getAmplifier() {
		return amplifier;
	}

	public int getWaitTime() {
		return waitTime;
	}

	public Map<String, Integer> getChoices() {
		return choices;
	}

	public QuestActionResult execute(final User user) {
		if(quest.isLocked() && !PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, false)) return new QuestActionResult(false, false);
		if (action == QuestActionType.TELEPORT_PLAYER) {
			user.getPlayer().teleport(to);
		} else if (action == QuestActionType.SPAWN_NPC) {
			npcClassDeferredLoad();
			World world = user.getPlayer().getWorld();
			NPC npc = npcLoader.registerNew(world, to, npcClass);
			if (phased) {
				npc.phase(user.getPlayer());
			}
			quest.registerNPCReference(user, npc, npcReferenceName);
		} else if (action == QuestActionType.BEGIN_DIALOGUE) {
			npcClassDeferredLoad();
			user.setDialogueBatch(quest, npcClass.getName(), dialogue);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!user.nextDialogue()) {
						user.updateQuests(null);
						cancel();
					}
				}
			}.runTaskTimer(Dragons.getInstance(), 0L, 40L);
			return new QuestActionResult(false, true);
		} else if (action == QuestActionType.GIVE_XP) {
			user.getPlayer().sendMessage(ChatColor.GRAY + "+ " + ChatColor.LIGHT_PURPLE + xpAmount + " XP" + ChatColor.GRAY + " from quest " + quest.getQuestName());
			user.addXP(xpAmount);
		} else if (action == QuestActionType.GOTO_STAGE) {
			user.debug("    - going to stage " + stage + " (" + quest.getSteps().get(stage).getStepName() + ")");
			user.updateQuestProgress(quest, quest.getSteps().get(stage), notify);
			return new QuestActionResult(true, false);
		} else if (action == QuestActionType.TELEPORT_NPC) {
			npcClassDeferredLoad();
			NPC npc = quest.getNPCByReference(user, npcReferenceName);
			npc.getEntity().teleport(to);
		} else if (action == QuestActionType.PATHFIND_NPC) {
			npcClassDeferredLoad();
			NPC npc = quest.getNPCByReference(user, npcReferenceName);
			double speed = 0.25D;
			if (npc.getEntity() instanceof Attributable) {
				speed = ((Attributable) npc.getEntity()).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
			}
			boolean hasAI = Dragons.getInstance().getBridge().hasAI(npc.getEntity());
			if(hasAI) {
				Dragons.getInstance().getBridge().setEntityAI(npc.getEntity(), false);
			}
			PathfindingUtil.walkToLocation(npc.getEntity(), to, speed, e -> {
				if (stage != -1) {
					user.updateQuestProgress(quest, quest.getSteps().get(stage), false);
				}
				if(hasAI) {
					Dragons.getInstance().getBridge().setEntityAI(e, true);
				}
			});
			return new QuestActionResult(stage != -1, false);
		} else if (action == QuestActionType.TAKE_ITEM) {
			int remaining = quantity;
			for(ItemStack itemStack : user.getPlayer().getInventory().getContents()) {
				Item item = ItemLoader.fromBukkit(itemStack);
				if (item != null && item.getClassName().equals(itemClass.getClassName())) {							
					int removeAmount = Math.min(remaining, item.getQuantity());
					user.takeItem(item, removeAmount, true, true, false);
					remaining -= item.getQuantity();
					if (remaining <= 0) {
						return new QuestActionResult(false, false);
					}
				}
			}
		} else if (action == QuestActionType.GIVE_ITEM) {
			Item item = itemLoader.registerNew(itemClass);
			item.setQuantity(quantity);
			user.giveItem(item);
		} else if (action == QuestActionType.ADD_POTION_EFFECT) {
			user.getPlayer().addPotionEffect(new PotionEffect(effectType, duration, amplifier));
		} else if (action == QuestActionType.REMOVE_POTION_EFFECT) {
			user.getPlayer().removePotionEffect(effectType);
		} else if (action == QuestActionType.COMPLETION_HEADER) {
			new BukkitRunnable() {
				private float pitch = 1.0F;
				@Override
				public void run() {
					user.getPlayer().playSound(user.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, pitch);
					pitch = (float) (pitch + 0.05D);
					if (pitch > 2.0F) {
						cancel();
					}
				}
			}.runTaskTimer(Dragons.getInstance(), 0L, 1L);
			user.getPlayer().sendMessage(" ");
			user.getPlayer().sendMessage(ChatColor.GREEN + "" + ChatColor.MAGIC + "ff" + ChatColor.DARK_GREEN + "  Quest Complete: " + quest.getQuestName() + ChatColor.GREEN + "  "
					+ ChatColor.MAGIC + "ff");
			user.getPlayer().sendMessage(ChatColor.GRAY + "Rewards:");
		} else if (action == QuestActionType.WAIT) {
			user.debug("Waiting " + waitTime + "s");
			user.setQuestPaused(quest, true);
			new BukkitRunnable() {
				@Override
				public void run() {
					user.debug("Resuming quest actions");
					user.setQuestPaused(quest, false);
					user.updateQuests(null);
				}
			}.runTaskLater(Dragons.getInstance(), 20L * waitTime);
			return new QuestActionResult(false, true);
		} else if (action == QuestActionType.CHOICES) {
			user.getPlayer().sendMessage(" ");
			for (Entry<String, Integer> choice : choices.entrySet()) {
				TextComponent choiceMessage = new TextComponent(ChatColor.YELLOW + " • " + ChatColor.GRAY + choice.getKey());
				choiceMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					new Text(ChatColor.YELLOW + "Click to respond\n"), 
					new Text(ChatColor.GRAY + "Quest: " + ChatColor.RESET + quest.getQuestName() + "\n"),
					new Text(ChatColor.GRAY + "Response: " + ChatColor.RESET + choice.getKey())));
				choiceMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/questchoice " + quest.getName() + " " + choice.getKey()));
				user.getPlayer().spigot().sendMessage(choiceMessage);
			}
			user.getPlayer().sendMessage(" ");
			user.setQuestPaused(quest, true);
			return new QuestActionResult(false, true);
		}
		return new QuestActionResult(false, false);
	}
}
