package mc.dragons.core.gameobject.npc;

import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.bridge.PlayerNPC;
import mc.dragons.core.bridge.PlayerNPC.Recipient;
import mc.dragons.core.bridge.impl.PlayerNPC116R3;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectRegistry;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.EntityHider;
import mc.dragons.core.util.HologramUtil;
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
 * <p>In contrast to other RPGs, the term "NPC" is used here
 * to refer to <i>any</i> custom entity which is registered
 * to a game object. Thus, "mobs" and "characters" are both
 * considered NPCs.
 * 
 * <p>The NPCTypes of HOSTILE and NEUTRAL refer to "mobs",
 * while QUEST, SHOP, PERSISTENT, and COMPANION refer to
 * "characters".
 * 
 * <p>An NPC may look like a player, and if its entity type is
 * a player, may require special handling elsewhere although
 * most of the internals are abstracted away.
 * 
 * <p>Some NPCs may be persistent and only exist in one fixed
 * place, while others may be non-persistent and spawn regularly
 * in a given region. This all depends upon their function in
 * the game.
 * 
 * @author Adam
 *
 */
public class NPC extends GameObject {

	/**
	 * The behavioral class of an NPC.
	 * 
	 * <p>Different types are associated with different
	 * default settings for AI, vulnerability, persistence,
	 * etc.
	 * 
	 * @author Adam
	 *
	 */
	public enum NPCType {
		HOSTILE(ChatColor.RED, "", false, false, true, false, false, true),
		NEUTRAL(ChatColor.YELLOW, "", false, false, true, false, false, true),
		QUEST(ChatColor.DARK_GREEN, ChatColor.DARK_GREEN + "[NPC] ", true, true, false, true, true, false), 
		SHOP(ChatColor.DARK_AQUA, ChatColor.DARK_AQUA + "", true, true, false, true, true, false),
		PERSISTENT(ChatColor.YELLOW, "", true, true, true, true, true, true), 
		COMPANION(ChatColor.GOLD, ChatColor.GOLD + "[COMPANION] ", true, false, true, false, true, true);

		private ChatColor nameColor;
		private String prefix;
		private boolean persistent;
		private boolean immortalByDefault;
		private boolean aiByDefault;
		private boolean loadImmediately;
		private boolean respawnOnDeath;
		private boolean showLevel;

		NPCType(ChatColor nameColor, String prefix, boolean persistent, boolean immortalByDefault,
				boolean aiByDefault, boolean loadImmediately, boolean respawnOnDeath, boolean showLevel) {
			this.nameColor = nameColor;
			this.prefix = prefix;
			this.persistent = persistent;
			this.immortalByDefault = immortalByDefault;
			this.aiByDefault = aiByDefault;
			this.loadImmediately = loadImmediately;
			this.respawnOnDeath = respawnOnDeath;
			this.showLevel = showLevel;
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
		
		public boolean showLevel() {
			return showLevel;
		}
	}	
	
	protected Entity entity;
	protected PlayerNPC pnpc;
	protected Entity pnpcGuide;
	protected Entity healthIndicator;
	protected boolean isDamageExternalized = false;

	// Yeah yeah it's static abuse, but would you really prefer storing instances of these for EVERY NPC?
	// Yeah, didn't think so.
	protected static GameObjectRegistry registry = Dragons.getInstance().getGameObjectRegistry();
	protected static NPCClassLoader npcClassLoader = GameObjectType.NPC_CLASS.getLoader();
	protected static EntityHider entityHider = Dragons.getInstance().getEntityHider();
	protected static PlayerNPCRegistry playerNPCRegistry = Dragons.getInstance().getPlayerNPCRegistry();
	protected static Bridge bridge = Dragons.getInstance().getBridge();
	
