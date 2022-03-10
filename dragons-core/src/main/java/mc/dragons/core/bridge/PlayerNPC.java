package mc.dragons.core.bridge;

import java.io.IOException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.npc.NPC;

/**
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

		private boolean on_fire, crouched, sprinting, invisible, glowing, flying_elytra;
		private byte result = 0;

		public Action(boolean on_fire, boolean crouched, boolean sprinting, boolean invisible, boolean glowing,
				boolean flying_elytra) {
			this.on_fire = on_fire;
			this.crouched = crouched;
			this.sprinting = sprinting;
			this.invisible = invisible;
			this.glowing = glowing;
			this.flying_elytra = flying_elytra;
		}

		public Action() {
		}

		public boolean isOn_fire() {
			return on_fire;
		}

		public Action setOn_fire(boolean on_fire) {
			this.on_fire = on_fire;
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

		public boolean isFlying_elytra() {
			return flying_elytra;
		}

		public Action setFlying_elytra(boolean flying_elytra) {
			this.flying_elytra = flying_elytra;
			return this;
		}

		public byte build() {
			result = 0;
			result = add(this.on_fire, (byte) 0x01);
			result = add(this.crouched, (byte) 0x02);
			result = add(this.sprinting, (byte) 0x08);
			result = add(this.invisible, (byte) 0x20);
			result = add(this.glowing, (byte) 0x40);
			result = add(this.flying_elytra, (byte) 0x80);
			return result;
		}

		private byte add(boolean condition, byte amount) {
			return (byte) (result += (condition ? amount : 0x00));
		}
	}
	
	public List<Player> getRecipients();
	public int getEntityId();
	public Location getLocation();
	public Recipient getRecipientType();
	public String getDisplayName();
	public String getTablistName();
	public boolean isDestroyed();
	public void addRecipient(Player p);
	public void removeRecipient(Player p);
	public void setRecipientType(Recipient type);
	public void spawn();
	public void spawnFor(Player player);
	public void setDisplayNameAboveHead(String name) throws IOException;
	public void setDisplayName(String name) throws IOException;
	public void setTablistName(String name);
	public void reload();
	public void setSkin(String texture, String signature);
	public void removeFromTablist();
	public void updateToTablist();
	public void addToTablist();
	public void setEquipment(EquipmentSlot slot, ItemStack item);
	public void destroy();
	@Deprecated public void setStatus(byte status);
	public void setStatus(NPCStatus status);
	@Deprecated public void setAnimation(byte animation);
	public void setAnimation(NPCAnimation animation);
	public void teleport(Location location, boolean onGround);
	public void rotateHead(float pitch, float yaw);
	public void refreshRotationFor(Player player);
	public NPC getDragonsNPC();
	public Entity getEntity();
	public void removeFromTablistFor(Player player);
	public void updateLocationFor(Player player, float pitch, float yaw);
	
	/**
	 * Gives the specified entity the same visibility as this NPC,
	 * i.e. each player will either see both this NPC and the specified
	 * entity, or neither.
	 * 
	 * @param entity
	 */
	public void setVisibilitySame(Entity entity);
}
