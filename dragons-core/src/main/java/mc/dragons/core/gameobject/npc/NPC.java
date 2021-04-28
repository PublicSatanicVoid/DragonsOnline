package mc.dragons.core.gameobject.npc;

import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.ProgressBarUtil;
import mc.dragons.core.util.StringUtil;

/**
 * Represents an NPC in the RPG.
 * 
 * <p>In addition to the properties of Minecraft NPCs,
 * RPG NPCs have properties like dialogue and enhanced
 * combat capabilities, as well as levels and more specific
 * interaction options.
 * 
 * <p>There is a many-to-many has-a relationship between
 * RPG NPC types and Minecraft NPC types.
 * 
 * @author Adam
 *
 */
public class NPC extends GameObject {

	public enum NPCType {
		HOSTILE(ChatColor.RED, "", false, false, true, false, false),
		NEUTRAL(ChatColor.YELLOW, "", false, false, true, false, false),
		QUEST(ChatColor.DARK_GREEN, ChatColor.DARK_GREEN + "[NPC] ", true, true, false, true, true), 
		SHOP(ChatColor.DARK_AQUA, ChatColor.DARK_AQUA + "[NPC] ", true, true, false, true, true),
		PERSISTENT(ChatColor.YELLOW, "", true, true, true, true, true), 
		COMPANION(ChatColor.GOLD, ChatColor.GOLD + "[COMPANION] ", true, false, true, false, true);

		private ChatColor nameColor;
		private String prefix;
		private boolean persistent;
		private boolean immortalByDefault;
		private boolean aiByDefault;
		private boolean loadImmediately;
		private boolean respawnOnDeath;

		NPCType(ChatColor nameColor, String prefix, boolean persistent, boolean immortalByDefault, boolean aiByDefault, boolean loadImmediately, boolean respawnOnDeath) {
			this.nameColor = nameColor;
			this.prefix = prefix;
			this.persistent = persistent;
			this.immortalByDefault = immortalByDefault;
			this.aiByDefault = aiByDefault;
			this.loadImmediately = loadImmediately;
			this.respawnOnDeath = respawnOnDeath;
		}

		public ChatColor getNameColor() {
			return nameColor;
		}

		public String getPrefix() {
			return prefix;
		}

		public boolean isPersistent() {
			return persistent;
		}

		public boolean isImmortalByDefault() {
			return immortalByDefault;
		}

		public boolean hasAIByDefault() {
			return aiByDefault;
		}

		public boolean isLoadedImmediately() {
			return loadImmediately;
		}

		public boolean canRespawnOnDeath() {
			return respawnOnDeath;
		}
	}	
	
	protected Entity entity;
	protected Entity healthIndicator;
	protected boolean isDamageExternalized = false;

	protected static GameObjectRegistry registry = Dragons.getInstance().getGameObjectRegistry();
	protected static NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.<NPCClass, NPCClassLoader>getLoader();
	protected static EntityHider entityHider = Dragons.getInstance().getEntityHider();

