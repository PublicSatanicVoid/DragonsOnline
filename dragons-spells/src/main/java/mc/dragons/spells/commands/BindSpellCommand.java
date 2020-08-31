package mc.dragons.spells.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.UserLoader;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.spells.SpellCastAddon;
import mc.dragons.spells.spells.Spell;

public class BindSpellCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		
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
		Spell spell = SpellCastAddon.getSpellRegistry().getSpellByName(args[0]);
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
