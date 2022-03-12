package mc.dragons.spells.spells;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.DragonsSpells;
import mc.dragons.spells.util.SpellUtil;

public abstract class Spell {
	public static enum BindStatus {
		BOUND,
		CAPABLE,
		INCAPABLE,
		DISALLOWED
	}
	
	private SpellRegistry spellRegistry;
	
	private String spellName;
	private String spellDisplayName;
	private String spellCombo;
	
	public Spell(DragonsSpells instance, String spellName, String spellDisplayName, String spellCombo) {
		this.spellName = spellName;
		this.spellDisplayName = spellDisplayName;
		this.spellCombo = spellCombo;
		instance.getSpellRegistry().register(this);
		spellRegistry = instance.getSpellRegistry();
	}
	
	public String getSpellName() {
		return spellName;
	}
	
	public String getSpellDisplayName() {
		return spellDisplayName;
	}
	
	public String getSpellCombo() {
		return spellCombo;
	}
	
	public BindStatus bindStatus(Item item) {
		List<String> boundSpells = item.getStorageAccess().getDocument().getList("spells", String.class);
		if(boundSpells == null) return BindStatus.DISALLOWED;
		for(String spellName : boundSpells) {
			if(spellName.equals(this.spellName)) return BindStatus.BOUND; 
			Spell spell = spellRegistry.getSpellByName(spellName);
			if(spell.getSpellCombo().equals(spellCombo)) return BindStatus.INCAPABLE;	
		}
		return BindStatus.CAPABLE;
	}
	
	public void bind(Player player, Item item) {
		BindStatus status = bindStatus(item);
		if(status != BindStatus.CAPABLE) return; 
		List<String> boundSpells = item.getStorageAccess().getDocument().getList("spells", String.class);
		boundSpells.add(spellName);
		item.getStorageAccess().update(new Document("spells", boundSpells));
		SpellUtil.updateItemData(spellRegistry, player, SpellUtil.getSlotOfItemStack(player, item.getItemStack()), item);
	}
	
	public abstract void execute(User user);
}
