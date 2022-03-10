package mc.dragons.core.bridge;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.npc.NPC;

/**
 * Representation of a server-side player entity.
 * 
 * <p>Refer to the {@link mc.dragons.core.gameobject.npc.NPC NPC} class
 * for a more-abstracted version that wraps this functionality for the
 * case that its entity type is <code>PLAYER</code>.
 * 
 * <p>NPCs registered only through this interface will lack key features
 * of NPCs and will not behave properly. Use {@link mc.dragons.core.gameobject.npc.NPCLoader NPCLoader}
 * to properly construct an NPC.
 * 
 * @author DanielTheDev https://gist.github.com/DanielTheDev/cb51c11bd2551fd9a6700c582d12ff48
 * @author Adam
 *
 */
public interface PlayerNPC {
	enum Recipient {
		ALL, LISTED_RECIPIENTS;
	}

	enum NPCAnimation {
		SWING_MAIN_HAND(0), TAKE_DAMAGE(1), LEAVE_BED(2), SWING_OFFHAND(3), CRITICAL_EFFECT(4),
		MAGIC_CRITICAL_EFFECT(5);

		private int id;

		private NPCAnimation(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

	}

	enum NPCStatus {
		HURT(2), DIE(3);

		private int id;

		private NPCStatus(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}

	public static class Action {
		private boolean onFire, crouched, sprinting, invisible, glowing, flyingElytra;
		private byte result = 0;

		public Action(boolean on_fire, boolean crouched, boolean sprinting, boolean invisible, boolean glowing,
				boolean flying_elytra) {
			this.onFire = on_fire;
			this.crouched = crouched;
			this.sprinting = sprinting;
			this.invisible = invisible;
			this.glowing = glowing;
			this.flyingElytra = flying_elytra;
		}

		public boolean isOnFire() {
			return onFire;
		}

		public Action setOnFire(boolean onFire) {
			this.onFire = onFire;
			return this;
		}

		public boolean isCrouched() {
			return crouched;
		}

		public Action setCrouched(boolean crouched) {
			this.crouched = crouched;
			return this;
		}

		public boolean isSprinting() {
			return sprinting;
		}

		public Action setSprinting(boolean sprinting) {
			this.sprinting = sprinting;
			return this;
		}

		public boolean isInvisible() {
			return invisible;
		}

		public Action setInvisible(boolean invisible) {
			this.invisible = invisible;
			return this;
		}

		public boolean isGlowing() {
			return glowing;
		}

		public Action setGlowing(boolean glowing) {
			this.glowing = glowing;
			return this;
		}

		public boolean isFlyingElytra() {
			return flyingElytra;
		}

		public Action setFlyingElytra(boolean flyingElytra) {
			this.flyingElytra = flyingElytra;
			return this;
		}

		public byte build() {
			result = 0;
			result = add(this.onFire, (byte) 0x01);
			result = add(this.crouched, (byte) 0x02);
			result = add(this.sprinting, (byte) 0x08);
			result = add(this.invisible, (byte) 0x20);
			result = add(this.glowing, (byte) 0x40);
			result = add(this.flyingElytra, (byte) 0x80);
			return result;
		}

		private byte add(boolean condition, byte amount) {
			return (byte) (result += (condition ? amount : 0x00));
		}
	}

	/**
	 * 
	 * @return The entity ID of the backing Minecraft entity.
	 */
	public int getEntityId();
	
	/**
	 * 
	 * @return Whether all players or only a list of players
	 * can see this NPC.
	 */
	public Recipient getRecipientType();
	
	/**
	 * 
	 * @return All players authorized to see the NPC.
	 * If the recipient mode is <code>ALL</code>, this
	 * is meaningless.
	 */
	public List<Player> getRecipients();
	
	/**
	 * Teleport the NPC to the specified location.
	 * 
	 * @param location
	 * @param onGround
	 */
	public void teleport(Location location, boolean onGround);
	
	/**
	 * 
	 * @return The current location of the NPC.
	 */
	public Location getLocation();
	
	/**
	 * 
	 * @return The name of this NPC in the tablist.
	 */
	public String getTablistName();
	