	public NPC(Location loc, List<BukkitRunnable> asyncSpawnHandler, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		asyncSpawnHandler.add(new BukkitRunnable() {
			@Override public void run() {
				long start = System.currentTimeMillis();
				entity = loc.getWorld().spawnEntity(loc, EntityType.valueOf((String) storageAccess.get("entityType")));
				entity.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), NPC.this));
				initializeEntity();
				initializeAddons();
				long duration = System.currentTimeMillis() - start;
				Bukkit.getLogger().finer("Spawned " + getUUID() + " - " + getNPCClass().getClassName() + " in " + duration + "ms");
			}
		});
	}
	
	public NPC(Entity entity, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.verbose("Constructing NPC (" + StringUtil.entityToString(entity) + ", " + storageManager + ", " + storageAccess + ")");
		this.entity = entity;
		initializeEntity();
		initializeAddons();
	}

	public void initializeEntity() {
		entity.setCustomName(getDecoratedName());
		entity.setCustomNameVisible(true);
		healthIndicator = entity;
		Dragons.getInstance().getBridge().setEntityAI(entity, getNPCClass().hasAI());
		Dragons.getInstance().getBridge().setEntityInvulnerable(entity, isImmortal());
		// TODO configurable baby status if ageable
		if (entity.isInsideVehicle()) {
			entity.getVehicle().eject();
		}
		if (entity instanceof Attributable) {
			Attributable att = (Attributable) entity;
			for (Entry<Attribute, Double> a : getNPCClass().getCustomAttributes().entrySet()) {
				att.getAttribute(a.getKey()).setBaseValue(a.getValue());
			}
		}
		Material heldItemType = getNPCClass().getHeldItemType();
		if (heldItemType != null) {
			setHeldItem(new ItemStack(heldItemType));
		}
		entity.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), this));
	}
	
	public void initializeAddons() {
		getNPCClass().getAddons().forEach(addon -> addon.initialize(this));
	}

	public boolean isDamageExternalized() {
		return isDamageExternalized;
	}

	public void setDamageExternalized(boolean externalized) {
		isDamageExternalized = externalized;
	}

	public NPCClass getNPCClass() {
		String className = (String) getData("className");
		if (className == null) {
			return null;
		}
		return npcClassLoader.getNPCClassByClassName(className);
	}

	public void setMaxHealth(double maxHealth) {
		if (entity instanceof Attributable) {
			Attributable attributable = (Attributable) entity;
			attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
			setData("maxHealth", Double.valueOf(maxHealth));
		}
	}

	public double getMaxHealth() {
		if (entity instanceof Attributable) {
			Attributable attributable = (Attributable) entity;
			return attributable.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		}
		return 0.0D;
	}

	public void setHealth(double health) {
		if (entity instanceof Damageable) {
			Damageable damageable = (Damageable) entity;
			damageable.setHealth(health);
		}
	}

	public void damage(double damage, Entity source) {
		if (entity instanceof Damageable) {
			Damageable damageable = (Damageable) entity;
			damageable.damage(damage, source);
		}
	}

	public void damage(double damage) {
		if (entity instanceof Damageable) {
			Damageable damageable = (Damageable) entity;
			damageable.damage(damage);
		}
	}

	public double getHealth() {
		if (entity instanceof Damageable) {
			Damageable damageable = (Damageable) entity;
			return damageable.getHealth();
		}
		return 0.0D;
	}

	public ItemStack getHeldItem() {
		if (entity instanceof LivingEntity) {
			return ((LivingEntity) entity).getEquipment().getItemInMainHand();
		}
		return null;
	}

	public void setHeldItem(ItemStack itemStack) {
		if (entity instanceof LivingEntity) {
			((LivingEntity) entity).getEquipment().setItemInMainHand(itemStack);
		}
		setData("holding", itemStack.getType().toString());
	}

	public boolean isImmortal() {
		return ((Boolean) getData("immortal")).booleanValue();
	}

	public void setExternalHealthIndicator(ArmorStand indicator) {
		healthIndicator.setCustomNameVisible(false);
		healthIndicator = indicator;
		indicator.setCustomNameVisible(true);
		updateHealthBar(0.0D);
	}

	public void updateHealthBar() {
		updateHealthBar(0.0D);
	}

	public void updateHealthBar(double additionalDamage) {
		healthIndicator.setCustomName(String.valueOf(getDecoratedName()) + (isImmortal() ? ChatColor.LIGHT_PURPLE + " [Immortal]"
				: ChatColor.DARK_GRAY + " [" + ProgressBarUtil.getHealthBar(getHealth() - additionalDamage, getMaxHealth()) + ChatColor.DARK_GRAY + "]"));
	}

	public String getName() {
		return (String) getData("name");
	}

	public String getDecoratedName() {
		return String.valueOf(getNPCType().getPrefix()) + getNPCType().getNameColor() + getName() + ChatColor.GRAY + " Lv " + getLevel();
	}

	public NPCType getNPCType() {
		return NPCType.valueOf((String) getData("npcType"));
	}

	public void setNPCType(NPCType npcType) {
		setData("npcType", npcType.toString());
	}

	public int getLevel() {
		return (int) getData("level");
	}

	public void setTarget(LivingEntity target) {
		if (entity instanceof Creature) {
			if (target != null) {
				Creature c = (Creature) entity;
				c.setTarget(target);
			}
			entity.setMetadata("target", new FixedMetadataValue(Dragons.getInstance(), target));
		}
	}

	public LivingEntity getDeclaredTarget() {
		if (entity instanceof Creature && entity.getMetadata("target").size() > 0) {
			return (LivingEntity) entity.getMetadata("target").get(0).value();
		}
		return null;
	}

	public void remove() {
		entity.remove();
		registry.removeFromDatabase(this);
	}

	public void phase(Player playerFor) {
		LOGGER.trace("Phasing NPC " + getIdentifier() + " for " + playerFor.getName());
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (!p.equals(playerFor)) {
				entityHider.hideEntity(p, entity);
			}
		}
		entityHider.showEntity(playerFor, entity);
	}

	public void unphase(Player playerFor) {
		entityHider.hideEntity(playerFor, entity);
	}

	public void setEntity(Entity entity) {
		if (this.entity != null) {
			this.entity.removeMetadata("handle", Dragons.getInstance());
		}
		LOGGER.trace("Replacing entity backing NPC " + getIdentifier() + ": " + StringUtil.entityToString(this.entity) + " -> " + StringUtil.entityToString(entity));
		this.entity = entity;
		this.entity.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), this));
	}

	public Entity getEntity() {
		return entity;
	}

	public void regenerate(Location spawn) {
		LOGGER.trace("Regenerating NPC " + getIdentifier() + " at " + StringUtil.locToString(spawn));
		if (entity != null) {
			entity.remove();
		}
		setEntity(spawn.getWorld().spawnEntity(spawn, getNPCClass().getEntityType()));
		healthIndicator = entity;
		initializeEntity();
	}
}
