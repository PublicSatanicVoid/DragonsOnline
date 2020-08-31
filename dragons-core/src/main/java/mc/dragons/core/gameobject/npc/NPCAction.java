package mc.dragons.core.gameobject.npc;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gui.GUI;
import mc.dragons.core.gui.GUIElement;
import mc.dragons.core.storage.StorageUtil;
import mc.dragons.core.util.PathfindingUtil;

public class NPCAction {
	private static ItemClassLoader itemClassLoader;

	private static NPCClassLoader npcClassLoader;

	private static QuestLoader questLoader;

	private NPCActionType type;

	private NPCClass npcClass;

	private Quest quest;

	private List<String> dialogue;

	private Location to;

	private String shopName;

	private List<ShopItem> shopItems;

	public static class ShopItem {
		private String itemClass;

		private int quantity;

		private double costPer;

		public ShopItem(String itemClass, int quantity, double costPer) {
			this.itemClass = itemClass;
			this.quantity = quantity;
			this.costPer = costPer;
		}

		public ShopItem(Document document) {
			this(document.getString("itemClass"), document.getInteger("quantity").intValue(), document.getDouble("costPer").doubleValue());
		}

		public String getItemClassName() {
			return this.itemClass;
		}

		public int getQuantity() {
			return this.quantity;
		}

		public double getCostPer() {
			return this.costPer;
		}

		public Document toDocument() {
			return (new Document("itemClass", this.itemClass)).append("quantity", Integer.valueOf(this.quantity)).append("costPer", Double.valueOf(this.costPer));
		}
	}

	public enum NPCActionType {
		BEGIN_QUEST, BEGIN_DIALOGUE, TELEPORT_NPC, PATHFIND_NPC, OPEN_SHOP;
	}

	public static NPCAction fromDocument(NPCClass npcClass, Document document) {
		if (npcClassLoader == null) {
			npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
			questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
			itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
		}
		NPCActionType type = NPCActionType.valueOf(document.getString("type"));
		switch (type) {
		case BEGIN_QUEST:
			return beginQuest(npcClass, questLoader.getQuestByName(document.getString("quest")));
		case BEGIN_DIALOGUE:
			return beginDialogue(npcClass, document.getList("dialogue", String.class));
		case TELEPORT_NPC:
			return teleportNPC(npcClass, StorageUtil.docToLoc((Document) document.get("to", Document.class)));
		case PATHFIND_NPC:
			return pathfindNPC(npcClass, StorageUtil.docToLoc((Document) document.get("to", Document.class)));
		case OPEN_SHOP:
			return openShop(npcClass, document.getString("name"), (List<ShopItem>) document.getList("items", Document.class).stream().map(doc -> new ShopItem(doc)).collect(Collectors.toList()));
		}
		return null;
	}

	public static NPCAction beginQuest(NPCClass npcClass, Quest quest) {
		NPCAction action = new NPCAction();
		action.type = NPCActionType.BEGIN_QUEST;
		action.npcClass = npcClass;
		action.quest = quest;
		return action;
	}

	public static NPCAction beginDialogue(NPCClass npcClass, List<String> dialogue) {
		NPCAction action = new NPCAction();
		action.type = NPCActionType.BEGIN_DIALOGUE;
		action.npcClass = npcClass;
		action.dialogue = dialogue;
		return action;
	}

	public static NPCAction teleportNPC(NPCClass npcClass, Location to) {
		NPCAction action = new NPCAction();
		action.type = NPCActionType.TELEPORT_NPC;
		action.npcClass = npcClass;
		action.to = to;
		return action;
	}

	public static NPCAction pathfindNPC(NPCClass npcClass, Location to) {
		NPCAction action = new NPCAction();
		action.type = NPCActionType.PATHFIND_NPC;
		action.npcClass = npcClass;
		action.to = to;
		return action;
	}

	public static NPCAction openShop(NPCClass npcClass, String shopName, List<ShopItem> items) {
		NPCAction action = new NPCAction();
		action.type = NPCActionType.OPEN_SHOP;
		action.npcClass = npcClass;
		action.shopName = shopName;
		action.shopItems = items;
		return action;
	}

