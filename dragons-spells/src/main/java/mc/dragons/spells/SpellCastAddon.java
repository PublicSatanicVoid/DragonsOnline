package mc.dragons.spells;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.addon.ItemAddon;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemLoader;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.spells.Spell;
import mc.dragons.spells.spells.Spell.BindStatus;
import mc.dragons.spells.spells.SpellRegistry;
import mc.dragons.spells.util.SpellUtil;
import net.md_5.bungee.api.ChatColor;

public class SpellCastAddon extends ItemAddon {
	private SpellRegistry registry;
	
	public SpellCastAddon(DragonsSpells instance) {
		registry = instance.getSpellRegistry();
	}
	
	@Override
	public String getName() {
		return "SpellCast";
	}

	@Override
	public void initialize(User user, Item item) {
		ItemStack itemStack = item.getItemStack();
		int i = 0;
		boolean found = false;
		for(ItemStack test : user.getPlayer().getInventory().getContents()) {
			if(test != null && SpellUtil.sameItem(test, itemStack)) {
				SpellUtil.updateItemData(registry, user.getPlayer(), i, item);
				found = true;
			}
		}
		if(!found) {
			SpellUtil.updateAllSpellItems(registry, user.getPlayer());
		}
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
}
