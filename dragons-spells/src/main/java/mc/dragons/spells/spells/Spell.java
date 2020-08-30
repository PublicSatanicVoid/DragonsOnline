package mc.dragons.spells.spells;

import java.util.List;

import org.bson.Document;

import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.spells.SpellCastAddon;

public abstract class Spell {
	public static enum BindStatus {
		BOUND,
		CAPABLE,
		INCAPABLE,
		DISALLOWED
	}
	
	private String spellName;
	private String spellDisplayName;
	private String spellCombo;
	
	public Spell(String spellName, String spellDisplayName, String spellCombo) {
		this.spellName = spellName;
		this.spellDisplayName = spellDisplayName;
		this.spellCombo = spellCombo;
		SpellCastAddon.getSpellRegistry().register(this);
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
			Spell spell = SpellCastAddon.getSpellRegistry().getSpellByName(spellName);
			if(spell.getSpellCombo().equals(spellCombo)) return BindStatus.INCAPABLE;	
		}
		return BindStatus.CAPABLE;
	}
	
	public void bind(Item item) {
		BindStatus status = bindStatus(item);
		if(status != BindStatus.CAPABLE) return; 
		List<String> boundSpells = item.getStorageAccess().getDocument().getList("spells", String.class);
		boundSpells.add(spellName);
		item.getStorageAccess().update(new Document("spells", boundSpells));
		SpellCastAddon.updateItemData(item);
	}
	
	public abstract void execute(User user);
}