	/**
	 * Add a player to the NPC's whitelist.
	 * 
	 * @param p
	 */
	public void addRecipient(Player p);
	
	/**
	 * Remove a player from the NPC's whitelist.
	 * 
	 * <p>Does not hide this NPC if the recipient type is <code>ALL</code>.
	 * 
	 * @param p
	 */
	public void removeRecipient(Player p);
	
	/**
	 * Set whether all players or only a list of players can see this NPC.
	 * 
	 * @param type
	 */
	public void setRecipientType(Recipient type);
	
	/**
	 * Spawn the NPC server-side, and also client-side for any players within
	 * the spawn range.
	 * 
	 * <p>If the NPC should have a skin, make sure you have called <code>setSkin</code>
	 * before calling this.
	 */
	public void spawn();
	
	/**
	 * Render the NPC client-side for the specified player.
	 * 
	 * <p>Not needed if the player was within the spawn range
	 * when the NPC was spawned.
	 * 
	 * <p>If unsure, rely on {@link mc.dragons.core.gameobject.npc.PlayerNPCRegistry PlayerNPCRegistry}
	 * to take care of all this.
	 * 
	 * @param player
	 */
	public void spawnFor(Player player);
	
	/**
	 * Set the name of the NPC in the tablist.
	 * 
	 * <p>Does not show the NPC in the tablist to
	 * players who do not already see it.
	 * 
	 * @param name
	 */
	public void setTablistName(String name);
	
	/**
	 * Make sure the player sees the NPC's correct and current location.
	 * @param player
	 * @param pitch
	 * @param yaw
	 */
	public void updateLocationFor(Player player, float pitch, float yaw);
	
	/**
	 * Refresh this NPC from the client perspective. Nothing is affected
	 * server-side.
	 * 
	 * <p>This should not generally need to be used.
	 */
	public void reload();
	
	/**
	 * Set the NPC's skin.
	 * 
	 * @apiNote This must be called <b>before</b> the NPC is spawned,
	 * or it will have no effect.
	 * 
	 * @param texture The texture data of the skin
	 * @param signature The signature data of the skin
	 */
	public void setSkin(String texture, String signature);
	
	/**
	 * Remove this NPC from the tablist for all players.
	 */
	public void removeFromTablist();
	
	/**
	 * Remove this NPC from the tablist for the specified player.
	 * @param player
	 */
	public void removeFromTablistFor(Player player);
	
	/**
	 * Refresh this NPC's display name on the tablist.
	 */
	public void updateToTablist();
	
	/**
	 * Add this NPC to the tablist.
	 */
	public void addToTablist();
	
	/**
	 * Set an equipment slot.
	 * @param slot
	 * @param item
	 */
	public void setEquipment(EquipmentSlot slot, ItemStack item);
	
	/**
	 * Remove the NPC.
	 * 
	 * @implNote Does NOT remove the Dragons NPC; only
	 * removes the Minecraft entity and removes from the client.
	 */
	public void destroy();
	
	/**
	 * 
	 * @return Whether the NPC has been removed.
	 */
	public boolean isDestroyed();
	
	/**
	 * Set the NPC's status (hurt/dead).
	 * @param status
	 */
	public void setStatus(NPCStatus status);
	
	/**
	 * Play an animation for this NPC.
	 * @param animation
	 */
	public void playAnimation(NPCAnimation animation);
	
	/**
	 * Update the NPC's head rotation.
	 * @param pitch
	 * @param yaw
	 */
	public void rotateHead(float pitch, float yaw);
	
	/**
	 * Make sure the player sees the NPC's correct and current head rotation.
	 * @param player
	 */
	public void refreshRotationFor(Player player);
	
	/**
	 * The associated Dragons (GameObject) {@link mc.dragons.core.gameobject.npc.NPC NPC}
	 * associated with this NPC.
	 * @return
	 */
	public NPC getDragonsNPC();
	
	/**
	 * 
	 * @return The Bukkit entity backing this NPC.
	 */
	public Entity getEntity();
	
	/**
	 * Gives the specified entity the same visibility as this NPC,
	 * i.e. each player will either see both this NPC and the specified
	 * entity, or neither.
	 * 
	 * @param entity
	 */
	public void setVisibilitySame(Entity entity);
}
