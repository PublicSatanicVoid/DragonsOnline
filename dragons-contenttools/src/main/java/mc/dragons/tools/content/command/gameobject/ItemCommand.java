package mc.dragons.tools.content.command.gameobject;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.addon.Addon;
import mc.dragons.core.addon.AddonRegistry;
import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.item.ItemClassLoader;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.SystemProfile.SystemProfileFlags.SystemProfileFlag;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;
import mc.dragons.tools.content.util.MetadataConstants;

public class ItemCommand implements CommandExecutor {
	//private UserLoader userLoader;
	private ItemLoader itemLoader;
	private ItemClassLoader itemClassLoader;
	private GameObjectRegistry registry;
	private StorageManager storageManager;
	private AddonRegistry addonRegistry;
	
	public ItemCommand(Dragons instance) {
		//userLoader = GameObjectType.USER.<User, UserLoader>getLoader();
		itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();
		itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();
		registry = instance.getGameObjectRegistry();
		storageManager = instance.getPersistentStorageManager();
		addonRegistry = instance.getAddonRegistry();
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			//if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
			if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_ITEM, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/item class -c <ItemClass> <MaterialType> <LvMin> <Cooldown> <Unbreakable?> <Damage> <Armor>" 
				+ ChatColor.GRAY + " create a new item class");
			sender.sendMessage(ChatColor.YELLOW + "/item class -l [startingWith]" + ChatColor.GRAY + " list all item classes, optionally starting with the specified text");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass>" + ChatColor.GRAY + " show information about an item class");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> name <DisplayName>" + ChatColor.GRAY + " set item class display name");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> namecolor <Color>" + ChatColor.GRAY + " set item class display name color");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> lore <add <Lore>|remove <LineNo>]>" + ChatColor.GRAY + " view/edit item class lore");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> type|lvmin|cooldown|unbreakable|undroppable|damage|armor|speedboost|stacksize <Value>" + ChatColor.GRAY + " edit item class data");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> addon [add <AddonName>]" + ChatColor.GRAY + " manage addons to this item class.");
			sender.sendMessage(ChatColor.YELLOW + "/item class -s <ItemClass> push" + ChatColor.GRAY + " update all items of this class with updated stats (will revert custom changes made to these items)");
			sender.sendMessage(ChatColor.YELLOW + "/item class -d <ItemClass>" + ChatColor.GRAY + " delete item class");
			sender.sendMessage(ChatColor.YELLOW + "/item give <ItemClass> [Player] [Quantity]" + ChatColor.GRAY + " give an item of the specified class");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Class names must not contain spaces.");
			sender.sendMessage(ChatColor.GRAY + "View the full documentation at " + ChatColor.UNDERLINE + Dragons.STAFF_DOCUMENTATION);
			return true;
		}
		
		if(args[0].equalsIgnoreCase("class") || args[0].equalsIgnoreCase("c")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /item");
				return true;
			}
			
			if(args[1].equalsIgnoreCase("-c")) {
				if(args.length < 9) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item class -c <ItemClass> <MaterialType> <LvMin> <Cooldown> <IsUnbreakable> <Damage> <Armor>");
					return true;
				}
				
				Material type = StringUtil.parseMaterialType(sender, args[3]);
				if(type == null) return true;
				int lvMin = Integer.valueOf(args[4]);
				double cooldown = Double.valueOf(args[5]);
				boolean unbreakable = false;
				try {
					unbreakable = Boolean.valueOf(args[6]);
				}
				catch(Exception e) {
					sender.sendMessage(ChatColor.RED + "Unbreakability must be either true (unbreakable) or false (breakable)");
					return true;
				}
				double damage = Double.valueOf(args[7]);
				double armor = Double.valueOf(args[8]);
				ItemClass itemClass = itemClassLoader.registerNew(args[2], "Unnamed Item", ChatColor.YELLOW, type, lvMin, cooldown, 0.0, unbreakable, false, damage, armor, new ArrayList<>(), 64);
				if(itemClass == null) {
					sender.sendMessage(ChatColor.RED + "An error occurred! Does a class by this name already exist?");
					return true;
				}
				MetadataConstants.addBlankMetadata(itemClass, user);
				sender.sendMessage(ChatColor.GREEN + "Successfully created item class " + args[2]);
				return true;
			}
			
			if(args[1].equalsIgnoreCase("-l")) {
				String startingWith = "";
				if(args.length > 2) {
					startingWith = args[2];
				}
				sender.sendMessage(ChatColor.GREEN + "Listing all item classes" + (startingWith.length() > 0 ? (" starting with \"" + startingWith + "\"") : "") + ":");
				for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.ITEM_CLASS)) {
					ItemClass itemClass = (ItemClass) gameObject;
					if(!itemClass.getClassName().startsWith(startingWith)) continue;
					sender.sendMessage(ChatColor.GRAY + "- " + itemClass.getClassName() + " [Lv Min " + itemClass.getLevelMin() + "]");
				}
				return true;
			}
			
			if(args[1].equalsIgnoreCase("-s")) {
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! For usage info, do /item");
					return true;
				}

				ItemClass itemClass = itemClassLoader.getItemClassByClassName(args[2]);
				
				if(itemClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid item class name! To list all item classes, do /item class -l");
					return true;
				}
				
				if(args.length == 3) {
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
					MetadataConstants.displayMetadata(sender, itemClass);
					return true;
				}

				if(args[3].equalsIgnoreCase("push")) {
					Document update = new Document(itemClass.getData());
					update.remove("_id");
					update.remove("type");
					storageManager.push(GameObjectType.ITEM, new Document("className", itemClass.getClassName()), update);
					sender.sendMessage(ChatColor.GREEN + "Updated all items matching class " + itemClass.getClassName() + " in database.");
					sender.sendMessage(ChatColor.GREEN + "Players must rejoin to receive the updated items.");
					return true;
				}
				
				if(args[3].equalsIgnoreCase("addon") || args[3].equalsIgnoreCase("addons") || args[3].equalsIgnoreCase("a")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "Listing addons for item class " + itemClass.getClassName() + ":");
						for(ItemAddon addon : itemClass.getAddons()) {
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
					if(!(addon instanceof ItemAddon)) {
						sender.sendMessage(ChatColor.RED + "Invalid addon type! Only Item Addons can be applied to items.");
						return true;
					}
					if(args[4].equalsIgnoreCase("add")) {
						itemClass.addAddon((ItemAddon) addon);
						sender.sendMessage(ChatColor.GREEN + "Added addon " + addon.getName() + " to item class " + itemClass.getClassName() + ".");
						MetadataConstants.incrementRevisionCount(itemClass, user);
						return true;
					}
					if(args[4].equalsIgnoreCase("remove")) {
						itemClass.removeAddon((ItemAddon) addon);
						sender.sendMessage(ChatColor.GREEN + "Removed addon " + addon.getName() + " from item class " + itemClass.getClassName() + ".");
						MetadataConstants.incrementRevisionCount(itemClass, user);
						return true;
					}
					sender.sendMessage(ChatColor.RED + "Invalid arguments! /item class -s <ClassName> addon [<add|remove> <AddonName>]");
					return true;
				}
				
				if(args.length == 4) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item class -s <ClassName> <Attribute> <Value|Arguments...>");
					return true;
				}
				
				if(args[3].equalsIgnoreCase("name")) {
					String name = StringUtil.concatArgs(args, 4);
					itemClass.setName(ChatColor.translateAlternateColorCodes('&', name));
					sender.sendMessage(ChatColor.GREEN + "Updated item display name successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("namecolor")) {
					ChatColor nameColor = ChatColor.valueOf(args[4]);
					itemClass.setNameColor(nameColor);
					sender.sendMessage(ChatColor.GREEN + "Updated item display name color successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("lore")) {
					if(args[4].equalsIgnoreCase("add")) {
						String loreLine = StringUtil.concatArgs(args, 5);
						List<String> lore = itemClass.getLore();
						lore.add(loreLine);
						itemClass.setLore(lore);
						sender.sendMessage(ChatColor.GREEN + "Updated item lore successfully.");
						MetadataConstants.incrementRevisionCount(itemClass, user);
						return true;
					}
					if(args[4].equalsIgnoreCase("remove")) {
						List<String> lore = itemClass.getLore();
						lore.remove(Integer.valueOf(args[5]) - 1);
						itemClass.setLore(lore);
						sender.sendMessage(ChatColor.GREEN + "Updated item lore successfully.");
						MetadataConstants.incrementRevisionCount(itemClass, user);
						return true;
					}
				}
				
				if(args[3].equalsIgnoreCase("type")) {
					Material type = Material.valueOf(args[4]);
					itemClass.setMaterial(type);
					sender.sendMessage(ChatColor.GREEN + "Updated item type successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("lvmin")) {
					int lvMin = Integer.valueOf(args[4]);
					itemClass.setLevelMin(lvMin);
					sender.sendMessage(ChatColor.GREEN + "Updated level min successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("cooldown")) {
					double cooldown = Double.valueOf(args[4]);
					itemClass.setCooldown(cooldown);
					sender.sendMessage(ChatColor.GREEN + "Updated cooldown successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("unbreakable")) {
					boolean unbreakable = Boolean.valueOf(args[4]);
					itemClass.setUnbreakable(unbreakable);
					sender.sendMessage(ChatColor.GREEN + "Updated unbreakable status successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("undroppable")) {
					boolean undroppable = Boolean.valueOf(args[4]);
					itemClass.setUndroppable(undroppable);
					sender.sendMessage(ChatColor.GREEN + "Updated undroppable status successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("gmlock")) {
					boolean gmlock = Boolean.valueOf(args[4]);
					itemClass.setGMLocked(gmlock);
					sender.sendMessage(ChatColor.GREEN + "Updated GM Lock status successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("damage")) {
					double damage = Double.valueOf(args[4]);
					itemClass.setDamage(damage);
					sender.sendMessage(ChatColor.GREEN + "Updated damage successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("speedboost")) {
					double speedboost = Double.valueOf(args[4]);
					itemClass.setSpeedBoost(speedboost);
					sender.sendMessage(ChatColor.GREEN + "Updated speed boost successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("armor")) {
					double armor = Double.valueOf(args[4]);
					itemClass.setArmor(armor);
					sender.sendMessage(ChatColor.GREEN + "Updated armor successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				if(args[3].equalsIgnoreCase("stacksize")) {
					int stackSize = Integer.valueOf(args[4]);
					itemClass.setMaxStackSize(stackSize);
					sender.sendMessage(ChatColor.GREEN + "Updated max. stack size successfully.");
					MetadataConstants.incrementRevisionCount(itemClass, user);
					return true;
				}
				
				sender.sendMessage(ChatColor.RED + "Invalid attribute! For usage info, do /item");
				return true;
			}
			
			if(args[1].equalsIgnoreCase("-d")) {
				if(!PermissionUtil.verifyActiveProfileFlag(user, SystemProfileFlag.GM_DELETE, true)) return true;
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item class -d <ClassName>");
					return true;
				}

				ItemClass itemClass = itemClassLoader.getItemClassByClassName(args[2]);
				
				if(itemClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid item class name!");
					return true;
				}
				
				registry.removeFromDatabase(itemClass);
				sender.sendMessage(ChatColor.GREEN + "Deleted item class successfully.");
				return true;
			}
			
			return true;
		}
		
		if(args[0].equalsIgnoreCase("give")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! /item give <ClassName> [Player] [Quantity]");
				return true;
			}

			ItemClass itemClass = itemClassLoader.getItemClassByClassName(args[1]);
			
			if(itemClass == null) {
				sender.sendMessage(ChatColor.RED + "That's not a valid item class name!");
				return true;
			}
			
			Item item = itemLoader.registerNew(itemClass);
			if(args.length > 3) {
				item.setQuantity(Integer.valueOf(args[3]));
			}
			
			if(args.length > 2) {
				Player target = Bukkit.getPlayerExact(args[2]);
				if(target == null) {
					sender.sendMessage(ChatColor.RED + "That player is not online!");
					return true;
				}
				User targetUser = UserLoader.fromPlayer(target);
				targetUser.giveItem(item);
			}
			else {
				user.giveItem(item);		
			}
			return true;
		}
		
		sender.sendMessage(ChatColor.RED + "Invalid arguments! For usage info, do /item");
		return true;
	}
	
	
}
