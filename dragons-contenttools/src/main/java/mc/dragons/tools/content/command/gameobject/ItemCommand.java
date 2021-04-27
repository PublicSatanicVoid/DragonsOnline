package mc.dragons.tools.content.command.gameobject;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.Addon;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.gameobject.user.permission.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

public class ItemCommand extends DragonsCommandExecutor {
	private GameObjectRegistry registry = dragons.getGameObjectRegistry();;
	private StorageManager storageManager = dragons.getPersistentStorageManager();;
	private AddonRegistry addonRegistry = dragons.getAddonRegistry();;
	
	private void showHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.YELLOW + "/item create <ClassName> <MaterialType> <LvMin> <Cooldown> <Damage> <Armor>" 
				+ ChatColor.GRAY + " create a new item class");
		sender.sendMessage(ChatColor.YELLOW + "/item list [startingWith]" + ChatColor.GRAY + " list all item classes, optionally starting with the specified text");
		sender.sendMessage(ChatColor.YELLOW + "/item delete <ClassName>" + ChatColor.GRAY + " delete item class");
		sender.sendMessage(ChatColor.YELLOW + "/item give <ClassName> [Player] [Quantity]" + ChatColor.GRAY + " give an item of the specified class");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName>" + ChatColor.GRAY + " show information about an item class");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> name <DisplayName>" + ChatColor.GRAY + " set item class display name");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> namecolor <Color>" + ChatColor.GRAY + " set item class display name color");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> lore <add <Lore>|remove <LineNo>]>" + ChatColor.GRAY + " view/edit item class lore");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> <type|lvmin|cooldown|unbreakable|undroppable|damage|armor|speedboost|stacksize> <Value>" + ChatColor.GRAY + " edit item class data");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> addon [add <AddonName>]" + ChatColor.GRAY + " manage addons to this item class.");
		sender.sendMessage(ChatColor.YELLOW + "/item <ClassName> push" + ChatColor.GRAY + " update all items of this class with updated stats (will revert custom changes made to these items)");
		sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Class names must not contain spaces.");
		sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
	}
	
	private void createClass(CommandSender sender, String[] args) {
		if(args.length < 7) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item create <ClassName> <MaterialType> <LvMin> <Cooldown> <Damage> <Armor>");
			return;
		}
		
		Material type = StringUtil.parseMaterialType(sender, args[2]);
		if(type == null) return;
		Integer lvMin = parseInt(sender, args[3]);
		Double cooldown = parseDouble(sender, args[4]);
		Double damage = parseDouble(sender, args[5]);
		Double armor = parseDouble(sender, args[6]);
		if(lvMin == null || cooldown == null || damage == null || armor == null) return;
		
		ItemClass itemClass = itemClassLoader.registerNew(args[1], "Unnamed Item", ChatColor.GOLD, type, lvMin, cooldown, 0.0, false, false, damage, armor, new ArrayList<>(), 64);
		if(itemClass == null) {
			sender.sendMessage(ChatColor.RED + "An error occurred! Does an item class by this name already exist?");
			return;
		}
		
		MetadataConstants.addBlankMetadata(itemClass, user(sender));
		sender.sendMessage(ChatColor.GREEN + "Successfully created item class " + args[1]);
	}
	
	private void listClasses(CommandSender sender, String[] args) {
		String startingWith = "";
		if(args.length > 1) {
			startingWith = args[1];
		}
		sender.sendMessage(ChatColor.GREEN + "Listing all item classes" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.ITEM_CLASS)) {
			ItemClass itemClass = (ItemClass) gameObject;
			if(!itemClass.getClassName().startsWith(startingWith)) continue;
			sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "- " + itemClass.getClassName() + " [Lv Min " + itemClass.getLevelMin() + "]",
					"/item give " + itemClass.getClassName(), true, "Click to receive this item"));
		}
	}
	
	private void deleteClass(CommandSender sender, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_DELETE)) return;
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item delete <ClassName>");
			return;
		}

		ItemClass itemClass = lookupItemClass(sender, args[1]);
		if(itemClass == null) return;
		
		registry.removeFromDatabase(itemClass);
		sender.sendMessage(ChatColor.GREEN + "Deleted item class successfully.");
	}
	
	private void giveItemOfClass(CommandSender sender, String[] args) {
		if(args.length == 1) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item give <ClassName> [Player] [Quantity]");
			return;
		}

		ItemClass itemClass = lookupItemClass(sender, args[1]);
		if(itemClass == null) return;
		
		Item item = itemLoader.registerNew(itemClass);
		if(args.length > 3) {
			Integer quantityOpt = parseInt(sender, args[3]);
			if(quantityOpt == null) return;
			item.setQuantity(quantityOpt);
		}
		
		if(args.length > 2) {
			Player target = Bukkit.getPlayerExact(args[2]);
			if(target == null) {
				sender.sendMessage(ChatColor.RED + "That player is not online!");
				return;
			}
			User targetUser = UserLoader.fromPlayer(target);
			targetUser.giveItem(item);
		}
		else if(!isPlayer(sender)) {
			sender.sendMessage(ChatColor.RED + "Console must specify a player to give the item to!");
		}
		else {
			user(sender).giveItem(item);		
		}
	}
	
	private void displayItemClass(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		sender.sendMessage(ChatColor.GREEN + "=== Item Class: " + itemClass.getClassName() + " ===");
		sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + itemClass.getIdentifier().toString());
		sender.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GREEN + itemClass.getDecoratedName());
		sender.sendMessage(ChatColor.GRAY + "Level min: " + ChatColor.GREEN + itemClass.getLevelMin());
		sender.sendMessage(ChatColor.GRAY + "Material type: " + ChatColor.GREEN + itemClass.getMaterial().toString());
		sender.sendMessage(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + itemClass.getDamage() + ChatColor.GRAY + " - Armor: " + ChatColor.GREEN + itemClass.getArmor());
		sender.sendMessage(ChatColor.GRAY + "Cooldown: " + ChatColor.GREEN + itemClass.getCooldown());
		sender.sendMessage(ChatColor.GRAY + "Walk Speed Boost: " + ChatColor.GREEN + itemClass.getSpeedBoost());
		sender.sendMessage(ChatColor.GRAY + "Unbreakable: " + ChatColor.GREEN + itemClass.isUnbreakable());
		sender.sendMessage(ChatColor.GRAY + "Undroppable: " + ChatColor.GREEN + itemClass.isUndroppable());
		sender.sendMessage(ChatColor.GRAY + "GM Locked: " + ChatColor.GREEN + itemClass.isGMLocked());
		sender.sendMessage(ChatColor.GRAY + "Max. Stack Size: " + ChatColor.GREEN + itemClass.getMaxStackSize());
		sender.sendMessage(ChatColor.GRAY + "Lore:");
		for(String loreLine : itemClass.getLore()) {
			sender.sendMessage(ChatColor.GREEN + " " + loreLine);
		}
		sender.spigot().sendMessage(StringUtil.clickableHoverableText(ChatColor.GRAY + "[Receive Item] ", "/item give " + itemClass.getClassName(), true, "Click to receive this item"),
				ObjectMetadataCommand.getClickableMetadataLink(GameObjectType.ITEM_CLASS, itemClass.getUUID()));
	}
	
	private void push(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		Document update = new Document(itemClass.getData());
		update.remove("_id");
		update.remove("type");
		storageManager.push(GameObjectType.ITEM, new Document("className", itemClass.getClassName()), update);
		sender.sendMessage(ChatColor.GREEN + "Updated all items matching class " + itemClass.getClassName() + " in database.");
		sender.sendMessage(ChatColor.GREEN + "Players must rejoin to receive the updated items.");
		MetadataConstants.logPush(itemClass);
	}
	
	private void manageAddons(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		User user = user(sender);
		
		if(args.length == 2) {
			sender.sendMessage(ChatColor.GREEN + "Listing addons for item class " + itemClass.getClassName() + ":");
			for(ItemAddon addon : itemClass.getAddons()) {
				sender.sendMessage(ChatColor.GRAY + "- " + addon.getName());
			}
		}
		else if(args.length == 3) {
			sender.sendMessage(ChatColor.RED + "Specify an addon name! For a list of addons, do /addon -l");
		}
		else {
			Addon addon = addonRegistry.getAddonByName(args[3]);
			if(addon == null) {
				sender.sendMessage(ChatColor.RED + "Invalid addon name! For a list of addons, do /addon -l");
			}
			else if(!(addon instanceof ItemAddon)) {
				sender.sendMessage(ChatColor.RED + "Invalid addon type! Only Item Addons can be applied to items.");
			}
			else if(args[2].equalsIgnoreCase("add")) {
				Document base = Document.parse(itemClass.getData().toJson());
				itemClass.addAddon((ItemAddon) addon);
				sender.sendMessage(ChatColor.GREEN + "Added addon " + addon.getName() + " to item class " + itemClass.getClassName() + ".");
				MetadataConstants.logRevision(itemClass, user, base, "Added addon " + addon.getName());
			}
			else if(args[2].equalsIgnoreCase("remove")) {
				Document base = Document.parse(itemClass.getData().toJson());
				itemClass.removeAddon((ItemAddon) addon);
				sender.sendMessage(ChatColor.GREEN + "Removed addon " + addon.getName() + " from item class " + itemClass.getClassName() + ".");
				MetadataConstants.logRevision(itemClass, user, base, "Removed addon " + addon.getName());
			}
			else {
				sender.sendMessage(ChatColor.RED + "Invalid arguments! /item <ClassName> addon [<add|remove> <AddonName>]");
			}
		}
	}
	
	private void setName(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;

		Document base = Document.parse(itemClass.getData().toJson());
		String name = StringUtil.concatArgs(args, 2);
		String displayName = ChatColor.translateAlternateColorCodes('&', name);
		itemClass.setName(displayName);
		sender.sendMessage(ChatColor.GREEN + "Updated item display name successfully.");
		MetadataConstants.logRevision(itemClass, user(sender), base, "Set display name to " + displayName);
	}
	
	private void setNameColor(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		ChatColor nameColor = StringUtil.parseChatColor(sender, args[2]);
		if(nameColor == null) return;

		Document base = Document.parse(itemClass.getData().toJson());
		itemClass.setNameColor(nameColor);
		sender.sendMessage(ChatColor.GREEN + "Updated item display name color successfully.");
		MetadataConstants.logRevision(itemClass, user(sender), base, "Set display name color to " + nameColor);
	}
	
	private void manageLore(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		User user = user(sender);

		if(args.length == 2) {
			if(itemClass.getLore().size() == 0) {
				sender.sendMessage(ChatColor.RED + "This item has no lore!");
			}
			else {
				sender.sendMessage(ChatColor.GREEN + "Item lore:");
				for(String line : itemClass.getLore()) {
					sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + line);
				}
			}
		}
		else if(args[2].equalsIgnoreCase("add")) {
			Document base = Document.parse(itemClass.getData().toJson());
			String loreLine = StringUtil.concatArgs(args, 3);
			List<String> lore = itemClass.getLore();
			lore.add(loreLine);
			itemClass.setLore(lore);
			sender.sendMessage(ChatColor.GREEN + "Updated item lore successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Added lore line " + loreLine);
		}
		else if(args[2].equalsIgnoreCase("remove")) {
			Document base = Document.parse(itemClass.getData().toJson());
			List<String> lore = itemClass.getLore();
			lore.remove(Integer.valueOf(args[3]) - 1);
			itemClass.setLore(lore);
			sender.sendMessage(ChatColor.GREEN + "Updated item lore successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Removed lore line #" + args[3]);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /item <ClassName> lore [add <Lore>|remove <LineNo>]");
		}	
	}
	
	private void setAttribute(CommandSender sender, String[] args) {
		ItemClass itemClass = lookupItemClass(sender, args[0]);
		if(itemClass == null) return;
		
		User user = user(sender);

		if(args.length == 2) {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /item <ClassName> <Attribute> <Value>");
		}
		else if(args[1].equalsIgnoreCase("type")) {
			Material type = StringUtil.parseMaterialType(sender, args[2]);
			if(type == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setMaterial(type);
			sender.sendMessage(ChatColor.GREEN + "Updated item type successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item type to " + type);
		}
		else if(args[1].equalsIgnoreCase("lvmin")) {
			Integer lvMin = parseInt(sender, args[2]);
			if(lvMin == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setLevelMin(lvMin);
			sender.sendMessage(ChatColor.GREEN + "Updated level min successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item level min to " + lvMin);
		}
		else if(args[1].equalsIgnoreCase("cooldown")) {
			Double cooldown = parseDouble(sender, args[2]);
			if(cooldown == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setCooldown(cooldown);
			sender.sendMessage(ChatColor.GREEN + "Updated cooldown successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item cooldown to " + cooldown);
		}
		else if(args[1].equalsIgnoreCase("unbreakable")) {
			Boolean unbreakable = parseBoolean(sender, args[2]);
			if(unbreakable == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setUnbreakable(unbreakable);
			sender.sendMessage(ChatColor.GREEN + "Updated unbreakable status successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item unbreakability to " + unbreakable);
		}
		else if(args[1].equalsIgnoreCase("undroppable")) {
			Boolean undroppable = parseBoolean(sender, args[2]);
			if(undroppable == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setUndroppable(undroppable);
			sender.sendMessage(ChatColor.GREEN + "Updated undroppable status successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item undroppability to " + undroppable);
		}
		else if(args[1].equalsIgnoreCase("gmlock")) {
			Boolean gmLock = parseBoolean(sender, args[2]);
			if(gmLock == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setGMLocked(gmLock);
			sender.sendMessage(ChatColor.GREEN + "Updated GM Lock status successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item GM lock to " + gmLock);
		}
		else if(args[1].equalsIgnoreCase("damage")) {
			Double damage = parseDouble(sender, args[2]);
			if(damage == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setDamage(damage);
			sender.sendMessage(ChatColor.GREEN + "Updated damage successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item damage to " + damage);
		}
		else if(args[1].equalsIgnoreCase("speedboost")) {
			Double speedBoost = parseDouble(sender, args[2]);
			if(speedBoost == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setSpeedBoost(speedBoost);
			sender.sendMessage(ChatColor.GREEN + "Updated speed boost successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item speed boost to " + speedBoost);
		}
		else if(args[1].equalsIgnoreCase("armor")) {
			Double armor = parseDouble(sender, args[2]);
			if(armor == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setArmor(armor);
			sender.sendMessage(ChatColor.GREEN + "Updated armor successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item armor to " + armor);
		}
		else if(args[1].equalsIgnoreCase("stacksize")) {
			Integer stackSize = parseInt(sender, args[2]);
			if(stackSize == null) return;

			Document base = Document.parse(itemClass.getData().toJson());
			itemClass.setMaxStackSize(stackSize);
			sender.sendMessage(ChatColor.GREEN + "Updated max. stack size successfully.");
			MetadataConstants.logRevision(itemClass, user, base, "Set item max stack size to " + stackSize);
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid attribute! For usage info, do /item");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, SystemProfileFlag.GM_ITEM)) return true;
		
		if(args.length == 0) {
			showHelp(sender);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-c", "c", "create")) {
			createClass(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-l", "l", "list")) {
			listClasses(sender, args);
		}
		else if(StringUtil.equalsAnyIgnoreCase(args[0], "-d", "d", "del", "delete")) {
			deleteClass(sender, args);
		}
		else if(args[0].equalsIgnoreCase("give")) {
			giveItemOfClass(sender, args);
		}
		else if(args.length == 1) {
			displayItemClass(sender, args);
		}
		else if(args[1].equalsIgnoreCase("name")) {
			setName(sender, args);
		}
		else if(args[1].equalsIgnoreCase("namecolor")) {
			setNameColor(sender, args);
		}
		else if(args[1].equalsIgnoreCase("lore")) {
			manageLore(sender, args);
		}
		else if(args[1].equalsIgnoreCase("addon")) {
			manageAddons(sender, args);
		}
		else if(args[1].equalsIgnoreCase("push")) {
			push(sender, args);
		}
		else {
			setAttribute(sender, args);
		}
		
		return true;
	}
	
	
}
