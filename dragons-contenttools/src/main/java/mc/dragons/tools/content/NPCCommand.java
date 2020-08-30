package mc.dragons.tools.content;

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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.Addon;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.addon.NPCAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.QuestLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
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
import mc.dragons.core.storage.impl.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class NPCCommand implements CommandExecutor {
	//private UserLoader userLoader;
	private NPCLoader npcLoader;
	private NPCClassLoader npcClassLoader;
	private QuestLoader questLoader;
	private GameObjectRegistry gameObjectRegistry;
	private AddonRegistry addonRegistry;
	
	public NPCCommand(Dragons instance) {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		npcLoader = GameObjectType.NPC.<NPC, NPCLoader>getLoader();
		npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
		questLoader = GameObjectType.QUEST.<Quest, QuestLoader>getLoader();
		gameObjectRegistry = instance.getGameObjectRegistry();
		addonRegistry = instance.getAddonRegistry();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_NPC, true)) return true;
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/npc class -c <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>" + ChatColor.GRAY + " create a new NPC class");
			sender.sendMessage(ChatColor.GRAY + " * Valid NPCTypes are: " + StringUtil.parseList(NPCType.values()));
			sender.sendMessage(ChatColor.YELLOW + "/npc class -l [startingWith]" + ChatColor.GRAY + " list all NPC classes, optionally starting with the specified text (e.g. F2-)");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName>" + ChatColor.GRAY + " view information about NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> type <EntityType>" + ChatColor.GRAY + " change type of NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> name <DisplayName>" + ChatColor.GRAY + " set NPC class display name");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> health <MaxHealth>" + ChatColor.GRAY + " set NPC class max health");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> level <Level>" + ChatColor.GRAY + " set NPC level");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> npctype <NPCType>" + ChatColor.GRAY + " set NPC type");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> holding <MaterialType|NONE>" + ChatColor.GRAY + " set item type the NPC is holding");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> ai <HasAI>" + ChatColor.GRAY + " set whether the NPC has AI");
			sender.sendMessage(ChatColor.DARK_GRAY + " * Pathfinding behavior will still be enabled when triggered by the RPG");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> immortal <IsImmortal>" + ChatColor.GRAY + " set whether the NPC is immortal");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> loot [<RegionName> <ItemClassName> <Chance%|DEL>]" + ChatColor.GRAY + " manage NPC class loot table");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b [<CLICK|HIT> <add|remove <#>>]" + ChatColor.GRAY + " add/remove/view NPC behaviors");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b <CLICK|HIT> <#> condition <add [!]<ConditionType> <ConditionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove conditions on an NPC behavior");
			sender.sendMessage(ChatColor.DARK_GRAY + " * Adding a ! before the ConditionType will negate the condition.");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b <CLICK|HIT> <#> action <add <ActionType> <ActionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove actions on an NPC behavior");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b <CLICK|HIT> <#> action shop add <Action#> <ItemClass> <Quantity> <CostPer>" + ChatColor.GRAY + " manage shop behavior");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> addon [<add|remove> <AddonName>]" + ChatColor.GRAY + " manage addons");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> attribute [<Attribute> <Value|DEL>]" + ChatColor.GRAY + " manage attributes");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -d <ClassName>" + ChatColor.GRAY + " delete NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc spawn <ClassName>  [-phase <Player>]" + ChatColor.GRAY + " spawn a new NPC of the given class");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Class names must not contain spaces.");
			sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("class") || args[0].equalsIgnoreCase("c")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /npc");
				return true;
			}
			if(args[1].equalsIgnoreCase("-c")) {
				if(args.length < 7) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -c <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>");
					return true;
				}
				String npcClassName = args[2];
				EntityType type = StringUtil.parseEntityType(sender, args[3]);
				if(type == null) return true;
				double maxHealth = Double.valueOf(args[4]);
				int level = Integer.valueOf(args[5]);
				NPCType npcType = null;
				try {
					npcType = NPCType.valueOf(args[6]);
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Invalid NPC type! Valid NPC types are " + StringUtil.parseList(NPCType.values()));
					return true;
				}
				NPCClass npcClass = npcClassLoader.registerNew(npcClassName, "Unnamed Entity", type, maxHealth, level, npcType);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "An error occurred! Does a class by this name already exist?");
					return true;
				}
				sender.sendMessage(ChatColor.GREEN + "Successfully created NPC class " + npcClassName);
				return true;
			}
			if(args[1].equalsIgnoreCase("-l")) {
				String startingWith = "";
				if(args.length > 2) {
					startingWith = args[2];
				}
				sender.sendMessage(ChatColor.GREEN + "Listing all NPC classes" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
				for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects(GameObjectType.NPC_CLASS)) {
					NPCClass npcClass = (NPCClass) gameObject;
					if(!npcClass.getClassName().startsWith(startingWith)) continue;
					sender.sendMessage(ChatColor.GRAY + "- " + npcClass.getClassName() + " [Lv " + npcClass.getLevel() + "]");
				}
				return true;
			}
			if(args[1].equalsIgnoreCase("-s")) {
				if(args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /npc");
					return true;
				}
				NPCClass npcClass = npcClassLoader.getNPCClassByClassName(args[2]);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid NPC class name! To see all NPC classes, do /npc class -l");
					return true;
				}
				if(args.length == 3) {
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
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " loot" + ChatColor.YELLOW + " to view loot table");
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " behavior" + ChatColor.YELLOW + " to view behaviors");
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " addon" + ChatColor.YELLOW + " to view addons");
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " attribute" + ChatColor.YELLOW + " to view attributes");
					return true;
				}
				if(args[3].equalsIgnoreCase("attribute") || args[3].equalsIgnoreCase("attributes") || args[3].equalsIgnoreCase("att")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "Listing custom base attributes for NPC class " + npcClass.getClassName() + ":");
						for(Entry<Attribute, Double> attribute : npcClass.getCustomAttributes().entrySet()) {
							sender.sendMessage(ChatColor.GRAY + "- " + attribute.getKey() + ": " + attribute.getValue());
						}
						return true;
					}
					if(args.length < 6) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> attribute <Attribute> <Value|DEL>");
						return true;
					}
					Attribute att = null;
					try {
						att = Attribute.valueOf(args[4].toUpperCase());
					}
					catch(Exception e) {
						sender.sendMessage(ChatColor.RED + "Invalid attribute! Valid attributes are " + StringUtil.parseList(Attribute.values()));
						return true;
					}
					if(args[5].equalsIgnoreCase("DEL")) {
						npcClass.removeCustomAttribute(att);
					}
					else {
						npcClass.setCustomAttribute(att, Double.valueOf(args[5]));
					}
					sender.sendMessage(ChatColor.GREEN + "Updated attributes successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("addon") || args[3].equalsIgnoreCase("addons") || args[3].equalsIgnoreCase("a")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "Listing addons for NPC class " + npcClass.getClassName() + ":");
						for(NPCAddon addon : npcClass.getAddons()) {
							sender.sendMessage(ChatColor.GRAY + "- " + addon.getName());
						}
						return true;
					}
					if(args.length == 5) {
						sender.sendMessage(ChatColor.RED + "Specify an addon name! For a list of addons, do /addon -l");
						return true;
					}
					Addon addon = addonRegistry.getAddonByName(args[5]);
					if(addon == null) {
						sender.sendMessage(ChatColor.RED + "Invalid addon name! For a list of addons, do /addon -l");
						return true;
					}
					if(!(addon instanceof NPCAddon)) {
						sender.sendMessage(ChatColor.RED + "Invalid addon type! Only NPC Addons can be applied to NPCs.");
						return true;
					}
					if(args[4].equalsIgnoreCase("add")) {
						npcClass.addAddon((NPCAddon) addon);
						sender.sendMessage(ChatColor.GREEN + "Added addon " + addon.getName() + " to NPC class " + npcClass.getClassName() + ".");
						return true;
					}
					if(args[4].equalsIgnoreCase("remove")) {
						npcClass.removeAddon((NPCAddon) addon);
						sender.sendMessage(ChatColor.GREEN + "Removed addon " + addon.getName() + " from NPC class " + npcClass.getClassName() + ".");
						return true;
					}
					sender.sendMessage(ChatColor.RED + "Invalid arguments! /npc class -s <ClassName> addon [<add|remove> <AddonName>]");
					return true;
				}
				if(args[3].equalsIgnoreCase("loot") || args[3].equalsIgnoreCase("l")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "Loot Table:");
						for(Entry<String, Map<String, Double>> regionLoot : npcClass.getLootTable().asMap().entrySet()) {
							sender.sendMessage(ChatColor.GRAY + "- Region: " + regionLoot.getKey());
							for(Entry<String, Double> itemLoot : regionLoot.getValue().entrySet()) {
								sender.sendMessage(ChatColor.GRAY + "   - " + itemLoot.getKey() + ": " + itemLoot.getValue() + "%");
							}
						}
						return true;
					}
					if(args.length < 7) {
						sender.sendMessage(ChatColor.RED + "Invalid arguments! /npc class -s <ClassName> loot [<RegionShortName> <ItemClassShortName> <Chance%>]");
						return true;
					}
					if(args[6].equalsIgnoreCase("del")) {
						npcClass.deleteFromLootTable(args[4], args[5]);
						sender.sendMessage(ChatColor.GREEN + "Removed from entity loot table successfully.");
						return true;
					}
					npcClass.updateLootTable(args[4], args[5], Double.valueOf(args[6]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity loot table successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("behavior") || args[3].equalsIgnoreCase("b")) {
					if(args.length == 4) {
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
						return true;
					}
					NPCTrigger trigger = NPCTrigger.valueOf(args[4]);
					NPCConditionalActions behaviorsLocal = npcClass.getConditionalActions(trigger);
					NPCConditionalActions parsedBehaviors = npcClass.getConditionalActions(trigger);
					Document conditionals = npcClass.getStorageAccess().getDocument().get("conditionals", Document.class);
					List<Document> behaviors = conditionals
							.getList(trigger.toString(), Document.class);
					if(args[5].equalsIgnoreCase("add")) {
						behaviors.add(new Document("conditions", new ArrayList<Document>()).append("actions", new ArrayList<Document>()));
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						parsedBehaviors.addLocalEntry();
						sender.sendMessage(ChatColor.GREEN + "Successfully added a new conditional with trigger " + trigger);
						return true;
					}
					else if(args[5].equalsIgnoreCase("remove")) {
						if(args.length == 6) {
							sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> remove <#>");
							return true;
						}
						behaviors.remove((int) Integer.valueOf(args[6]));
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						parsedBehaviors.removeLocalEntry(Integer.valueOf(args[6]));
						sender.sendMessage(ChatColor.GREEN + "Successfully removed conditional #" + args[6] + " with trigger " + trigger);
						return true;
					}
					else {
						if(args.length < 8) {
							sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /npc");
							return true;
						}
						int behaviorNo = 0;
						try {
							behaviorNo = Integer.valueOf(args[5]);
						}
						catch(IndexOutOfBoundsException e) {
							sender.sendMessage(ChatColor.RED + "Invalid behavior index! To view behavior info, do /npc class -s <ClassName> behavior");
							return true;
						}
						//Entry<List<NPCCondition>, List<NPCAction>> conditions = behaviorsParsed.getConditional(behaviorNo);
						Document behavior = behaviors.get(behaviorNo);
						if(args[6].equalsIgnoreCase("condition") || args[6].equalsIgnoreCase("c")) {
							List<Document> conditions = behavior.getList("conditions", Document.class);
							if(args[7].equalsIgnoreCase("add")) {
								if(args.length < 10) {
									sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> condition add <ConditionType> <ConditionParams...>");
									return true;
								}
								boolean inverted = args[8].startsWith("!");
								if(inverted) args[8] = args[8].replace("!", "");
								NPCConditionType condType = NPCConditionType.valueOf(args[8]);
								NPCCondition cond = null;
								switch(condType) {
								case HAS_COMPLETED_QUEST:
									cond = NPCCondition.hasCompletedQuest(questLoader.getQuestByName(args[9]), inverted);
									break;
								case HAS_QUEST_STAGE:
									cond = NPCCondition.hasQuestStage(questLoader.getQuestByName(args[9]), Integer.valueOf(args[10]), inverted);
									break;
								case HAS_GOLD:
									cond = NPCCondition.hasGold(Double.valueOf(args[9]), inverted);
									break;
								case HAS_LEVEL:
									cond = NPCCondition.hasLevel(Integer.valueOf(args[9]), inverted);
									break;
								}
								conditions.add(cond.toDocument());
								behaviorsLocal.getConditional(behaviorNo).getKey().add(cond);
							}
							else if(args[7].equalsIgnoreCase("remove")) {
								if(args.length < 9) {
									sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> condition remove <#>");
									return true;
								}
								conditions.remove((int) Integer.valueOf(args[8]));
								behaviorsLocal.getConditional(behaviorNo).getKey().remove((int) Integer.valueOf(args[8]));
							}
							behavior.append("conditions", conditions);
						}
						else if(args[6].equalsIgnoreCase("action") || args[6].equalsIgnoreCase("a")) {
							List<Document> actions = behavior.getList("actions", Document.class);
							if(args[7].equalsIgnoreCase("shop")) {
								if(args[8].equalsIgnoreCase("add")) {
									if(args.length < 13) {
										sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action shop add <#> <ItemClass> <Quantity> <CostPer>");
										return true;
									}
									int actionNo = Integer.valueOf(args[9]);
									NPCAction action = behaviorsLocal.getConditional(behaviorNo).getValue().get(actionNo);
									action.getShopItems().add(new ShopItem(args[10], Integer.valueOf(args[11]), Double.valueOf(args[12])));
									actions.get(actionNo).putAll(action.toDocument());
									behavior.append("actions", actions);
									npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
									sender.sendMessage(ChatColor.GREEN + "Added item to shop successfully.");
									return true;
								}
							}
							if(args[7].equalsIgnoreCase("add")) {
								NPCActionType actionType = NPCActionType.valueOf(args[8]);
								NPCAction action = null;
								switch(actionType) {
								case BEGIN_DIALOGUE:
									if(args.length < 10) {
										sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action add BEGIN_DIALOGUE <DialogueLine1|DialogueLine2|...>");
										return true;
									}
									action = NPCAction.beginDialogue(npcClass, Arrays.asList(StringUtil.concatArgs(args, 9)
											.replaceAll(Pattern.quote("%PH%"), user.getLocalData().get("placeholder", "(Empty placeholder)"))
											.split(Pattern.quote("|"))));
									break;
								case BEGIN_QUEST:
									if(args.length < 10) {
										sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action add BEGIN_QUEST <QuestShortName>");
										return true;
									}
									action = NPCAction.beginQuest(npcClass, questLoader.getQuestByName(args[9]));
									break;
								case OPEN_SHOP:
									if(args.length < 10) {
										sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action add OPEN_SHOP <Shop Title>");
										return true;
									}
									action = NPCAction.openShop(npcClass, StringUtil.concatArgs(args, 9), new ArrayList<>());
									break;
								case TELEPORT_NPC:
									action = NPCAction.teleportNPC(npcClass, player.getLocation());
									break;
								case PATHFIND_NPC:
									action = NPCAction.pathfindNPC(npcClass, player.getLocation());
									break;
								}
								actions.add(action.toDocument());
								behaviorsLocal.getConditional(behaviorNo).getValue().add(action);
							}
							else if(args[7].equalsIgnoreCase("remove")) {
								if(args.length < 9) {
									sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> behavior <CLICK|HIT> <#> action remove <#>");
									return true;
								}
								actions.remove((int) Integer.valueOf(args[8]));
								behaviorsLocal.getConditional(behaviorNo).getValue().remove((int) Integer.valueOf(args[8]));
							}
							behavior.append("actions", actions);
						}
						else {
							sender.sendMessage(ChatColor.RED + "Invalid behavioral arguments! For usage info, do /npc");
							return true;
						}
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						sender.sendMessage(ChatColor.GREEN + "Updated behavior successfully.");
						return true;
					}
					
				}
				if(args.length == 4) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -s <ClassName> <Attribute> <Value>");
					return true;
				}
				if(args[3].equalsIgnoreCase("type")) {
					EntityType type = EntityType.valueOf(args[4].toUpperCase());
					npcClass.setEntityType(type);
					sender.sendMessage(ChatColor.GREEN + "Updated entity type successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("name")) {
					npcClass.setName(StringUtil.concatArgs(args, 4));
					sender.sendMessage(ChatColor.GREEN + "Updated entity display name successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("health")) {
					npcClass.setMaxHealth(Double.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity max health successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("level")) {
					npcClass.setLevel(Integer.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity level successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("npctype")) {
					npcClass.setNPCType(NPCType.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated NPC type successfully.");
					if(!npcClass.getNPCType().hasAIByDefault() && npcClass.hasAI()) {
						npcClass.setAI(false);
						sender.sendMessage(ChatColor.GREEN + "Automatically toggled off AI for this class based on the NPC type.");
					}
					return true;
				}
				if(args[3].equalsIgnoreCase("holding")) {
					if(args[4].equalsIgnoreCase("none")) {
						npcClass.setHeldItemType(null);
						sender.sendMessage(ChatColor.GREEN + "Removed held item successfully.");
						return true;
					}
					Material type = StringUtil.parseMaterialType(sender, args[4]);
					if(type == null) return true;
					npcClass.setHeldItemType(type);
					sender.sendMessage(ChatColor.GREEN + "Set held item successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("ai")) {
					npcClass.setAI(Boolean.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity AI successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("immortal")) {
					npcClass.setImmortal(Boolean.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity immortality successfully.");
					return true;
				}
				return true;
			}
			if(args[1].equalsIgnoreCase("-d")) {
				if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_DELETE, true)) return true;
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Specify a class name to delete! /npc class -d <ClassName>");
					return true;
				}
				NPCClass npcClass = npcClassLoader.getNPCClassByClassName(args[2]);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid NPC class name!");
					return true;
				}
				gameObjectRegistry.removeFromDatabase(npcClass);
				sender.sendMessage(ChatColor.GREEN + "Successfully deleted NPC class.");
				return true;
			}
		}
		if(args[0].equalsIgnoreCase("spawn")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify an NPC class to spawn! /npc spawn <ClassName>");
				return true;
			}
			NPC npc = npcLoader.registerNew(player.getWorld(), player.getLocation(), args[1]);
			sender.sendMessage(ChatColor.GREEN + "Spawned an NPC of class " + args[1] + " at your location.");
			if(args.length >= 3) {
				if(args[2].equalsIgnoreCase("-phase")) {
					Player phaseFor = Bukkit.getPlayerExact(args[3]);
					npc.phase(phaseFor);
					sender.sendMessage(ChatColor.GREEN + "Phased NPC successfully.");
				}
			}
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /npc");
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
		}
		result += ChatColor.GRAY + ")";
		return result;
	}

}
