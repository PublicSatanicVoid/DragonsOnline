package mc.dragons.core.gameobject.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCClassLoader;
import mc.dragons.core.gameobject.npc.NPCLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;
import mc.dragons.core.gameobject.user.User;

/**
 * A trigger condition for a quest stage's actions to be executed.
 * 
 * @author Adam
 *
 */
public class QuestTrigger {
	private static RegionLoader regionLoader = GameObjectType.REGION.getLoader();
	private static NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.getLoader();
	private static ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.getLoader();
	
	private TriggerType type;
	private String npcClassShortName;
	private NPCClass npcClass;
	private ItemClass itemClass;
	private int quantity;
	private Region region;
	private Map<QuestTrigger, QuestAction> branchPoints;
	private Map<User, Integer> killQuantity;
	private double minDistance;

	public enum TriggerType {
		ENTER_REGION,
		WALK_REGION,
		EXIT_REGION, 
		CLICK_NPC, 
		KILL_NPC, 
		INSTANT, 
		HAS_ITEM, 
		NEVER, 
		BRANCH_CONDITIONAL;
	}

	public static QuestTrigger fromDocument(Document trigger, Quest quest) {
		QuestTrigger questTrigger = new QuestTrigger();
		questTrigger.type = TriggerType.valueOf(trigger.getString("type"));
		if (questTrigger.type == TriggerType.ENTER_REGION || questTrigger.type == TriggerType.EXIT_REGION) {
			questTrigger.region = regionLoader.getRegionByName(trigger.getString("region"));
		} else if (questTrigger.type == TriggerType.WALK_REGION) {
			questTrigger.region = regionLoader.getRegionByName(trigger.getString("region"));
			questTrigger.minDistance = trigger.getDouble("minDistance");
		} else if (questTrigger.type == TriggerType.CLICK_NPC) {
			questTrigger.npcClassShortName = trigger.getString("npcClass");
		} else if (questTrigger.type == TriggerType.KILL_NPC) {
			questTrigger.npcClassShortName = trigger.getString("npcClass");
			questTrigger.quantity = trigger.getInteger("quantity").intValue();
		} else if (questTrigger.type == TriggerType.BRANCH_CONDITIONAL) {
			questTrigger.branchPoints = new LinkedHashMap<>();
			for (Document conditional : trigger.getList("branchPoints", Document.class)) {
				questTrigger.branchPoints.put(fromDocument((Document) conditional.get("trigger"), quest), QuestAction.fromDocument((Document) conditional.get("action"), quest));
			}
		} else if (questTrigger.type == TriggerType.HAS_ITEM) {
			questTrigger.itemClass = itemClassLoader.getItemClassByClassName(trigger.getString("itemClass"));
			questTrigger.quantity = trigger.getInteger("quantity").intValue();
		}
		return questTrigger;
	}

	private QuestTrigger() {
		killQuantity = new HashMap<>();
	}
	
	/* No need to create subclasses for each trigger type; this works fine */

