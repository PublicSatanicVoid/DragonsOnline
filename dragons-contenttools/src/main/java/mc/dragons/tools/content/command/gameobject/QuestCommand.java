package mc.dragons.tools.content.command.gameobject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

import mc.dragons.core.Dragons;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestAction;
import mc.dragons.core.gameobject.quest.QuestAction.QuestActionType;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.quest.QuestTrigger;
import mc.dragons.core.gameobject.quest.QuestTrigger.TriggerType;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

public class QuestCommand extends DragonsCommandExecutor {
	private GameObjectRegistry registry = instance.getGameObjectRegistry();

	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/quest create <ShortName> <LvMin>" + ChatColor.GRAY + " create a new quest");
		sender.sendMessage(ChatColor.YELLOW + "/quest list" + ChatColor.GRAY + " list all quests");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName>" + ChatColor.GRAY + " view quest information");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> questname <FullQuestName>" + ChatColor.GRAY + " set full quest name");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> lvmin <NewLvMin>" + ChatColor.GRAY + " set quest level min");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stages" + ChatColor.GRAY + " list all quest steps");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#>" + ChatColor.GRAY + " view quest stage");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#> name <QuestStageName>" + ChatColor.GRAY + " set name of quest stage");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <add|<Stage#> trigger> <TriggerType> [TriggerParam]" + ChatColor.GRAY + " update quest stage trigger");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#> action "
				+ "<add <ActionType> [ActionParams...]|"
				+ "dialogue add <Action#> <Message>|"
				+ "branch add <Action#> <GoToStage#> <TriggerType> [TriggerParams...]|"
				+ "del <Action#>>" + ChatColor.GRAY + " manage quest stage actions");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#> del" + ChatColor.GRAY + " delete quest stage");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> delete" + ChatColor.GRAY + " delete quest");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> [un]lock" + ChatColor.GRAY + " lock or unlock quest");
		sender.sendMessage(ChatColor.YELLOW + "/testquest <ShortName>" + ChatColor.GRAY + " test quest");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Quest ShortNames must not contain spaces.");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " All quests MUST end with a stage named \"Complete\" with trigger INSTANT and no actions.");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void createQuest(CommandSender sender, String[] args) {
		if(args.length < 3) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest create <ShortName> <LvMin>");
			return;
		}
		Integer lvMin = parseInt(sender, args[2]);
		if(lvMin == null) return;
		Quest quest = questLoader.registerNew(args[1], "Unnamed Quest", lvMin);
		MetadataConstants.addBlankMetadata(quest, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Quest created successfully.");
	}
	
	private void listQuests(CommandSender sender, String[] args) {
		unusedParameter(args);
		sender.sendMessage(ChatColor.GREEN + "Listing all quests:");
		int nInvalid = 0;
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.QUEST)) {
			Quest quest = (Quest) gameObject;
			if(!quest.isValid()) nInvalid++;
			sender.sendMessage(ChatColor.GRAY + "- " + quest.getName() + " (" + quest.getQuestName() + ") [Lv " + quest.getLevelMin() + "] [" + quest.getSteps().size() + " steps]" + (quest.isValid() ? "" : ChatColor.RED + " (Incomplete Setup!)"));
		}
		if(nInvalid > 0) {
			sender.sendMessage(ChatColor.RED + "" + nInvalid + " invalid or incomplete quests found!");
		}
	}
	
	private void deleteQuest(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE)) return;
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Specify a quest to delete! /quest delete <ShortName>");
			return;
		}
		Quest quest = lookupQuest(sender, args[1]);
		if(quest == null) return;
		registry.removeFromDatabase(quest);
		sender.sendMessage(ChatColor.GREEN + "Deleted quest successfully.");
	}
	
	private void displayQuest(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		sender.sendMessage(ChatColor.GREEN + "=== Quest: " + quest.getName() + " ===");
		sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + quest.getIdentifier().toString());
		if(quest.isLocked()) {
			sender.sendMessage(ChatColor.RED + "Currently locked to players");
		}
		sender.sendMessage(ChatColor.GRAY + "Full name: " + ChatColor.GREEN + quest.getQuestName());
		sender.sendMessage(ChatColor.GRAY + "Level min: " + ChatColor.GREEN + quest.getLevelMin());
		sender.sendMessage(ChatColor.GRAY + "# Stages: " + ChatColor.GREEN + quest.getSteps().size());
		sender.spigot().sendMessage(ObjectMetadataCommand.getClickableMetadataLink(GameObjectType.QUEST, quest.getUUID()));
		if(!quest.isValid()) {
			sender.sendMessage(ChatColor.RED + "Warning: Invalid or incomplete quest! To be valid, the final stage must be named \"Complete\", have trigger type INSTANT, and have no actions.");
		}
	}
	
	private void displayStages(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		sender.sendMessage(ChatColor.GREEN + "Listing quest stages:");
		int i = 0;
		for(QuestStep step : quest.getSteps()) {
			sender.sendMessage(ChatColor.YELLOW + "#" + i + ": " + ChatColor.GRAY + step.getStepName() + " [Trigger: " + step.getTrigger().getTriggerType() + "] [" + step.getActions().size() + " actions" + "]");
			i++;
		}
	}
	
	private void updateQuestName(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setQuestName(StringUtil.concatArgs(args, 2));
		sender.sendMessage(ChatColor.GREEN + "Updated quest name successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Set quest name to " + StringUtil.concatArgs(args, 2));
	}
	
	private void updateLevelMin(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLevelMin(Integer.valueOf(args[2]));
		sender.sendMessage(ChatColor.GREEN + "Updated quest level min successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Set level min to " + args[2]);
	}
	
	private void addStage(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		if(args.length < 4) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage add <TriggerType> [TriggerParams...]");
			return;
		}
		Document base = Document.parse(quest.getData().toJson());
		TriggerType type = StringUtil.parseEnum(sender, TriggerType.class, args[3]);
		QuestStep step = new QuestStep("Unnamed Step", makeTrigger(type, args.length > 4 ? Arrays.copyOfRange(args, 4, args.length) : null), new ArrayList<>(), quest);
		quest.addStep(step);
		sender.sendMessage(ChatColor.GREEN + "Added new quest stage successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Added new quest stage");
	}
	
	private void manageStage(CommandSender sender, String[] args) {
		if(args.length <= 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <<Stage#> [name|trigger|action|del] [...]|add <trigger> [params...]");
		}
		else if(args[2].equalsIgnoreCase("add")) {
			addStage(sender, args);
		}
		else if(args.length == 3) {
			displayStage(sender, args);
		}
		else if(args[3].equalsIgnoreCase("action")) {
			if(args.length == 4) {
				displayActionHelp(sender);
			}
			else if(args[4].equalsIgnoreCase("add")) {
				addAction(sender, args);
			}
			else if(args[4].equalsIgnoreCase("dialogue")) {
				editDialogue(sender, args);
			}
			else if(args[4].equalsIgnoreCase("branch")) {
				addBranch(sender, args);
			}
			else if(args[4].equalsIgnoreCase("choice")) {
				addChoice(sender, args);
			}
			else if(args[4].equalsIgnoreCase("del")) {
				deleteAction(sender, args);
			}
		}
		else if(args[3].equalsIgnoreCase("del")) {
			deleteStep(sender, args);
		}
		else if(args.length == 4) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <Stage#> <Attribute> <Value|Arguments...>");
		}
		else if(args[3].equalsIgnoreCase("name")) {
			updateStepName(sender, args);
		}
		else if(args[3].equalsIgnoreCase("trigger")) {
			updateStepTrigger(sender, args);
		}
	}
	
	private void displayStage(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		QuestStep step = quest.getSteps().get(stepNo);
		sender.sendMessage(ChatColor.GREEN + "Quest Stage #" + stepNo);
		sender.sendMessage(ChatColor.GRAY + "Objective: " + ChatColor.GREEN + step.getStepName());
		sender.sendMessage(ChatColor.GRAY + "Trigger: " + ChatColor.GREEN + step.getTrigger().getTriggerType());
		sender.sendMessage(ChatColor.GRAY + "- " + displayTrigger(step.getTrigger()));
		sender.sendMessage(ChatColor.GRAY + "Actions:");
		int i = 0;
		for(QuestAction action : step.getActions()) {
			sender.sendMessage(ChatColor.YELLOW + "#" + i + ": " + ChatColor.GRAY + action.getActionType().toString());
			sender.sendMessage(ChatColor.GRAY + "    - " + displayAction(action));
			i++;
		}
	}
	
	private void displayActionHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add TELEPORT_PLAYER");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add SPAWN_NPC <NpcClass> [NpcReferenceName] [Phased?]");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add TELEPORT_NPC <NpcReferenceName>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add PATHFIND_NPC <NpcReferenceName> [GotoStage#WhenComplete]");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add BEGIN_DIALOGUE <NpcClass>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add GIVE_XP <XPAmount>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add GOTO_STAGE <Stage#> <ShouldNotify>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add <GIVE_ITEM|TAKE_ITEM> <ItemClass> <Amount>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add ADD_POTION_EFFECT <EffectType> <Duration> <Amplifier>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add REMOVE_POTION_EFFECT <EffectType>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add COMPLETION_HEADER");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add WAIT <Seconds>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add CHOICES");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action dialogue add <Action#> <DialogueText>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action branch add <GoToStage#> <TriggerType> <TriggerParam|NONE>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action choice add <Action#> <GoToStage#> <ChoiceText>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action del <Action#>");
	}
	
	private void addAction(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		QuestStep step = quest.getSteps().get(stepNo);
		User user = user(sender);
		Document base = Document.parse(quest.getData().toJson());
		if(StringUtil.equalsAnyIgnoreCase(args[5], "TELEPORT_PLAYER", "TeleportPlayer")) {
			if(!requirePlayer(sender)) return;
			step.addAction(QuestAction.teleportPlayerAction(quest, user.getPlayer().getLocation()));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "SPAWN_NPC", "SpawnNPC")) {
			step.addAction(QuestAction.spawnNPCAction(quest, npcClassLoader.getNPCClassByClassName(args[6]), user.getPlayer().getLocation(), args.length <= 7 ? "" : args[7], args.length <= 8 ? false : Boolean.valueOf(args[9])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "TELEPORT_NPC", "TeleportNPC")) {
			if(!requirePlayer(sender)) return;
			step.addAction(QuestAction.teleportNPCAction(quest, args[6], user.getPlayer().getLocation()));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "PATHFIND_NPC", "PathfindNPC", "Pathfind", "Path")) {
			if(!requirePlayer(sender)) return;
			step.addAction(QuestAction.pathfindNPCAction(quest, args[6], user.getPlayer().getLocation(), args.length <= 7 ? -1 : Integer.valueOf(args[7])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "BEGIN_DIALOGUE", "BeginDialogue", "Dialogue", "Speak", "Talk", "Conversation", "Converse")) {
			step.addAction(QuestAction.beginDialogueAction(quest, npcClassLoader.getNPCClassByClassName(args[6]), new ArrayList<>()));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "GIVE_XP", "GiveXP", "XP")) {
			step.addAction(QuestAction.giveXPAction(quest, Integer.valueOf(args[6])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "GOTO_STAGE", "GoToStage", "GoTo", "Jump")) {
			step.addAction(QuestAction.goToStageAction(quest, Integer.valueOf(args[6]), Boolean.valueOf(args[7])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "TAKE_ITEM", "TakeItem", "Take")) {
			step.addAction(QuestAction.takeItemAction(quest, itemClassLoader.getItemClassByClassName(args[6]), args.length <= 7 ? 1 : Integer.valueOf(args[7])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "GIVE_ITEM", "GiveItem", "Give")) {
			step.addAction(QuestAction.giveItemAction(quest, itemClassLoader.getItemClassByClassName(args[6]), args.length <= 7 ? 1 : Integer.valueOf(args[7])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "ADD_POTION_EFFECT", "AddPotionEffect", "AddPotion", "AddEffect")) {
			step.addAction(QuestAction.addPotionEffectAction(quest, PotionEffectType.getByName(args[6]), Integer.valueOf(args[7]), Integer.valueOf(args[8])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "REMOVE_POTION_EFFECT", "RemovePotionEffect", "RemovePotion", "RemoveEffect")) {
			step.addAction(QuestAction.removePotionEffectAction(quest, PotionEffectType.getByName(args[6])));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[5], "COMPLETION_HEADER", "CompletionHeader")) {
			step.addAction(QuestAction.completionHeaderAction(quest));
		}
		else if(args[5].equalsIgnoreCase("WAIT")) {
			step.addAction(QuestAction.waitAction(quest, Integer.valueOf(args[6])));
		}
		else if(args[5].equalsIgnoreCase("CHOICES")) {
			step.addAction(QuestAction.choicesAction(quest, new LinkedHashMap<>()));
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid action type! Valid action types are " + StringUtil.parseList(QuestActionType.values()));
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "Added new action to quest stage successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Added action to stage " + args[2]);
	}
	
	private void editDialogue(CommandSender sender, String[] args) {
		if(args.length < 8) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <Stage#> action dialogue [list <Action#> | add <Action#> <Dialogue Line> | remove <Action#> <Line#>]");
			return;
		}
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		QuestStep step = quest.getSteps().get(stepNo);
		
		if(args[5].equalsIgnoreCase("list")) {
			List<String> dialogue = step.getActions().get(Integer.valueOf(args[6])).getDialogue();
			if(dialogue.size() == 0) {
				sender.sendMessage(ChatColor.RED + "No dialogue found for this step!");
			}
			else {
				for(String line : dialogue) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + line);
				}
			}
		}
		else if(args[5].equalsIgnoreCase("add")) {
			String dialogue = StringUtil.concatArgs(args, 7);
			User user = user(sender);
			if(isPlayer(sender)) {
				dialogue = dialogue.replaceAll(Pattern.quote("%PH%"), user.getLocalData().get("placeholder", "(Empty placeholder)"));
			}
			Document base = Document.parse(quest.getData().toJson());
			step.addDialogue(Integer.valueOf(args[6]), dialogue);
			sender.sendMessage(ChatColor.GREEN + "Added dialogue to quest stage action successfully.");
			MetadataConstants.logRevision(quest, user(sender), base, "Added dialogue to action " + args[6] + " of stage " + args[2]);
		}
		else if(args[5].equalsIgnoreCase("remove")) {
			Document base = Document.parse(quest.getData().toJson());
			step.removeDialogue(Integer.valueOf(args[6]), Integer.valueOf(args[7]));
			sender.sendMessage(ChatColor.GREEN + "Removed dialogue line from quest stage action successfully.");
			MetadataConstants.logRevision(quest, user(sender), base, "Removed dialogue line " + args[7] + " from action " + args[6] + " of stage " + args[2]);
		}
	}
	
	private void addBranch(CommandSender sender, String[] args) {
		if(args.length < 8) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <Stage#> action branch add <GoToStage#> <TriggerType> [TriggerParams...]");
			return;
		}
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		if(args[5].equalsIgnoreCase("add")) {
			QuestTrigger trigger = makeTrigger(TriggerType.valueOf(args[7]), Arrays.copyOfRange(args, 8, args.length));
			QuestAction action = QuestAction.goToStageAction(quest, Integer.valueOf(args[6]), false);
			step.addBranchPoint(trigger, action);
			sender.sendMessage(ChatColor.GREEN + "Added branch point successfully.");
			MetadataConstants.logRevision(quest, user(sender), base, "Added branch point to stage " + args[2]);
		}
	}
	
	private void addChoice(CommandSender sender, String[] args) {
		if(args.length < 9) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <Stage#> action choice add <Action#> <GoToStage#> <Choice Text>");
			return;
		}
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		if(args[5].equalsIgnoreCase("add")) {
			String choice = StringUtil.concatArgs(args, 8);
			User user = user(sender);
			if(isPlayer(sender)) {
				choice = choice.replaceAll(Pattern.quote("%PH%"), user.getLocalData().get("placeholder", "(Empty placeholder)"));
			}
			step.addChoice(Integer.valueOf(args[6]), choice, Integer.valueOf(args[8]));
			sender.sendMessage(ChatColor.GREEN + "Added choice to quest stage action successfully.");
			MetadataConstants.logRevision(quest, user(sender), base, "Added quest choice to action " + args[6] + " of stage " + args[2]);
		}
	}
	
	private void deleteAction(CommandSender sender, String[] args) {
		if(args.length < 6) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <Stage#> action del <Action#>");
			return;
		}
		
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		
		step.deleteAction(Integer.valueOf(args[5]));
		sender.sendMessage(ChatColor.GREEN + "Removed action from quest stage successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Removed action " + args[4] + " from stage " + args[2]);
	}
	
	private void updateStepName(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		
		step.setStepName(StringUtil.concatArgs(args, 4));
		sender.sendMessage(ChatColor.GREEN + "Updated quest step name successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Set name of stage " + args[2] + " to " + StringUtil.concatArgs(args, 4));
	}
	
	private void updateStepTrigger(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		
		TriggerType type = StringUtil.parseEnum(sender, TriggerType.class, args[4]);
		step.setTrigger(makeTrigger(type, args.length > 5 ? args[5] : null));
		sender.sendMessage(ChatColor.GREEN + "Updated quest stage trigger successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Set trigger of stage " + args[2] + " to " + type);
	}
	
	private void deleteStep(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		
		quest.delStep(stepNo);
		sender.sendMessage(ChatColor.GREEN + "Deleted quest stage successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Deleted step " + args[2]);
	}
	
	private void lockQuest(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[1]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLocked(true);
		sender.sendMessage(ChatColor.GREEN + "Locked quest successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Locked quest");
	}
	
	private void unlockQuest(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[1]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLocked(false);
		sender.sendMessage(ChatColor.GREEN + "Unlocked quest successfully.");
		MetadataConstants.logRevision(quest, user(sender), base, "Unlocked quest");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_QUEST)) return true;
		
		if(args.length == 0) {
			showHelp(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "create", "-c", "-create", "--create")) {
			createQuest(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "list", "-l", "-list", "--list")) {
			listQuests(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "delete", "del", "-d", "-delete", "--delete")) {
			deleteQuest(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "lock", "-lock", "--lock")) {
			lockQuest(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "unlock", "-unlock", "--unlock")) {
			unlockQuest(sender, args);
		}
		else if(args.length == 1) {
			displayQuest(sender, args);
		}
		else if(args[1].equalsIgnoreCase("stages")) {
			displayStages(sender, args);
		}
		else if(args[1].equalsIgnoreCase("stage")) {
			manageStage(sender, args);
		}
		else if(args.length < 3) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> <Attribute> <Value|Arguments...>");
		}
		else if(args[1].equalsIgnoreCase("questname")) {
			updateQuestName(sender, args);
		}
		else if(args[1].equalsIgnoreCase("lvmin")) {
			updateLevelMin(sender, args);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /quest");
		}
		
		return true;
	}
	
	private QuestTrigger makeTrigger(TriggerType type, String... params) {
		switch(type) {
		case INSTANT:
			return QuestTrigger.instant();
		case NEVER:
			return QuestTrigger.never();
		case KILL_NPC:
			return QuestTrigger.onKillNPC(npcClassLoader.getNPCClassByClassName(params[0]), Integer.valueOf(params[1]));
		case CLICK_NPC:
			return QuestTrigger.onClickNPC(npcClassLoader.getNPCClassByClassName(params[0]));
		case ENTER_REGION:
			return QuestTrigger.onEnterRegion(regionLoader.getRegionByName(params[0]));
		case EXIT_REGION:
			return QuestTrigger.onExitRegion(regionLoader.getRegionByName(params[0]));
		case BRANCH_CONDITIONAL:
			return QuestTrigger.branchConditional(new HashMap<>());
		case HAS_ITEM:
			return QuestTrigger.hasItem(itemClassLoader.getItemClassByClassName(params[0]), Integer.valueOf(params[1]));
		}
		return null;
	}
	
	private String displayTrigger(QuestTrigger trigger) {
		switch(trigger.getTriggerType()) {
		case INSTANT:
			return "Triggered immediately";
		case NEVER:
			return "Never triggered (limbo)";
		case KILL_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + trigger.getNPCClass().getName() + " (" + trigger.getNPCClass().getClassName() + ")"
				+ ChatColor.GRAY + "; Quantity: " + ChatColor.GREEN + trigger.getQuantity();
		case CLICK_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + trigger.getNPCClass().getName() + " (" + trigger.getNPCClass().getClassName() + ")";
		case ENTER_REGION:
		case EXIT_REGION:
			return "Target Region: " + ChatColor.GREEN + trigger.getRegion().getName() + " (" + trigger.getRegion().getFlags().getString("fullname") + ")";
		case BRANCH_CONDITIONAL:
			String triggerMsg = "Branch Points:";
			for(Entry<QuestTrigger, QuestAction> conditional : trigger.getBranchPoints().entrySet()) {
				triggerMsg += "\n    - " + conditional.getKey().getTriggerType().toString() + " (" + displayTrigger(conditional.getKey()) + ChatColor.GRAY + ")" 
						+ " -> " + conditional.getValue().getActionType().toString() + " (" + displayAction(conditional.getValue()) + ChatColor.GRAY + ")";
			}
			return triggerMsg;
		case HAS_ITEM:
			return "Triggered immediately once player has " + ChatColor.GREEN + trigger.getQuantity() + ChatColor.GRAY + " of " + ChatColor.GREEN + trigger.getItemClass().getClassName();
		default:
			break;
		}
		return "";
	}
	
	private String displayAction(QuestAction action) {
		switch(action.getActionType()) {
		case TELEPORT_PLAYER:
			return "Target Location: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation());
		case SPAWN_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
					+ ChatColor.GRAY + "    - Location: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation()) + "\n"
					+ ChatColor.GRAY + (action.getNPCReferenceName() == null ? "    - No reference name" : "    - Reference name: " + ChatColor.GREEN + action.getNPCReferenceName()) + "\n"
					+ ChatColor.GRAY + (action.isPhased() ? "    - Phased for player" : "    - Not phased");
		case TELEPORT_NPC:
			return "NPC Reference Name: " + ChatColor.GREEN + action.getNPCReferenceName() + "\n"
					+ ChatColor.GRAY + "    - To: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation());
		case PATHFIND_NPC:
			return "NPC Reference Name: " + ChatColor.GREEN + action.getNPCReferenceName() + "\n"
					+ ChatColor.GRAY + "    - To: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation()) + "\n"
					+ ChatColor.GRAY + (action.getGotoStage() == -1 ? "    - No special behavior upon completion" : "    - On completion: " + ChatColor.GREEN + "Go to stage " + action.getGotoStage());
		case BEGIN_DIALOGUE:
			String actionMsg = "Spoken By: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
				+ ChatColor.GRAY + "    - Dialogue:";
			int j = 0;
			for(String line : action.getDialogue()) {
				actionMsg += "\n" + ChatColor.YELLOW + "      #" + j + ":  " + ChatColor.GRAY + line + "";
				j++;
			}
			return actionMsg;
		case GIVE_XP:
			return ChatColor.GRAY + "Amount: " + ChatColor.GREEN + action.getXPAmount();
		case GOTO_STAGE:
			return ChatColor.GRAY + "Stage: " + ChatColor.GREEN + action.getGotoStage();
		case TAKE_ITEM:
		case GIVE_ITEM:
			return ChatColor.GRAY + "Item Class: " + ChatColor.GREEN + action.getItemClass().getClassName() + ChatColor.GRAY + "; Amount: " + ChatColor.GREEN + action.getQuantity();
		case ADD_POTION_EFFECT:
			return ChatColor.GRAY + "Effect Type: " + ChatColor.GREEN + action.getEffectType().getName() + ChatColor.GRAY + "; Duration: " + ChatColor.GREEN + action.getDuration() + "s"
					+ ChatColor.GRAY + "; Amplifier: " + ChatColor.GREEN + action.getAmplifier();
		case REMOVE_POTION_EFFECT:
			return ChatColor.GRAY + "Effect Type: " + ChatColor.GREEN + action.getEffectType().getName();
		case COMPLETION_HEADER:
			return ChatColor.GRAY + "(No data)";
		case WAIT:
			return ChatColor.GRAY + "Time: " + ChatColor.GREEN + action.getWaitTime() + "s";
		case CHOICES:
			String choiceMsg = "Choices:\n";
			for(Entry<String, Integer> choice : action.getChoices().entrySet()) {
				choiceMsg += "      - " + choice.getKey() + ChatColor.YELLOW + " -> " + ChatColor.GRAY + "Step " + choice.getValue() + "\n";
			}
			return choiceMsg;
		}
		return "";
	}
}
