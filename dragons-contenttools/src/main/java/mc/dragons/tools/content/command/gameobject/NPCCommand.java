package mc.dragons.tools.content.command.gameobject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.Addon;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.gameobject.npc.NPCAction;
import mc.dragons.core.gameobject.npc.NPCAction.NPCActionType;
import mc.dragons.core.gameobject.npc.NPCAction.ShopItem;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCCondition;
import mc.dragons.core.gameobject.npc.NPCCondition.NPCConditionType;
import mc.dragons.core.gameobject.npc.NPCConditionalActions;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

public class NPCCommand extends DragonsCommandExecutor {
	private GameObjectRegistry gameObjectRegistry = instance.getGameObjectRegistry();;
	private AddonRegistry addonRegistry = instance.getAddonRegistry();;

	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/npc create <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>" + ChatColor.GRAY + " create a new NPC class");
		sender.sendMessage(ChatColor.GRAY + " * Valid NPCTypes are: " + StringUtil.parseList(NPCType.values()));
		sender.sendMessage(ChatColor.YELLOW + "/npc list [startingWith]" + ChatColor.GRAY + " list all NPC classes, optionally starting with the specified text (e.g. F2-)");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName>" + ChatColor.GRAY + " view information about NPC class");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> type <EntityType>" + ChatColor.GRAY + " change type of NPC class");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> name <DisplayName>" + ChatColor.GRAY + " set NPC class display name");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> health <MaxHealth>" + ChatColor.GRAY + " set NPC class max health");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> level <Level>" + ChatColor.GRAY + " set NPC level");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> npctype <NPCType>" + ChatColor.GRAY + " set NPC type");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> holding <MaterialType|NONE>" + ChatColor.GRAY + " set item type the NPC is holding");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> ai <HasAI>" + ChatColor.GRAY + " set whether the NPC has AI");
		sender.sendMessage(ChatColor.DARK_GRAY + " * Pathfinding behavior will still be enabled when triggered by the RPG");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> immortal <IsImmortal>" + ChatColor.GRAY + " set whether the NPC is immortal");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> loot [<RegionName> <ItemClassName> <Chance%|DEL>]" + ChatColor.GRAY + " manage NPC class loot table");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> behavior|b [<CLICK|HIT> <add|remove <#>>]" + ChatColor.GRAY + " add/remove/view NPC behaviors");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> behavior|b <CLICK|HIT> <#> condition <add [!]<ConditionType> <ConditionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove conditions on an NPC behavior");
		sender.sendMessage(ChatColor.DARK_GRAY + " * Adding a ! before the ConditionType will negate the condition.");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> behavior|b <CLICK|HIT> <#> action <add <ActionType> <ActionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove actions on an NPC behavior");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> behavior|b <CLICK|HIT> <#> action shop add <Action#> <ItemClass> <Quantity> <CostPer>" + ChatColor.GRAY + " manage shop behavior");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> addon [<add|remove> <AddonName>]" + ChatColor.GRAY + " manage addons");
		sender.sendMessage(ChatColor.YELLOW + "/npc <ClassName> attribute [<Attribute> <Value|DEL>]" + ChatColor.GRAY + " manage attributes");
		sender.sendMessage(ChatColor.YELLOW + "/npc delete <ClassName>" + ChatColor.GRAY + " delete NPC class");
		sender.sendMessage(ChatColor.YELLOW + "/npc spawn <ClassName>  [Quantity] [-phase <Player>]" + ChatColor.GRAY + " spawn a new NPC of the given class");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Class names must not contain spaces. Prefix with primary floor F#-, e.g. F2- for floor 2");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void createClass(CommandSender sender, String[] args) {
		if(args.length < 6) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc create <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>");
			return;
		}
		String npcClassName = args[1];
		EntityType type = StringUtil.parseEntityType(sender, args[2]);
		Double maxHealth = parseDoubleType(sender, args[3]);
		Integer level = parseIntType(sender, args[4]);
		NPCType npcType = StringUtil.parseEnum(sender, NPCType.class, args[5]);
		if(type == null || maxHealth == null || level == null || npcType == null) return;
		
		NPCClass npcClass = npcClassLoader.registerNew(npcClassName, "Unnamed Entity", type, maxHealth, level, npcType);
		if(npcClass == null) {
			sender.sendMessage(ChatColor.RED + "An error occurred! Does an NPC class by this name already exist?");
			return;
		}
		MetadataConstants.addBlankMetadata(npcClass, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Successfully created NPC class " + npcClassName);
	}
	
	private void listClasses(CommandSender sender, String[] args) {
		String startingWith = "";
		if(args.length > 1) {
			startingWith = args[1];
		}
		sender.sendMessage(ChatColor.GREEN + "Listing all NPC classes" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
		for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects(GameObjectType.NPC_CLASS)) {
			NPCClass npcClass = (NPCClass) gameObject;
			if(!npcClass.getClassName().startsWith(startingWith)) continue;
			sender.sendMessage(ChatColor.GRAY + "- " + npcClass.getClassName() + " [Lv " + npcClass.getLevel() + "]");
		}
	}
	
	private void deleteClass(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE)) return;
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a class name to delete! /npc delete <ClassName>");
			return;
		}
		NPCClass npcClass = lookupNPCClass(sender, args[1]);
		if(npcClass == null) return;
		gameObjectRegistry.removeFromDatabase(npcClass);
		sender.sendMessage(ChatColor.GREEN + "Successfully deleted NPC class.");
	}
	
	private void spawnNPCOfClass(CommandSender sender, String[] args) {
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify an NPC class to spawn! /npc spawn <ClassName> [Quantity] [-phase <Player>]");
			return;
		}
		Player player = player(sender);
		List<NPC> spawned = new ArrayList<>();
		int quantity = 1;
		if(args.length > 2) {
			quantity = Integer.valueOf(args[2]);
		}
		int remaining = quantity;
		while(remaining > 0) {
			spawned.add(npcLoader.registerNew(player.getWorld(), player.getLocation(), args[1]));
			remaining--;
		}
		sender.sendMessage(ChatColor.GREEN + "Spawned an NPC of class " + args[1] + " at your location (x" + quantity + ")");
		if(args.length > 4 && args[3].equalsIgnoreCase("-phase")) {
			Player phaseFor = Bukkit.getPlayerExact(args[4]);
			spawned.stream().forEach(npc -> npc.phase(phaseFor));
			sender.sendMessage(ChatColor.GREEN + "Phased NPC successfully.");
		}
	}
	
	private void displayClass(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		sender.sendMessage(ChatColor.GREEN + "=== NPC Class: " + npcClass.getClassName() + " ===");
		sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + npcClass.getIdentifier().toString());
		sender.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GREEN + npcClass.getName());
		sender.sendMessage(ChatColor.GRAY + "Entity type: " + ChatColor.GREEN + npcClass.getEntityType().toString());
		sender.sendMessage(ChatColor.GRAY + "Max health: " + ChatColor.GREEN + npcClass.getMaxHealth());
		sender.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.GREEN + npcClass.getLevel());
		sender.sendMessage(ChatColor.GRAY + "NPC type: " + ChatColor.GREEN + npcClass.getNPCType().toString());
		sender.sendMessage(ChatColor.GRAY + "AI: " + ChatColor.GREEN + npcClass.hasAI());
		sender.sendMessage(ChatColor.GRAY + "Immortal: " + ChatColor.GREEN + npcClass.isImmortal());
		Material heldItemType = npcClass.getHeldItemType();
		String heldItemTypeName = "Nothing";
		if(heldItemType != null) {
			heldItemTypeName = heldItemType.toString();
		}
		sender.sendMessage(ChatColor.GRAY + "Holding: " + ChatColor.GREEN + heldItemTypeName);
		MetadataConstants.displayMetadata(sender, npcClass);
		sender.sendMessage(ChatColor.GRAY + "/npc " + npcClass.getClassName() + " loot" + ChatColor.YELLOW + " to view loot table");
		sender.sendMessage(ChatColor.GRAY + "/npc " + npcClass.getClassName() + " behavior" + ChatColor.YELLOW + " to view behaviors");
		sender.sendMessage(ChatColor.GRAY + "/npc " + npcClass.getClassName() + " addon" + ChatColor.YELLOW + " to view addons");
		sender.sendMessage(ChatColor.GRAY + "/npc " + npcClass.getClassName() + " attribute" + ChatColor.YELLOW + " to view attributes");
	}
	
	private void manageAttributes(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.GREEN + "Listing custom base attributes for NPC class " + npcClass.getClassName() + ":");
			for(Entry<Attribute, Double> attribute : npcClass.getCustomAttributes().entrySet()) {
				sender.sendMessage(ChatColor.GRAY + "- " + attribute.getKey() + ": " + attribute.getValue());
			}
			return;
		}
		else if(args.length < 4) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> attribute <Attribute> <Value|DEL>");
			return;
		}
		Attribute att = StringUtil.parseEnum(sender, Attribute.class, args[2]);
		if(args[3].equalsIgnoreCase("DEL")) {
			npcClass.removeCustomAttribute(att);
		}
		else {
			Double value = parseDoubleType(sender, args[3]);
			if(value == null) return;
			npcClass.setCustomAttribute(att, value);
		}
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Updated attributes successfully.");
	}
	
	private void manageAddons(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		User user = user(sender);
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.GREEN + "Listing addons for NPC class " + npcClass.getClassName() + ":");
			for(NPCAddon addon : npcClass.getAddons()) {
				sender.sendMessage(ChatColor.GRAY + "- " + addon.getName());
			}
			return;
		}
		else if(args.length == 5) {
			sender.sendMessage(ChatColor.RED + "Specify an addon name! For a list of addons, do /addon -l");
			return;
		}
		Addon addon = addonRegistry.getAddonByName(args[5]);
		if(addon == null) {
			sender.sendMessage(ChatColor.RED + "Invalid addon name! For a list of addons, do /addon -l");
		}
		else if(!(addon instanceof NPCAddon)) {
			sender.sendMessage(ChatColor.RED + "Invalid addon type! Only NPC Addons can be applied to NPCs.");
		}
		else if(args[4].equalsIgnoreCase("add")) {
			npcClass.addAddon((NPCAddon) addon);
			sender.sendMessage(ChatColor.GREEN + "Added addon " + addon.getName() + " to NPC class " + npcClass.getClassName() + ".");
			MetadataConstants.incrementRevisionCount(npcClass, user);
		}
		else if(args[4].equalsIgnoreCase("remove")) {
			npcClass.removeAddon((NPCAddon) addon);
			sender.sendMessage(ChatColor.GREEN + "Removed addon " + addon.getName() + " from NPC class " + npcClass.getClassName() + ".");
			MetadataConstants.incrementRevisionCount(npcClass, user);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /npc <ClassName> addon [<add|remove> <AddonName>]");
		}
	}
	
	private void manageLoot(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		User user = user(sender);
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.GREEN + "Loot Table:");
			for(Entry<String, Map<String, Double>> regionLoot : npcClass.getLootTable().asMap().entrySet()) {
				sender.sendMessage(ChatColor.GRAY + "- Region: " + regionLoot.getKey());
				for(Entry<String, Double> itemLoot : regionLoot.getValue().entrySet()) {
					sender.sendMessage(ChatColor.GRAY + "   - " + itemLoot.getKey() + ": " + itemLoot.getValue() + "%");
				}
			}
		}
		else if(args.length < 5) {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /npc <ClassName> loot [<RegionShortName> <ItemClassShortName> <Chance%|DEL>]");
		}
		else if(args[4].equalsIgnoreCase("del")) {
			npcClass.deleteFromLootTable(args[2], args[3]);
			sender.sendMessage(ChatColor.GREEN + "Removed from entity loot table successfully.");
			MetadataConstants.incrementRevisionCount(npcClass, user);
		}
		else {
			Double chance = parseDoubleType(sender, args[4]);
			if(chance == null) return;
			npcClass.updateLootTable(args[2], args[3], chance);
			sender.sendMessage(ChatColor.GREEN + "Updated entity loot table successfully.");
			MetadataConstants.incrementRevisionCount(npcClass, user);
		}
	}
	
	private void displayBehavior(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		sender.sendMessage(ChatColor.GREEN + "NPC Behaviors:");
		for(NPCTrigger trigger : NPCTrigger.values()) {
			sender.sendMessage(ChatColor.GRAY + "- Trigger: " + trigger);
			int i = 0;
			for(Entry<List<NPCCondition>, List<NPCAction>> entry : npcClass.getConditionalActions(trigger).getConditionals().entrySet()) {
				sender.sendMessage(ChatColor.GRAY + "  - Behavior " + ChatColor.DARK_GREEN + "#" + i + ChatColor.GRAY + ":");
				sender.sendMessage(ChatColor.GRAY + "    - Conditions:");
				int j = 0;
				for(NPCCondition condition : entry.getKey()) {
					sender.sendMessage(ChatColor.DARK_GREEN + "      #" + j + ": " + ChatColor.GRAY + displayNPCCondition(condition));
					j++;
				}
				sender.sendMessage(ChatColor.GRAY + "    - Actions:");
				j = 0;
				for(NPCAction action : entry.getValue()) {
					sender.sendMessage(ChatColor.DARK_GREEN + "      #" + j + ": " + ChatColor.GRAY + displayNPCAction(action));
					j++;
				}
				i++;
			}
		}
	}
	
	private void manageBehavior(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		if(args.length == 2) {
			displayBehavior(sender, args);
			return;
		}
		NPCTrigger trigger = NPCTrigger.valueOf(args[2]);
		NPCConditionalActions behaviorsLocal = npcClass.getConditionalActions(trigger);
		NPCConditionalActions parsedBehaviors = npcClass.getConditionalActions(trigger);
		Document conditionals = npcClass.getStorageAccess().getDocument().get("conditionals", Document.class);
		List<Document> behaviors = conditionals.getList(trigger.toString(), Document.class);
		if(args[3].equalsIgnoreCase("add")) {
			addBehavior(sender, args, behaviors, conditionals, trigger, npcClass, parsedBehaviors);
		}
		else if(args[3].equalsIgnoreCase("remove")) {
			removeBehavior(sender, args, behaviors, conditionals, trigger, npcClass, parsedBehaviors);
		}
		else {
			manageBehavior(sender, args, behaviors, conditionals, trigger, npcClass, parsedBehaviors, behaviorsLocal);
		}
	}
	
	private void addBehavior(CommandSender sender, String[] args, List<Document> behaviors, Document conditionals, NPCTrigger trigger, NPCClass npcClass, NPCConditionalActions parsedBehaviors) {
		behaviors.add(new Document("conditions", new ArrayList<Document>()).append("actions", new ArrayList<Document>()));
		npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
		parsedBehaviors.addLocalEntry();
		sender.sendMessage(ChatColor.GREEN + "Successfully added a new conditional with trigger " + trigger);
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void removeBehavior(CommandSender sender, String[] args, List<Document> behaviors, Document conditionals, NPCTrigger trigger, NPCClass npcClass, NPCConditionalActions parsedBehaviors) {
		if(args.length == 6) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> remove <#>");
			return;
		}
		Integer behaviorNo = parseIntType(sender, args[4]);
		if(behaviorNo == null) return;
		
		behaviors.remove((int) behaviorNo);
		npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
		parsedBehaviors.removeLocalEntry(behaviorNo);
		sender.sendMessage(ChatColor.GREEN + "Successfully removed conditional #" + behaviorNo + " with trigger " + trigger);
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}

	private void manageBehavior(CommandSender sender, String[] args, List<Document> behaviors, Document conditionals, NPCTrigger trigger, NPCClass npcClass, NPCConditionalActions parsedBehaviors, NPCConditionalActions behaviorsLocal) {
		User user = user(sender);
		Player player = player(sender);
		
		if(args.length < 6) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /npc");
			return;
		}
		
		Integer behaviorNo = parseIntType(sender, args[3]);
		if(behaviorNo == null) return;
		
		Document behavior = behaviors.get(behaviorNo);
		if(args[4].equalsIgnoreCase("condition") || args[4].equalsIgnoreCase("c")) {
			List<Document> conditions = behavior.getList("conditions", Document.class);
			if(args[5].equalsIgnoreCase("add")) {
				if(args.length < 8) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> condition add <ConditionType> <ConditionParams...>");
					return;
				}
				boolean inverted = args[6].startsWith("!");
				if(inverted) args[6] = args[6].replace("!", "");
				NPCConditionType condType = StringUtil.parseEnum(sender, NPCConditionType.class, args[6]);
				if(condType == null) return;
				
				NPCCondition cond = null;
				switch(condType) {
				case HAS_COMPLETED_QUEST:
					cond = NPCCondition.hasCompletedQuest(questLoader.getQuestByName(args[7]), inverted);
					break;
				case HAS_QUEST_STAGE:
					Integer stage = parseIntType(sender, args[8]);
					if(stage == null) return;
					cond = NPCCondition.hasQuestStage(questLoader.getQuestByName(args[7]), stage, inverted);
					break;
				case HAS_GOLD:
					Double qty = parseDoubleType(sender, args[7]);
					if(qty == null) return;
					cond = NPCCondition.hasGold(qty, inverted);
					break;
				case HAS_LEVEL:
					Integer lv = parseIntType(sender, args[7]);
					if(lv == null) return;
					cond = NPCCondition.hasLevel(lv, inverted);
					break;
				default:
					break;
				}
				conditions.add(cond.toDocument());
				behaviorsLocal.getConditional(behaviorNo).getKey().add(cond);
			}
			else if(args[5].equalsIgnoreCase("remove")) {
				if(args.length < 7) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> condition remove <#>");
					return;
				}
				Integer index = parseIntType(sender, args[6]);
				if(index == null) return;
				conditions.remove((int) index);
				behaviorsLocal.getConditional(behaviorNo).getKey().remove((int) index);
			}
			behavior.append("conditions", conditions);
		}
		else if(args[4].equalsIgnoreCase("action") || args[4].equalsIgnoreCase("a")) {
			List<Document> actions = behavior.getList("actions", Document.class);
			if(args[5].equalsIgnoreCase("shop")) {
				if(args[6].equalsIgnoreCase("add")) {
					if(args.length < 11) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> action shop add <#> <ItemClass> <Quantity> <CostPer>");
						return;
					}
					Integer actionNo = parseIntType(sender, args[7]);
					Integer qty = parseIntType(sender, args[9]);
					Double cost = parseDoubleType(sender, args[10]);
					if(actionNo == null || qty == null || cost == null) return;
					
					NPCAction action = behaviorsLocal.getConditional(behaviorNo).getValue().get(actionNo);
					action.getShopItems().add(new ShopItem(args[8],  qty, cost));
					actions.get(actionNo).putAll(action.toDocument());
					behavior.append("actions", actions);
					npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
					sender.sendMessage(ChatColor.GREEN + "Added item to shop successfully.");
					MetadataConstants.incrementRevisionCount(npcClass, user);
					return;
				}
				else if(args[6].equalsIgnoreCase("remove")) {
					if(args.length < 10) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action shop remove <action#> <item#>");
						return;
					}
					Integer actionNo = parseIntType(sender, args[7]);
					Integer slotNo = parseIntType(sender, args[8]);
					if(actionNo == null || slotNo == null) return;
					NPCAction action = behaviorsLocal.getConditional(behaviorNo).getValue().get(actionNo);
					action.getShopItems().remove((int) slotNo);
					actions.get(actionNo).clear();
					actions.get(actionNo).putAll(action.toDocument());
					behavior.append("actions", actions);
					npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
					sender.sendMessage(ChatColor.GREEN + "Removed item from shop successfully.");
					MetadataConstants.incrementRevisionCount(npcClass, user);
					return;
				}
			}
			if(args[5].equalsIgnoreCase("add")) {
				NPCActionType actionType = StringUtil.parseEnum(sender, NPCActionType.class, args[6]);
				if(actionType == null) return;
				NPCAction action = null;
				switch(actionType) {
				case BEGIN_DIALOGUE:
					if(args.length < 8) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> action add BEGIN_DIALOGUE <DialogueLine1|DialogueLine2|...>");
						return;
					}
					action = NPCAction.beginDialogue(npcClass, Arrays.asList(StringUtil.concatArgs(args, 9)
							.replaceAll(Pattern.quote("%PH%"), user.getLocalData().get("placeholder", "(Empty placeholder)"))
							.split(Pattern.quote("|"))));
					break;
				case BEGIN_QUEST:
					if(args.length < 8) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> action add BEGIN_QUEST <QuestShortName>");
						return;
					}
					Quest quest = lookupQuest(sender, args[7]);
					if(quest == null) return;
					action = NPCAction.beginQuest(npcClass, questLoader.getQuestByName(args[7]));
					break;
				case OPEN_SHOP:
					if(args.length < 8) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> action add OPEN_SHOP <Shop Title>");
						return;
					}
					action = NPCAction.openShop(npcClass, StringUtil.concatArgs(args, 7), new ArrayList<>());
					break;
				case TELEPORT_NPC:
					if(!requirePlayer(sender)) return;
					action = NPCAction.teleportNPC(npcClass, player.getLocation());
					break;
				case PATHFIND_NPC:
					if(!requirePlayer(sender)) return;
					action = NPCAction.pathfindNPC(npcClass, player.getLocation());
					break;
				default:
					break;
				}
				actions.add(action.toDocument());
				behaviorsLocal.getConditional(behaviorNo).getValue().add(action);
			}
			else if(args[5].equalsIgnoreCase("remove")) {
				if(args.length < 7) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> behavior <CLICK|HIT> <#> action remove <#>");
					return;
				}
				Integer index = parseIntType(sender, args[6]);
				if(index == null) return;
				actions.remove((int) index);
				behaviorsLocal.getConditional(behaviorNo).getValue().remove((int) index);
			}
			behavior.append("actions", actions);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid behavioral arguments! For usage info, do /npc");
			return;
		}
		npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
		sender.sendMessage(ChatColor.GREEN + "Updated behavior successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setType(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		EntityType type = StringUtil.parseEnum(sender, EntityType.class, args[2]);
		if(npcClass == null || type == null) return;
		
		npcClass.setEntityType(type);
		sender.sendMessage(ChatColor.GREEN + "Updated entity type successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setName(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		npcClass.setName(StringUtil.concatArgs(args, 2));
		sender.sendMessage(ChatColor.GREEN + "Updated entity display name successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setMaxHealth(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		Double maxHealth = parseDoubleType(sender, args[2]);
		if(npcClass == null || maxHealth == null) return;
		
		npcClass.setMaxHealth(maxHealth);
		sender.sendMessage(ChatColor.GREEN + "Updated entity max health successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setLevel(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		Integer level = parseIntType(sender, args[2]);
		if(npcClass == null || level == null) return;

		npcClass.setLevel(level);
		sender.sendMessage(ChatColor.GREEN + "Updated entity level successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setNPCType(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		NPCType npcType = StringUtil.parseEnum(sender, NPCType.class, args[2]);

		npcClass.setNPCType(npcType);
		sender.sendMessage(ChatColor.GREEN + "Updated NPC type successfully.");
		if(!npcClass.getNPCType().hasAIByDefault() && npcClass.hasAI()) {
			npcClass.setAI(false);
			sender.sendMessage(ChatColor.GREEN + "Automatically toggled off AI for this class based on the NPC type.");
		}
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setHolding(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		if(npcClass == null) return;
		
		if(args[4].equalsIgnoreCase("none")) {
			npcClass.setHeldItemType(null);
			sender.sendMessage(ChatColor.GREEN + "Removed held item successfully.");
			MetadataConstants.incrementRevisionCount(npcClass, user(sender));
		}
		
		Material type = StringUtil.parseMaterialType(sender, args[2]);
		if(type == null) return;
		
		npcClass.setHeldItemType(type);
		sender.sendMessage(ChatColor.GREEN + "Set held item successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setAI(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		Boolean ai = parseBooleanType(sender, args[2]);
		if(npcClass == null || ai == null) return;

		npcClass.setAI(ai);
		sender.sendMessage(ChatColor.GREEN + "Updated entity AI successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	private void setImmortal(CommandSender sender, String[] args) {
		NPCClass npcClass = lookupNPCClass(sender, args[0]);
		Boolean immortal = parseBooleanType(sender, args[2]);
		if(npcClass == null || immortal == null) return;
		
		npcClass.setImmortal(immortal);
		sender.sendMessage(ChatColor.GREEN + "Updated entity immortality successfully.");
		MetadataConstants.incrementRevisionCount(npcClass, user(sender));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_NPC)) return true;
		
		if(args.length == 0) {
			showHelp(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "create", "-c", "-create", "--create")) {
			createClass(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "list", "-l", "-list", "--list")) {
			listClasses(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "delete", "del", "-d", "-delete", "--delete")) {
			deleteClass(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "spawn", "-s", "-spawn", "--spawn")) {
			spawnNPCOfClass(sender, args);
		}
		else if(args.length == 1) {
			displayClass(sender, args);
		}
		else if(args[1].equalsIgnoreCase("attribute") || args[1].equalsIgnoreCase("attributes") || args[1].equalsIgnoreCase("att")) {
			manageAttributes(sender, args);	
		}
		else if(args[1].equalsIgnoreCase("addon") || args[1].equalsIgnoreCase("addons") || args[1].equalsIgnoreCase("a")) {
			manageAddons(sender, args);
		}
		else if(args[1].equalsIgnoreCase("loot") || args[1].equalsIgnoreCase("l")) {
			manageLoot(sender, args);
		}
		else if(args[1].equalsIgnoreCase("behavior") || args[1].equalsIgnoreCase("b")) {
			manageBehavior(sender, args);
		}
		else if(args.length == 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc <ClassName> <Property> <Value>");
		}
		else if(args[1].equalsIgnoreCase("type")) {
			setType(sender, args);
		}
		else if(args[1].equalsIgnoreCase("name")) {
			setName(sender, args);
		}
		else if(args[1].equalsIgnoreCase("health")) {
			setMaxHealth(sender, args);
		}
		else if(args[1].equalsIgnoreCase("level")) {
			setLevel(sender, args);
		}
		else if(args[1].equalsIgnoreCase("npctype")) {
			setNPCType(sender, args);
		}
		else if(args[1].equalsIgnoreCase("holding")) {
			setHolding(sender, args);
		}
		else if(args[1].equalsIgnoreCase("ai")) {
			setAI(sender, args);
		}
		else if(args[1].equalsIgnoreCase("immortal")) {
			setImmortal(sender, args);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid usage! /npc");
		}
		
		return true;
	}
	
	private String displayNPCCondition(NPCCondition cond) {
		String result = (cond.isInverse() ? ChatColor.RED + "NOT " + ChatColor.GRAY : "") + cond.getType() + " (";
		switch(cond.getType()) {
		case HAS_COMPLETED_QUEST:
			result += cond.getQuest().getName();
			break;
		case HAS_QUEST_STAGE:
			result += cond.getQuest().getName() + " - stage " + cond.getStageRequirement();
			break;
		case HAS_LEVEL:
			result += "Lv " + cond.getLevelRequirement();
			break;
		case HAS_GOLD:
			result += cond.getGoldRequirement() + " Gold";
			break;
		default:
			return ChatColor.RED + "[Error parsing condition]";
		}
		result += ")";
		return result;
	}
	
	private String displayNPCAction(NPCAction action) {
		String result = action.getType() + " (";
		switch(action.getType()) {
		case BEGIN_DIALOGUE:
			result += action.getDialogue().stream().map(line -> ChatColor.GREEN + line).collect(Collectors.joining(ChatColor.GRAY + " // "));
			break;
		case BEGIN_QUEST:
			result += action.getQuest().getName();
			break;
		case TELEPORT_NPC:
		case PATHFIND_NPC:
			result += StringUtil.locToString(action.getTo());
			break;
		case OPEN_SHOP:
			result += action.getShopName() + " - " + StringUtil.parseList(action.getShopItems()
					.stream()
					.map(item -> item.getItemClassName() + " (x" + item.getQuantity() + "@" + item.getCostPer() + "g per)")
					.collect(Collectors.toList()));
			break;
		default:
			return ChatColor.RED + "[Error parsing action]";
		}
		result += ChatColor.GRAY + ")";
		return result;
	}

}