	public static QuestTrigger onEnterRegion(Region region) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.ENTER_REGION;
		trigger.region = region;
		return trigger;
	}
	
	public static QuestTrigger onWalkRegion(Region region, double minDistance) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.WALK_REGION;
		trigger.region = region;
		trigger.minDistance = minDistance;
		return trigger;
	}

	public static QuestTrigger onExitRegion(Region region) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.EXIT_REGION;
		trigger.region = region;
		return trigger;
	}

	public static QuestTrigger onClickNPC(NPCClass npcClass) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.CLICK_NPC;
		trigger.npcClass = npcClass;
		return trigger;
	}

	public static QuestTrigger onKillNPC(NPCClass npcClass, int quantity) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.KILL_NPC;
		trigger.npcClass = npcClass;
		trigger.quantity = quantity;
		return trigger;
	}

	public static QuestTrigger instant() {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.INSTANT;
		return trigger;
	}

	public static QuestTrigger hasItem(ItemClass itemClass, int quantity) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.HAS_ITEM;
		trigger.itemClass = itemClass;
		trigger.quantity = quantity;
		return trigger;
	}

	public static QuestTrigger never() {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.NEVER;
		return trigger;
	}

	public static QuestTrigger branchConditional(Map<QuestTrigger, QuestAction> branchPoints) {
		QuestTrigger trigger = new QuestTrigger();
		trigger.type = TriggerType.BRANCH_CONDITIONAL;
		trigger.branchPoints = branchPoints;
		return trigger;
	}

	public TriggerType getTriggerType() {
		return type;
	}

	public NPCClass getNPCClass() {
		npcClassDeferredLoad();
		return npcClass;
	}

	public Region getRegion() {
		return region;
	}

	public ItemClass getItemClass() {
		return itemClass;
	}

	public int getQuantity() {
		return quantity;
	}

	public Map<QuestTrigger, QuestAction> getBranchPoints() {
		return branchPoints;
	}

	public double getMinDistance() {
		return minDistance;
	}
	
	private void npcClassDeferredLoad() {
		if (npcClass == null) {
			npcClass = npcClassLoader.getNPCClassByClassName(npcClassShortName);
		}
	}

	public Document toDocument() {
		List<Document> conditions;
		Document document = new Document("type", type.toString());
		switch (type) {
		case ENTER_REGION:
		case EXIT_REGION:
			document.append("region", region.getName());
			break;
		case WALK_REGION:
			document.append("region", region.getName());
			document.append("minDistance", minDistance);
			break;
		case CLICK_NPC:
			npcClassDeferredLoad();
			document.append("npcClass", npcClass.getClassName());
			break;
		case KILL_NPC:
			npcClassDeferredLoad();
			document.append("npcClass", npcClass.getClassName());
			document.append("quantity", quantity);
			break;
		case HAS_ITEM:
			document.append("itemClass", itemClass.getClassName()).append("quantity", Integer.valueOf(quantity));
			break;
		case BRANCH_CONDITIONAL:
			conditions = new ArrayList<>();
			for (Entry<QuestTrigger, QuestAction> entry : branchPoints.entrySet()) {
				conditions.add(new Document("trigger", entry.getKey().toDocument()).append("action", entry.getValue().toDocument()));
			}
			document.append("branchPoints", conditions);
			break;

		case INSTANT:
			break;
		case NEVER:
			break;
		default:
			break;
		}
		return document;
	}

	public boolean test(User user, Event event) {
		if (type == TriggerType.INSTANT) {
			return true;
		}
		else if (type == TriggerType.NEVER) {
			return false;
		}
		else if (type == TriggerType.HAS_ITEM) {
			user.debug(" [ - Testing if has item " + itemClass.getClassName());
			int has = 0;
			for(ItemStack itemStack : user.getPlayer().getInventory().getContents()) {
				Item item = ItemLoader.fromBukkit(itemStack);
				if (item != null && item.getClassName().equals(itemClass.getClassName())) {
					has += itemStack.getAmount();
				}	
			}
			user.debug("    [ - has " + has + " vs. needs " + quantity);
			return has >= quantity;
		}
		else if (type == TriggerType.ENTER_REGION) {
			user.updateState(false, false);
			if (user.getRegions().contains(region)) {
				return true;
			}
		}
		else if (type == TriggerType.WALK_REGION) {
			user.updateState(false, false);
			user.debug("    [ - continuous walk distance is " + user.getContinuousWalkDistance(region));
			if (user.getRegions().contains(region) && user.getContinuousWalkDistance(region) >= minDistance) {
				return true;
			}
		}
		else if (type == TriggerType.EXIT_REGION) {
			user.updateState(false, false);
			if (!user.getRegions().contains(region)) {
				return true;
			}
		}
		else if (type == TriggerType.CLICK_NPC) {
			npcClassDeferredLoad();
			if (event == null) {
				return false;
			}
			if (event instanceof PlayerInteractEntityEvent) {
				user.debug("    [ - it's an interact entity event");
				PlayerInteractEntityEvent interactEvent = (PlayerInteractEntityEvent) event;
				NPC npc = NPCLoader.fromBukkit(interactEvent.getRightClicked());
				if (npc == null) {
					return false;
				}
				user.debug("    [ - clicked class: " + npc.getNPCClass().getClassName() + "; want: " + npcClass.getClassName());
				if (npc.getNPCClass().equals(npcClass)) {
					return true;
				}
			}
		}
		else if (type == TriggerType.KILL_NPC) {
			npcClassDeferredLoad();
			if (event == null) {
				return false;
			}
			if (event instanceof EntityDeathEvent) {
				EntityDeathEvent deathEvent = (EntityDeathEvent) event;
				NPC npc = NPCLoader.fromBukkit(deathEvent.getEntity());
				if (npc == null) {
					return false;
				}
				if (npc.getNPCClass().equals(npcClass)) {
					killQuantity.put(user, killQuantity.getOrDefault(user, 0) + 1);
					if (killQuantity.getOrDefault(user, 0) >= quantity) {
						killQuantity.remove(user);
						return true;
					}
				}
			}
		}
		else if (type == TriggerType.BRANCH_CONDITIONAL) {
			for (Entry<QuestTrigger, QuestAction> conditional : branchPoints.entrySet()) {
				if (conditional.getKey().test(user, event)) {
					conditional.getValue().execute(user);
					return true;
				}
			}
		}
		return false;
	}
}
