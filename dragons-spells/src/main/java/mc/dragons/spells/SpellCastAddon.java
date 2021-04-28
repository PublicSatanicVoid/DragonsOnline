package mc.dragons.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.spells.Spell;
import mc.dragons.spells.spells.Spell.BindStatus;
import mc.dragons.spells.spells.SpellRegistry;
import net.md_5.bungee.api.ChatColor;

public class SpellCastAddon extends ItemAddon {
	private SpellRegistry registry;
	
	public SpellCastAddon(DragonsSpells instance) {
		registry = instance.getSpellRegistry();
	}
	
	public void updateItemData(Item item) {
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
	}
	
	@Override
	public String getName() {
		return "SpellCast";
	}

	@Override
	public void initialize(GameObject gameObject) {
		if(!(gameObject instanceof Item)) return;
		Item item = (Item) gameObject;
		updateItemData(item);
	}

	@Override
	public void onCombo(User user, String combo) {
		user.debug("Combo " + combo);
		ItemStack itemStack = user.getPlayer().getInventory().getItemInMainHand();
		Item item = ItemLoader.fromBukkit(itemStack);
		List<Spell> spells = registry.getSpellsByCombo(combo);
		for(Spell spell : spells) {
			if(spell.bindStatus(item) == BindStatus.BOUND) {
				user.sendActionBar(ChatColor.DARK_PURPLE + "Cast " + ChatColor.LIGHT_PURPLE + spell.getSpellDisplayName());
				spell.execute(user);
				return;
			}
		}
		user.sendActionBar(ChatColor.RED + "No spell with that combo is bound to this item!");
	}

	@Override
	public void onPrepareCombo(User user, String combo) {
		user.sendActionBar(ChatColor.GRAY + "Casting spell: " + comboActionBarString(combo));
	}

	@Override
	public void onCreateStorageAccess(Document data) {
		if(!data.containsKey("spells")) {
			data.append("spells", new ArrayList<>());
		}
	}
	
	@Override
	public void onEnable() { }
}