	/**
	 * Lazy construction of the NPC. It will exist in memory but the NPC will not
	 * be spawned until the async spawn handler is called.
	 * 
	 * <p>This is the strongly preferred method of spawning NPCs.
	 * 
	 * @param loc The location to spawn the NPC at.
	 * @param asyncSpawnHandler List of async spawn handlers to add this NPC's spawn
	 * 	handler to.
	 * @param storageManager
	 * @param storageAccess
	 */
	public NPC(Location loc, List<BukkitRunnable> asyncSpawnHandler, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		asyncSpawnHandler.add(new BukkitRunnable() {
			@Override public void run() {
				long start = System.currentTimeMillis();
				EntityType type = getEntityType();
				if(type == EntityType.PLAYER) {
					pnpc = new PlayerNPC116R3(getName(), loc, NPC.this);
					NPCClass npcClass = getNPCClass();
					String texture = npcClass.getSkinTexture();
					String signature = npcClass.getSkinSignature();
					if(texture != null && signature != null) {
						pnpc.setSkin(texture, signature);
					}
					pnpc.spawn();
					entity = pnpc.getEntity();
					if(getNPCType() == NPCType.HOSTILE) {
						Zombie hidden = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
						hidden.setInvisible(true);
						hidden.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
						hidden.setMetadata("partOf", new FixedMetadataValue(Dragons.getInstance(), NPC.this));
						hidden.setMetadata("shadow", new FixedMetadataValue(Dragons.getInstance(), pnpc.getEntity()));
						pnpcGuide = hidden;
					}
					else if(getNPCClass().hasAI()) {
						Villager hidden = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
						hidden.setInvisible(true);
						hidden.setMetadata("allow", new FixedMetadataValue(Dragons.getInstance(), true));
						hidden.setMetadata("partOf", new FixedMetadataValue(Dragons.getInstance(), NPC.this));
						hidden.setMetadata("shadow", new FixedMetadataValue(Dragons.getInstance(), pnpc.getEntity()));
						pnpcGuide = hidden;
					}
				}
				else {
					entity = loc.getWorld().spawnEntity(loc, type);
					entity.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), NPC.this));
				}
				initializeEntity();
				initializeAddons();
				long duration = System.currentTimeMillis() - start;
				LOGGER.verbose("Spawned " + getUUID() + " - " + getNPCClass().getClassName() + " in " + duration + "ms (" + StringUtil.entityToString(entity) + ")");
			}
		});
	}
	
	/**
	 * @deprecated May miss key init logic depending on the entity type.
	 * @param entity
	 * @param storageManager
	 * @param storageAccess
	 */
	@Deprecated
	public NPC(Entity entity, StorageManager storageManager, StorageAccess storageAccess) {
		super(storageManager, storageAccess);
		LOGGER.warning("Do not construct NPCs with the (Entity, StorageManager, StorageAccess) constructor!");
		LOGGER.verbose("Constructing NPC (" + StringUtil.entityToString(entity) + ", " + storageManager + ", " + storageAccess + ")");
		this.entity = entity;
		initializeEntity();
		initializeAddons();
	}

	/**
	 * Call once when the backing Bukkit entity is set or changed.
	 */
	public void initializeEntity() {
		if(getEntityType() == EntityType.PLAYER) {
			((Player) entity).setHealth(getMaxHealth());
			Material heldItemType = getNPCClass().getHeldItemType();
			if(heldItemType != null) {
				pnpc.setEquipment(EquipmentSlot.HAND, new ItemStack(heldItemType));
			}
			ArmorStand hologram = HologramUtil.makeHologram(getDecoratedName(), getEntity().getLocation().add(0, 0.8, 0));
			pnpc.setVisibilitySame(hologram);
			hologram.setMetadata("followDY", new FixedMetadataValue(Dragons.getInstance(), 0.8));
			getEntity().setMetadata("shadow", new FixedMetadataValue(Dragons.getInstance(), hologram));
			setExternalHealthIndicator(hologram);
		}
		else {
			entity.setCustomName(getDecoratedName());
			entity.setCustomNameVisible(true);
			healthIndicator = entity;
			Dragons.getInstance().getBridge().setEntityAI(entity, getNPCClass().hasAI());
			Dragons.getInstance().getBridge().setEntityInvulnerable(entity, isImmortal());
			// TODO configurable baby status if ageable
			if(entity instanceof Ageable) {
				Ageable ageable = (Ageable) entity;
				ageable.setAdult();
			}
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
		}
		entity.setMetadata("handle", new FixedMetadataValue(Dragons.getInstance(), this));
		
	}
	
	/**
	 * Call once during construction.
	 */
	public void initializeAddons() {
		getNPCClass().getAddons().forEach(addon -> addon.initialize(this));
	}

	/**
	 * 
	 * @return The Bukkit entity type of this NPC.
	 */
	public EntityType getEntityType() {
		return EntityType.valueOf((String) storageAccess.get("entityType"));
	}
	
	/**
	 * 
	 * @return Whether damage to this NPC comes from other entities.
	 */
	public boolean isDamageExternalized() {
		return isDamageExternalized;
	}

	/**
	 * Set whether damage to this NPC comes from other entities.
	 * 
	 * @param externalized
	 */
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
			setData("maxHealth", maxHealth);
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
		return getMaxHealth();
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
		return (boolean) getData("immortal");
	}

	/**
	 * Specify an armor stand to show this NPC's health status.
	 * @param indicator
	 */
	public void setExternalHealthIndicator(ArmorStand indicator) {
		if(healthIndicator != null) {
			healthIndicator.setCustomNameVisible(false);
		}
		healthIndicator = indicator;
		indicator.setCustomNameVisible(true);
		updateHealthBar();
	}

	public void updateHealthBar() {
		updateHealthBar(0.0D);
	}

	public void updateHealthBar(double additionalDamage) {
		healthIndicator.setCustomName(getDecoratedName() + (isImmortal() ? ""
				: ChatColor.DARK_GRAY + " [" + ProgressBarUtil.getHealthBar(getHealth() - additionalDamage, getMaxHealth()) + ChatColor.DARK_GRAY + "]"));
	}

	public String getName() {
		return (String) getData("name");
	}

	public String getDecoratedName() {
		return getNPCType().getPrefix() + getNPCType().getNameColor() + getName() + (getNPCType().showLevel() ?  ChatColor.GRAY + " Lv " + getLevel() : "");
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
		if(entity != null) {
			if(entity.hasMetadata("shadow")) {
				for(MetadataValue shadow : entity.getMetadata("shadow")) {
					Entity e = (Entity) shadow.value();
					e.remove();
				}
			}
			if(entity.getType() != EntityType.PLAYER) {
				entity.remove();
			}
		}
		if(pnpcGuide != null) {
			pnpcGuide.remove();
		}
		if(pnpc != null) {
			pnpc.destroy();
		}
		if(healthIndicator != null) {
			healthIndicator.remove();
		}
		registry.removeFromDatabase(this);
	}

	/**
	 * Only allow this NPC to appear to the specified player.
	 * 
	 * @param playerFor
	 */
	public void phase(Player playerFor) {
		LOGGER.trace("Phasing NPC " + getIdentifier() + " for " + playerFor.getName());
		if(getEntityType() == EntityType.PLAYER) {
			pnpc.setRecipientType(Recipient.LISTED_RECIPIENTS);
			pnpc.addRecipient(playerFor);
			pnpc.reload();
		}
		else {
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (!p.equals(playerFor)) {
					entityHider.hideEntity(p, entity);
				}
			}
			entityHider.showEntity(playerFor, entity);
		}
	}

	/**
	 * Hide this NPC from the specified player.
	 * 
	 * @param playerFor
	 */
	public void unphase(Player playerFor) {
		if(getEntityType() == EntityType.PLAYER) {
			pnpc.removeRecipient(playerFor);
			pnpc.reload();
		}
		else {
			entityHider.hideEntity(playerFor, entity);
		}
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
		if(this.entity != null) {
			return entity;
		}
		else {
			return pnpc.getEntity();
		}
	}
	
	public PlayerNPC getPlayerNPC() {
		return pnpc;
	}

	public void regenerate(Location spawn) {
		LOGGER.debug("Regenerating NPC " + getIdentifier() + " at " + StringUtil.locToString(spawn));
		if (pnpc != null) {
			pnpc.destroy();
			pnpc.spawn();
			entity = pnpc.getEntity();
			pnpcGuide.removeMetadata("shadow", Dragons.getInstance());
			pnpcGuide.setMetadata("shadow", new FixedMetadataValue(Dragons.getInstance(), pnpc.getEntity()));
			initializeEntity();
		}
		else {
			entity.remove();
			setEntity(spawn.getWorld().spawnEntity(spawn, getNPCClass().getEntityType()));
			healthIndicator = entity;
			initializeEntity();
		}
	}
}
