package mc.dragons.core.commands;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.ItemLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ILostTheLousyStickCommand implements CommandExecutor {
	private ItemLoader itemLoader = GameObjectType.ITEM.<Item, ItemLoader>getLoader();

	private ItemClassLoader itemClassLoader = GameObjectType.ITEM_CLASS.<ItemClass, ItemClassLoader>getLoader();

	private ItemClass lousyStickClass = this.itemClassLoader.getItemClassByClassName("LousyStick");

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		byte b;
		int i;
		ItemStack[] arrayOfItemStack;
		for (i = (arrayOfItemStack = player.getInventory().getContents()).length, b = 0; b < i;) {
			ItemStack itemStack = arrayOfItemStack[b];
			if (itemStack != null) {
				Item item = ItemLoader.fromBukkit(itemStack);
				if (item != null && item.getClassName() != null && item.getClassName().equals("LousyStick")) {
					sender.sendMessage("No you didn't");
					return true;
				}
			}
			b++;
		}
		user.giveItem(this.itemLoader.registerNew(this.lousyStickClass));
		sender.sendMessage("Be more careful next time...");
		return true;
	}
}
