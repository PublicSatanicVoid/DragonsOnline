package mc.dragons.spells.util;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.spells.SpellCastAddon;
import mc.dragons.spells.SpellConstants;
import mc.dragons.spells.spells.Spell;
import mc.dragons.spells.spells.SpellRegistry;
import net.md_5.bungee.api.ChatColor;

public class SpellUtil {
	public static boolean sameItem(ItemStack a, ItemStack b) {
		Item ia = ItemLoader.fromBukkit(a);
		Item ib = ItemLoader.fromBukkit(b);
		if(ia == null || ib == null) return false;
		return ia.getUUID().equals(ib.getUUID());
	}
	
	public static int getSlotOfItemStack(Player player, ItemStack itemStack) {
		ItemStack[] contents = player.getInventory().getContents();
		for(int i = 0; i < contents.length; i++) {
			if(contents[i] != null && sameItem(contents[i], itemStack)) {
				return i;
			}
		}
		return -1;
	}
	
	public static void updateAllSpellItems(SpellRegistry registry, Player player) {
		int i = -1;
		for(ItemStack itemStack : player.getInventory().getContents()) {
			i++;
			if(itemStack == null) continue;
			Item item = ItemLoader.fromBukkit(itemStack);
			if(item == null || !item.getItemClass().getAddons().stream().map(a -> a.getClass()).collect(Collectors.toList()).contains(SpellCastAddon.class)) continue;
			updateItemData(registry, player, i, item);
		}
		player.updateInventory();
	}
	
	public static void updateItemData(SpellRegistry registry, Player player, int slot, Item item) {
		ItemMeta meta = item.getItemStack().getItemMeta();
		List<String> lore = Item.getCompleteLore(item.getData(), item.getLore().toArray(new String[item.getLore().size()]), item.getUUID(), item.isCustom(), item.getItemClass());
		List<Spell> spells = registry.getSpells(item);
		lore.add(" ");
		if(spells.size() == 0) {
			lore.add(ChatColor.LIGHT_PURPLE + "Magic Item");
			lore.add(ChatColor.RED + "No spells are bound to");
			lore.add(ChatColor.RED + "this item yet!");
		}
		else if(!item.getName().contains(SpellConstants.MAGIC_ITEM_TITLE_PREFIX)) {
			item.setName(SpellConstants.MAGIC_ITEM_TITLE_PREFIX + ChatColor.stripColor(item.getName()));
			item.setCustom(true);
			meta.setDisplayName(item.getName());
		}
		lore.addAll(spells
			.stream()
			.map(spell -> ChatColor.DARK_PURPLE + "[" + spell.getSpellCombo() + "] " + ChatColor.LIGHT_PURPLE + spell.getSpellDisplayName())
			.collect(Collectors.toList()));
		meta.setLore(lore);
		item.getItemStack().setItemMeta(meta);
		if(slot != -1) {
			player.getInventory().setItem(slot, item.getItemStack());
		}
	}
}
