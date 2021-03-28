package mc.dragons.tools.content.command.statistics;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;

public class RestatCommand extends DragonsCommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePermission(sender, PermissionLevel.GM) || !requirePlayer(sender)) return true;
		
		User user = user(sender);
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "/restat armor <ArmorValue>");
			sender.sendMessage(ChatColor.RED + "/restat cooldown <CooldownSeconds>");
			sender.sendMessage(ChatColor.RED + "/restat damage <DamageValue>");
			sender.sendMessage(ChatColor.RED + "/restat lvmin <LevelMin>");
			sender.sendMessage(ChatColor.RED + "/restat stacksize <MaxStackSize>");
			sender.sendMessage(ChatColor.RED + "/restat speedboost <SpeedBoost>");
			sender.sendMessage(ChatColor.RED + "/restat unbreakable <Unbreakable>");
			sender.sendMessage(ChatColor.RED + "/restat undroppable <Undroppable>");
			return true;
		}
		
		Item heldItem = ItemLoader.fromBukkit(user.getPlayer().getInventory().getItemInMainHand());
		
		if(heldItem == null) {
			sender.sendMessage(ChatColor.RED + "You must hold the item you want to restat!");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("armor")) {
			heldItem.setArmor(Double.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item armor value to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("cooldown")) {
			heldItem.setCooldown(Double.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item cooldown to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("damage")) {
			heldItem.setDamage(Double.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item damage value to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("lvmin")) {
			heldItem.setLevelMin(Integer.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item level min to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("stacksize")) {
			heldItem.setMaxStackSize(Integer.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item max stack size to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("speedboost")) {
			heldItem.setSpeedBoost(Double.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item speed boost to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("unbreakable")) {
			heldItem.setUnbreakable(Boolean.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item unbreakability flag to " + args[1]);
		}
		
		else if(args[0].equalsIgnoreCase("undroppable")) {
			heldItem.setUndroppable(Boolean.valueOf(args[1]));
			sender.sendMessage(ChatColor.GREEN + "Updated item undroppability flag to " + args[1]);
		}
		
		else {
			sender.sendMessage(ChatColor.RED + "Invalid arguments! /restat");
			return true;
		}

		heldItem.setCustom(true);
		heldItem.updateItemStackData();
		user.getPlayer().getInventory().setItemInMainHand(heldItem.getItemStack());
		return true;
		
	}

}
