package mc.dragons.spells.spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import mc.dragons.core.gameobject.item.Item;

public class SpellRegistry {
	private Map<String, List<Spell>> spells;
	
	public SpellRegistry() {
		spells = new HashMap<>();
	}
	
	public void register(Spell spell) {
		List<Spell> sub = spells.getOrDefault(spell.getSpellCombo(), new ArrayList<>());
		sub.add(spell);
		spells.put(spell.getSpellCombo(), sub);
	}
	
	public List<Spell> getSpellsByCombo(String combo) {
		return spells.getOrDefault(combo, new ArrayList<>());
	}
	
	public Spell getSpellByName(String name) {
		for(List<Spell> spellList : spells.values()) {
			for(Spell spell : spellList) {
				if(spell.getSpellName().equalsIgnoreCase(name)) {
					return spell;
				}
			}
		}
		return null;
	}
	
	public Spell getSpellByDisplayName(String name) {
		for(List<Spell> spellList : spells.values()) {
			for(Spell spell : spellList) {
				if(spell.getSpellDisplayName().equalsIgnoreCase(name)) {
					return spell;
				}
			}
		}
		return null;
	}
	
	public List<Spell> getSpells(Item item) {
		return item.getStorageAccess().getDocument().getList("spells", String.class)
				.stream().map(spellName -> getSpellByName(spellName)).collect(Collectors.toList());
	}
}
