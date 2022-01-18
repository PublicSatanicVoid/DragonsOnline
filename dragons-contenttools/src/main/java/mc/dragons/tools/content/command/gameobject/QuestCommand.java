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
import org.bukkit.Material;
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
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.AuditLogLoader;

public class QuestCommand extends DragonsCommandExecutor {
	private GameObjectRegistry registry = dragons.getGameObjectRegistry();
	private AuditLogLoader AUDIT_LOG = dragons.getLightweightLoaderRegistry().getLoader(AuditLogLoader.class);

	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/quest create <ShortName> <LvMin>" + ChatColor.GRAY + " create a new quest");
		sender.sendMessage(ChatColor.YELLOW + "/quest list" + ChatColor.GRAY + " list all quests");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName>" + ChatColor.GRAY + " view quest information");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> questname <FullQuestName>" + ChatColor.GRAY + " set full quest name");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> lvmin <NewLvMin>" + ChatColor.GRAY + " set quest level min");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stages" + ChatColor.GRAY + " list all quest steps");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#>" + ChatColor.GRAY + " view quest stage");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#> name <QuestStageName>" + ChatColor.GRAY + " set name of quest stage");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <add|insert <StageBefore#>|<Stage#> trigger> <TriggerType> [TriggerParam]" + ChatColor.GRAY + " update quest stage trigger");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage <Stage#> action "
				+ "<add <ActionType> [ActionParams...]|"
				+ "dialogue add <Action#> <Message>|"
				+ "branch add <Action#> <GoToStage#> <TriggerType> [TriggerParams...]|"
				+ "del <Action#>>" + ChatColor.GRAY + " manage quest stage actions");
		sender.sendMessage(ChatColor.YELLOW + "/quest <ShortName> stage del <Stage#>" + ChatColor.GRAY + " delete quest stage");
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
//		MetadataConstants.addBlankMetadata(quest, user(sender));
		AUDIT_LOG.saveEntry(quest, user(sender), "Created");
		sender.sendMessage(ChatColor.GREEN + "Quest created successfully.");
	}
	
	private void listQuests(CommandSender sender, String[] args) {
		unusedParameter(args);
		sender.sendMessage(ChatColor.GREEN + "Listing all quests:");
		int nInvalid = 0;
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.QUEST)) {
			Quest quest = (Quest) gameObject;
			if(!quest.isValid()) nInvalid++;
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + quest.getName() + " (" + quest.getQuestName() + ") [Lv " + quest.getLevelMin() + "] [" + quest.getSteps().size() + " steps]"
				+ (quest.isValid() ? "" : ChatColor.RED + " (Incomplete Setup!)"), "/quest " + quest.getName(), "Click to view quest details"));
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
		AUDIT_LOG.saveEntry(quest, user(sender), "Deleted");
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
		sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "[Test Quest] ", "/testquest " + quest.getName(), true, "Click to test quest"), 
				ObjectMetadataCommand.getClickableMetadataLink(GameObjectType.QUEST, quest.getUUID()));
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
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Set quest name to " + StringUtil.concatArgs(args, 2));
	}
	
	private void updateLevelMin(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLevelMin(Integer.valueOf(args[2]));
		sender.sendMessage(ChatColor.GREEN + "Updated quest level min successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Set level min to " + args[2]);
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
		if(type == null) return;
		QuestStep step = new QuestStep("Unnamed Step", makeTrigger(quest, type, args.length > 4 ? Arrays.copyOfRange(args, 4, args.length) : null), new ArrayList<>(), quest);
		quest.addStep(step);
		sender.sendMessage(ChatColor.GREEN + "Added new quest stage successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Added new quest stage");
	}
	
	private void insertStage(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		if(quest == null) return;
		if(args.length < 5) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage insert <StageBefore#> <TriggerType> [TriggerParams...]");
			return;
		}
		Document base = Document.parse(quest.getData().toJson());
		Integer before = parseInt(sender, args[3]);
		TriggerType type = StringUtil.parseEnum(sender, TriggerType.class, args[4]);
		if(before == null || type == null) return;
		QuestStep step = new QuestStep("Unnamed Step", makeTrigger(quest, type, args.length > 5 ? Arrays.copyOfRange(args, 5, args.length) : null), new ArrayList<>(), quest);
		quest.insertStep(before, step);
		sender.sendMessage(ChatColor.GREEN + "Added new quest stage successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Inserted new quest stage");
	}
	
	private void manageStage(CommandSender sender, String[] args) {
		if(args.length <= 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <QuestName> stage <<Stage#> [name|trigger|action] [...]|<add|insert <StageBefore#>>|del <Stage#>> <trigger> [params...]");
		}
		else if(args[2].equalsIgnoreCase("add")) {
			addStage(sender, args);
		}
		else if(args[2].equalsIgnoreCase("insert")) {
			insertStage(sender, args);
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
			else if(args[4].equalsIgnoreCase("insert")) {
				insertAction(sender, args);
			}
			else if(args[4].equalsIgnoreCase("del")) {
				deleteAction(sender, args);
			}
		}
		else if(args[2].equalsIgnoreCase("del")) {
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
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action TELEPORT_PLAYER");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add SPAWN_NPC <NpcClass> [NpcReferenceName [Phased?]]");
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
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add FAIL_QUEST");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add [UN]STASH_ITEMS <MaterialType>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action add SPAWN_RELATIVE <NpcClass> <Phased?> <Quantity> <MaxRadius>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action dialogue add <Action#> <DialogueText>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action branch add <GoToStage#> <TriggerType> <TriggerParam|NONE>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action choice add <Action#> <GoToStage#> <ChoiceText>");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action insert <BeforeAction#> <ACTION_TYPE> [ActionParams...]");
		sender.sendMessage(ChatColor.RED + "/quest <ShortName> stage <Stage#> action del <Action#>");
	}
	
	private void addAction(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		QuestStep step = quest.getSteps().get(stepNo);
		Document base = Document.parse(quest.getData().toJson());
		QuestAction action = makeAction(quest, sender, Arrays.copyOfRange(args, 5, args.length));
		step.addAction(action);
		sender.sendMessage(ChatColor.GREEN + "Added new action to quest stage successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Added action to stage " + args[2]);
	}
	
	private void insertAction(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		Integer actionNo = parseInt(sender, args[5]);
		if(quest == null || stepNo == null || actionNo == null) return;
		QuestStep step = quest.getSteps().get(stepNo);
		QuestAction action = makeAction(quest, sender, Arrays.copyOfRange(args, 6, args.length));
		step.insertAction(action, actionNo);
		Document base = Document.parse(quest.getData().toJson());
		sender.sendMessage(ChatColor.GREEN + "Inserted new action to quest stage successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Inserted action before " + args[5] + " to stage " + args[2]);
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
			AUDIT_LOG.saveEntry(quest, user(sender), base, "Added dialogue to action " + args[6] + " of stage " + args[2]);
		}
		else if(args[5].equalsIgnoreCase("remove")) {
			Document base = Document.parse(quest.getData().toJson());
			boolean success = step.removeDialogue(Integer.valueOf(args[6]), Integer.valueOf(args[7]));
			if(success) {
				sender.sendMessage(ChatColor.GREEN + "Removed dialogue line from quest stage action successfully.");
			}
			else {
				sender.sendMessage(ChatColor.RED + "Could not remove dialogue line from quest stage action");
			}
			AUDIT_LOG.saveEntry(quest, user(sender), base, "Removed dialogue line " + args[7] + " from action " + args[6] + " of stage " + args[2]);
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
			QuestTrigger trigger = makeTrigger(quest, TriggerType.valueOf(args[7]), Arrays.copyOfRange(args, 8, args.length));
			QuestAction action = QuestAction.goToStageAction(quest, Integer.valueOf(args[6]), false);
			step.addBranchPoint(trigger, action);
			sender.sendMessage(ChatColor.GREEN + "Added branch point successfully.");
			AUDIT_LOG.saveEntry(quest, user(sender), base, "Added branch point to stage " + args[2]);
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
			step.addChoice(Integer.valueOf(args[6]), choice, Integer.valueOf(args[7]));
			sender.sendMessage(ChatColor.GREEN + "Added choice to quest stage action successfully.");
			AUDIT_LOG.saveEntry(quest, user(sender), base, "Added quest choice to action " + args[6] + " of stage " + args[2]);
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
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Removed action " + args[4] + " from stage " + args[2]);
	}
	
	private void updateStepName(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		
		step.setStepName(StringUtil.concatArgs(args, 4));
		sender.sendMessage(ChatColor.GREEN + "Updated quest step name successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Set name of stage " + args[2] + " to " + StringUtil.concatArgs(args, 4));
	}
	
	private void updateStepTrigger(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[2]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		QuestStep step = quest.getSteps().get(stepNo);
		
		TriggerType type = StringUtil.parseEnum(sender, TriggerType.class, args[4]);
		List<String> params = new ArrayList<>();
		for(int i = 5; i < args.length; i++) {
			params.add(args[i]);
		}
		step.setTrigger(makeTrigger(quest, type, params.toArray(new String[] {})));
		sender.sendMessage(ChatColor.GREEN + "Updated quest stage trigger successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Set trigger of stage " + args[2] + " to " + type);
	}
	
	private void deleteStep(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[0]);
		Integer stepNo = parseInt(sender, args[3]);
		if(quest == null || stepNo == null) return;
		Document base = Document.parse(quest.getData().toJson());
		
		quest.delStep(stepNo);
		sender.sendMessage(ChatColor.GREEN + "Deleted quest stage successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Deleted step " + args[2]);
	}
	
	private void lockQuest(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[1]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLocked(true);
		sender.sendMessage(ChatColor.GREEN + "Locked quest successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Locked quest");
	}
	
	private void unlockQuest(CommandSender sender, String[] args) {
		Quest quest = lookupQuest(sender, args[1]);
		if(quest == null) return;
		Document base = Document.parse(quest.getData().toJson());
		quest.setLocked(false);
		sender.sendMessage(ChatColor.GREEN + "Unlocked quest successfully.");
		AUDIT_LOG.saveEntry(quest, user(sender), base, "Unlocked quest");
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
	
	private QuestTrigger makeTrigger(Quest quest, TriggerType type, String... params) {
		switch(type) {
		case INSTANT:
			return QuestTrigger.instant(quest);
		case NEVER:
			return QuestTrigger.never(quest);
		case KILL_NPC:
			return QuestTrigger.onKillNPC(quest, npcClassLoader.getNPCClassByClassName(params[0]), Integer.valueOf(params[1]));
		case CLICK_NPC:
			return QuestTrigger.onClickNPC(quest, npcClassLoader.getNPCClassByClassName(params[0]));
		case ENTER_REGION:
			return QuestTrigger.onEnterRegion(quest, regionLoader.getRegionByName(params[0]));
		case WALK_REGION:
			return QuestTrigger.onWalkRegion(quest, regionLoader.getRegionByName(params[0]), Double.valueOf(params[1]));
		case EXIT_REGION:
			return QuestTrigger.onExitRegion(quest, regionLoader.getRegionByName(params[0]));
		case BRANCH_CONDITIONAL:
			return QuestTrigger.branchConditional(quest, new HashMap<>());
		case HAS_ITEM:
			return QuestTrigger.hasItem(quest, itemClassLoader.getItemClassByClassName(params[0]), Integer.valueOf(params[1]));
		case HAS_LESS_ITEM:
			return QuestTrigger.hasLessItem(quest, itemClassLoader.getItemClassByClassName(params[0]), Integer.valueOf(params[1]));
		case HAS_NO_ITEM:
			return QuestTrigger.hasNoItem(quest, itemClassLoader.getItemClassByClassName(params[0]));
		default:
			return null;
		}
	}
	
	private QuestAction makeAction(Quest quest, CommandSender sender, String... args) {
		User user = user(sender);
		if(StringUtil.equalsAnyIgnoreCase(args[0], "TELEPORT_PLAYER", "TeleportPlayer")) {
			if(!requirePlayer(sender)) return null;
			return QuestAction.teleportPlayerAction(quest, user.getPlayer().getLocation());
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "SPAWN_NPC", "SpawnNPC")) {
			return QuestAction.spawnNPCAction(quest, npcClassLoader.getNPCClassByClassName(args[1]), user.getPlayer().getLocation(), args.length <= 2 ? "" : args[2], args.length <= 3 ? false : Boolean.valueOf(args[3]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "TELEPORT_NPC", "TeleportNPC")) {
			if(!requirePlayer(sender)) return null;
			return QuestAction.teleportNPCAction(quest, args[1], user.getPlayer().getLocation());
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "PATHFIND_NPC", "PathfindNPC", "Pathfind", "Path")) {
			if(!requirePlayer(sender)) return null;
			return QuestAction.pathfindNPCAction(quest, args[1], user.getPlayer().getLocation(), args.length <= 2 ? -1 : Integer.valueOf(args[2]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "BEGIN_DIALOGUE", "BeginDialogue", "Dialogue", "Speak", "Talk", "Conversation", "Converse")) {
			return QuestAction.beginDialogueAction(quest, npcClassLoader.getNPCClassByClassName(args[1]), new ArrayList<>());
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "GIVE_XP", "GiveXP", "XP")) {
			return QuestAction.giveXPAction(quest, Integer.valueOf(args[1]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "GOTO_STAGE", "GoToStage", "GoTo", "Jump")) {
			return QuestAction.goToStageAction(quest, Integer.valueOf(args[1]), Boolean.valueOf(args[2]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "TAKE_ITEM", "TakeItem", "Take")) {
			return QuestAction.takeItemAction(quest, itemClassLoader.getItemClassByClassName(args[1]), args.length <= 2 ? 1 : Integer.valueOf(args[2]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "GIVE_ITEM", "GiveItem", "Give")) {
			return QuestAction.giveItemAction(quest, itemClassLoader.getItemClassByClassName(args[1]), args.length <= 2 ? 1 : Integer.valueOf(args[2]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "ADD_POTION_EFFECT", "AddPotionEffect", "AddPotion", "AddEffect")) {
			return QuestAction.addPotionEffectAction(quest, PotionEffectType.getByName(args[1]), Integer.valueOf(args[2]), Integer.valueOf(args[3]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "REMOVE_POTION_EFFECT", "RemovePotionEffect", "RemovePotion", "RemoveEffect")) {
			return QuestAction.removePotionEffectAction(quest, PotionEffectType.getByName(args[1]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "COMPLETION_HEADER", "CompletionHeader")) {
			return QuestAction.completionHeaderAction(quest);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "FAIL_QUEST", "FailQuest", "Fail")) {
			return QuestAction.failQuestAction(quest);
		}
		else if(args[0].equalsIgnoreCase("WAIT")) {
			return QuestAction.waitAction(quest, Integer.valueOf(args[1]));
		}
		else if(args[0].equalsIgnoreCase("CHOICES")) {
			return QuestAction.choicesAction(quest, new LinkedHashMap<>());
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "STASH_ITEMS", "StashItems", "Stash")) {
			return QuestAction.stashItemAction(quest, Material.valueOf(args[1]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "UNSTASH_ITEMS", "UnstashItems", "Unstash")) {
			return QuestAction.unstashItemAction(quest, Material.valueOf(args[1]));
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "SPAWN_RELATIVE", "SpawnRelative")) {
			return QuestAction.spawnRelativeAction(quest, npcClassLoader.getNPCClassByClassName(args[1]), Boolean.valueOf(args[2]), Integer.valueOf(args[3]), Integer.valueOf(args[4]));
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid action type! Valid action types are " + StringUtil.parseList(QuestActionType.values()));
			return null;
		}
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
			return "Target Region: " + ChatColor.GREEN + trigger.getRegion().getName() + " (" + trigger.getRegion().getFlags().getString(Region.FLAG_FULLNAME) + ")";
		case WALK_REGION:
			return "Target Region: " + ChatColor.GREEN + trigger.getRegion().getName() + " (" + trigger.getRegion().getFlags().getString(Region.FLAG_FULLNAME) + "); Distance: " + trigger.getMinDistance();
		case BRANCH_CONDITIONAL:
			String triggerMsg = "Branch Points:";
			for(Entry<QuestTrigger, QuestAction> conditional : trigger.getBranchPoints().entrySet()) {
				triggerMsg += "\n    - " + conditional.getKey().getTriggerType().toString() + " (" + displayTrigger(conditional.getKey()) + ChatColor.GRAY + ")" 
						+ " -> " + conditional.getValue().getActionType().toString() + " (" + displayAction(conditional.getValue()) + ChatColor.GRAY + ")";
			}
			return triggerMsg;
		case HAS_ITEM:
			return "Triggered immediately once player has " + ChatColor.GREEN + trigger.getQuantity() + ChatColor.GRAY + " of " + ChatColor.GREEN + trigger.getItemClass().getClassName();
		case HAS_LESS_ITEM:
			return "Triggered immediately once player has less than " + ChatColor.GREEN + trigger.getQuantity() + ChatColor.GRAY + " of " + ChatColor.GREEN + trigger.getItemClass().getClassName();
		case HAS_NO_ITEM:
			return "Triggered immediately once player has none of " + ChatColor.GREEN + trigger.getItemClass().getClassName();
		default:
			return "";
		}
	}
	
	private String displayAction(QuestAction action) {
		switch(action.getActionType()) {
		case TELEPORT_PLAYER:
			return "Target Location: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation());
		case SPAWN_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
					+ ChatColor.GRAY + "    - Location: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation()) + "\n"
					+ ChatColor.GRAY + (action.getNPCReferenceName() == null ? "    - No reference name" : "    - Reference name: " + ChatColor.GREEN + action.getNPCReferenceName()) + "\n"
					+ ChatColor.GRAY + "    - " + (action.isPhased() ? "Phased for player" : "Not phased");
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
		case FAIL_QUEST:
			return ChatColor.GRAY + "(No data)";
		case WAIT:
			return ChatColor.GRAY + "Time: " + ChatColor.GREEN + action.getWaitTime() + "s";
		case CHOICES:
			String choiceMsg = "Choices:\n";
			for(Entry<String, Integer> choice : action.getChoices().entrySet()) {
				choiceMsg += "      - " + choice.getKey() + ChatColor.YELLOW + " -> " + ChatColor.GRAY + "Step " + choice.getValue() + "\n";
			}
			return choiceMsg;
		case STASH_ITEMS:
		case UNSTASH_ITEMS:
			return ChatColor.GRAY + "Material Type: " + ChatColor.GREEN + action.getMaterialType();
		case SPAWN_RELATIVE:
			return ChatColor.GRAY + "Target NPC Class: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
					+ ChatColor.GRAY + "    - Quantity: " + ChatColor.GREEN + action.getQuantity() + "\n"
					+ ChatColor.GRAY + "    - Radius: " + ChatColor.GREEN + action.getRadius() + "\n"
					+ ChatColor.GRAY + "    - " + (action.isPhased() ? "Phased for player" : "Not phased");
		}
		return "";
	}
}
