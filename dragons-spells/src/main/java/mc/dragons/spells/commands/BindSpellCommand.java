package mc.dragons.spells.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.commands.DragonsCommandExecutor;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.permission.PermissionLevel;
import mc.dragons.spells.DragonsSpells;
import mc.dragons.spells.spells.Spell;
import mc.dragons.spells.spells.SpellRegistry;

public class BindSpellCommand extends DragonsCommandExecutor {
	private SpellRegistry registry;
	
	public BindSpellCommand(DragonsSpells instance) {
		registry = instance.getSpellRegistry();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!requirePlayer(sender) || !requirePermission(sender, PermissionLevel.GM)) return true;
		Player player = player(sender);
		
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		if(itemStack == null) {
			sender.sendMessage(ChatColor.RED + "Item stack is null");
			return true;
		}
		Item item = ItemLoader.fromBukkit(itemStack);
		if(item == null) {
			sender.sendMessage(ChatColor.RED + "Item stack is not an RPG item");
			return true;
		}
		Spell spell = registry.getSpellByName(args[0]);
		if(spell == null) {
			sender.sendMessage(ChatColor.RED + "Invalid spell name");
			return true;
		}
		sender.sendMessage("Bind status before: " + spell.bindStatus(item));
		spell.bind(item);
		sender.sendMessage("Bind status after: " + spell.bindStatus(item));
		player.getInventory().setItemInMainHand(item.getItemStack()); // since we updated the lore
		
		return true;
	}

}