	public NPCActionType getType() {
		return this.type;
	}

	public NPCClass getNPCClass() {
		return this.npcClass;
	}

	public Quest getQuest() {
		return this.quest;
	}

	public List<String> getDialogue() {
		return this.dialogue;
	}

	public Location getTo() {
		return this.to;
	}

	public String getShopName() {
		return this.shopName;
	}

	public List<ShopItem> getShopItems() {
		return this.shopItems;
	}

	public Document toDocument() {
		Document result = new Document("type", this.type.toString());
		switch (this.type) {
		case BEGIN_QUEST:
			result.append("quest", this.quest.getName());
			break;
		case BEGIN_DIALOGUE:
			result.append("dialogue", this.dialogue);
			break;
		case TELEPORT_NPC:
		case PATHFIND_NPC:
			result.append("to", StorageUtil.locToDoc(this.to));
			break;
		case OPEN_SHOP:
			result.append("name", this.shopName).append("items", this.shopItems.stream().map(item -> item.toDocument()).collect(Collectors.toList()));
			break;
		}
		return result;
	}

	public void execute(final User user, NPC npc) {
		GUI confirmation;
		int rows;
		GUI shop;
		int slot;
		switch (this.type) {
		case BEGIN_QUEST:
			confirmation = new GUI(1, "Accept quest?");
			confirmation.add(new GUIElement(1, Material.EMERALD_BLOCK, ChatColor.GREEN + "✓ " + ChatColor.DARK_GREEN + ChatColor.BOLD + "Accept",
					ChatColor.GRAY + "You will be unable to interact with\n" + ChatColor.GRAY + "other players, mobs, or NPCs during\n" + ChatColor.GRAY + "quest dialogue.", u -> {
						u.updateQuestProgress(this.quest, this.quest.getSteps().get(0));
						u.closeGUI(true);
					}));
			confirmation.add(new GUIElement(4, Material.PAPER, ChatColor.YELLOW + "Quest Information",
					ChatColor.GRAY + "Quest: " + ChatColor.RESET + this.quest.getQuestName() + "\n" + ChatColor.GRAY + "Lv Min: " + ChatColor.RESET + this.quest.getLevelMin()));
			confirmation.add(new GUIElement(7, Material.REDSTONE_BLOCK, ChatColor.RED + "✘ " + ChatColor.DARK_RED + ChatColor.BOLD + "Reject",
					ChatColor.GRAY + "You can always come back later to\n" + ChatColor.GRAY + "start the quest.", u -> u.closeGUI(true)));
			confirmation.open(user);
			break;
		case BEGIN_DIALOGUE:
			user.setDialogueBatch(null, this.npcClass.getName(), this.dialogue);
			(new BukkitRunnable() {
				public void run() {
					if (!user.nextDialogue())
						cancel();
				}
			}).runTaskTimer((Plugin) Dragons.getInstance(), 0L, 40L);
			break;
		case TELEPORT_NPC:
			npc.getEntity().teleport(this.to);
			break;
		case PATHFIND_NPC:
			PathfindingUtil.walkToLocation(npc.getEntity(), this.to, 0.15D, null);
			break;
		case OPEN_SHOP:
			rows = 2 + (int) Math.ceil(this.shopItems.size() / 7.0D);
			user.debug("opening shop with " + rows + " rows and name " + this.shopName + " with " + this.shopItems.size() + " items");
			shop = new GUI(rows, this.shopName);
			slot = 10;
			for (ShopItem item : this.shopItems) {
				ItemClass itemClass = itemClassLoader.getItemClassByClassName(item.getItemClassName());
				shop.add(itemClass.getAsGuiElement(slot, item.getQuantity(), item.getCostPer(), false, u -> u.buyItem(itemClass, item.getQuantity(), item.getCostPer())));
				slot++;
				if ((slot + 1) % 9 == 0)
					slot += 2;
			}
			shop.open(user);
			break;
		}
	}
}
