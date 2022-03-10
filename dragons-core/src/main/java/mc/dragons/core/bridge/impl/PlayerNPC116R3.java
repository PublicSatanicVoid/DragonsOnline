package mc.dragons.core.bridge.impl;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftEquipmentSlot;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;
import mc.dragons.core.bridge.PlayerNPC;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.PlayerNPCRegistry;
import mc.dragons.core.util.HologramUtil;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.EnumGamemode;
import net.minecraft.server.v1_16_R3.EnumProtocolDirection;
import net.minecraft.server.v1_16_R3.MobEffect;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntity;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityEffect;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_16_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_16_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_16_R3.PlayerConnection;
import net.minecraft.server.v1_16_R3.PlayerInteractManager;

/**
 * 
 * @author DanielTheDev (https://gist.github.com/DanielTheDev/cb51c11bd2551fd9a6700c582d12ff48)
 * @author vivabenfica4 (https://www.spigotmc.org/threads/use-entity-packet-help.284421/)
 * @author Adam
 *
 */
public class PlayerNPC116R3 implements PlayerNPC {
	private static int FORCE_REFRESH_LOCATION_INTERVAL_MS = 1000 * 5;
	private static Dragons dragons = Dragons.getInstance();
	private static Bridge bridge = dragons.getBridge();
	private static PlayerNPCRegistry registry = dragons.getPlayerNPCRegistry();
	
	private UUID uuid;
	private Location location;

	private List<Player> recipients;
	private Recipient recipientType = Recipient.ALL;

	private String displayName;
	private String tablistName;

	private String texture = null;
	private String signature = null;
	private Map<Player, Location> lastSeenLocation = new HashMap<>();
	private Map<Player, Long> lastForceRefresh = new HashMap<>();
	
	private boolean isDestroyed;
	private NPC npc;
	public EntityPlayer handle;
	
	public PlayerNPC116R3(String name, Location location, NPC npc) {
		this.npc = npc;
		this.recipientType = Recipient.ALL;
		this.recipients = new ArrayList<Player>();
		this.uuid = UUID.randomUUID();
		try {
			setDisplayName(name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.tablistName = name;
		this.location = location;
	}

	public List<Player> getRecipients() {
		return this.recipients;
	}

	public Location getLocation() {
		return location;
	}

	public Recipient getRecipientType() {
		return recipientType;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getTablistName() {
		return tablistName;
	}

	public boolean isDestroyed() {
		return isDestroyed;
	}

	public void addRecipient(Player p) {
		this.recipients.add(p);
	}
	
	public void removeRecipient(Player p) {
		this.recipients.remove(p);
	}
	
	public void setRecipientType(Recipient recipientType) {
		this.recipientType = recipientType;
	}
	
	public int getEntityId() {
		return handle.getId();
	}
	
	public void spawn() {
		this.isDestroyed = false;
		String texture = this.texture;
		String signature = this.signature;
		GameProfile gameProfile = new GameProfile(uuid, ""); // displayName);
		gameProfile.getProperties().clear();
		gameProfile.getProperties().put("textures", new Property("textures", texture, signature));
		this.handle = new EntityPlayer(((CraftServer) Bukkit.getServer()).getServer(),
				((CraftWorld) location.getWorld()).getHandle(), gameProfile,
				new PlayerInteractManager(((CraftWorld) location.getWorld()).getHandle()));
		handle.persist = true;
		handle.collides = false;
		handle.setCustomNameVisible(false);
		handle.setInvulnerable(npc.isImmortal());
		handle.getBukkitEntity().setMetadata("handle", new FixedMetadataValue(dragons, npc));
		handle.playerConnection = new PlayerConnection(((CraftServer) Bukkit.getServer()).getServer(),
				new NetworkManager(EnumProtocolDirection.SERVERBOUND), handle);
		((CraftWorld) location.getWorld()).addEntity(handle, CreatureSpawnEvent.SpawnReason.CUSTOM);
		handle.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		registry.register(this);
		ArmorStand hologram = HologramUtil.makeHologram(npc.getDecoratedName(), getEntity().getLocation().add(0, 0.8, 0));
		hologram.setMetadata("followDY", new FixedMetadataValue(dragons, 0.8));
		npc.getEntity().setMetadata("shadow", new FixedMetadataValue(dragons, hologram));
		npc.setExternalHealthIndicator(hologram);
	}
	
	public void spawnFor(Player player) {
		if(isDestroyed) return;
		location = getEntity().getLocation(); // resync
		handle.setCustomNameVisible(false);
		sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, handle), player);
		sync(() -> sendPacket(new PacketPlayOutNamedEntitySpawn(handle), player), 1);
		PacketPlayOutEntityTeleport tp1 = new PacketPlayOutEntityTeleport();
		Location inFrontOfPlayer = player.getLocation().add(player.getEyeLocation().getDirection().normalize().multiply(0.1));
		setField(tp1, "a", getEntityId());
		setField(tp1, "b", inFrontOfPlayer.getX());
		setField(tp1, "c", inFrontOfPlayer.getY());
		setField(tp1, "d", inFrontOfPlayer.getZ());
		setField(tp1, "e", (byte) 0); // Don't care
		setField(tp1, "f", (byte) 0); // Don't care
		setField(tp1, "g", true); // Don't care
		PacketPlayOutEntityTeleport tp2 = new PacketPlayOutEntityTeleport();
		setField(tp2, "a", getEntityId());
		setField(tp2, "b", location.getX());
		setField(tp2, "c", location.getY());
		setField(tp2, "d", location.getZ());
		setField(tp2, "e", getPacketRotation(location.getYaw()));
		setField(tp2, "f", getPacketRotation(location.getPitch()));
		setField(tp2, "g", handle.isOnGround());
		sync(() -> sendPacket(new PacketPlayOutEntityMetadata(handle.getId(), handle.getDataWatcher(), true), player), 2);
		sync(() -> setTablistName(getTablistName()), 2);
		sync(() -> sendPacket(new PacketPlayOutEntityHeadRotation(handle, getPacketRotation(location.getYaw())), player), 6);
		sync(() -> removeFromTablistFor(player), 20 + (int) Math.ceil(2 * bridge.getPing(player) * 20 / 1000));
		lastSeenLocation.put(player, location.clone());
	}
	
	public void updateLocationFor(Player player, float pitch, float yaw) {
		if(!lastSeenLocation.containsKey(player)) {
			spawnFor(player);
			return;
		}
		location = getEntity().getLocation(); // resync
		byte byaw = getPacketRotation(yaw);
		byte bpitch = getPacketRotation(pitch);
		Vector move = getEntity().getLocation().subtract(lastSeenLocation.get(player)).toVector();
		
		// It's occasionally a good idea to completely resync the NPC's location,
		// since error in the dx/dy/dz's can accumulate or get desynced with the client.
		if(move.lengthSquared() > 10 * 10 || System.currentTimeMillis() - lastForceRefresh.getOrDefault(player, 0L) > FORCE_REFRESH_LOCATION_INTERVAL_MS) {
			PacketPlayOutEntityTeleport tp = new PacketPlayOutEntityTeleport();
			setField(tp, "a", getEntityId());
			setField(tp, "b", location.getX());
			setField(tp, "c", location.getY());
			setField(tp, "d", location.getZ());
			setField(tp, "e", byaw);
			setField(tp, "f", bpitch);
			setField(tp, "g", handle.isOnGround());
			sync(() -> sendPacket(tp, player));
			lastForceRefresh.put(player, System.currentTimeMillis());
		}
		
		// But usually we can just send dx/dy/dz, resulting in smoother movement
		// and smaller packets
		else {
			int dx = (int) move.getX() * 4096;
			int dy = (int) move.getY() * 4096;
			int dz = (int) move.getZ() * 4096;
			boolean onGround = handle.isOnGround();
			PacketPlayOutRelEntityMoveLook packet = new PacketPlayOutRelEntityMoveLook(getEntityId(), (short) dx, (short) dy, (short) dz, byaw, bpitch, onGround);
			sendPacket(packet, player);
		}
		sync(() -> sendPacket(new PacketPlayOutEntityHeadRotation(handle, byaw), player));
		lastSeenLocation.put(player, location);
	}

	public void refreshRotationFor(Player player) {
		location = getEntity().getLocation(); // resync
		PacketPlayOutEntityTeleport tp = new PacketPlayOutEntityTeleport();
		setField(tp, "a", getEntityId());
		setField(tp, "b", location.getX());
		setField(tp, "c", location.getY());
		setField(tp, "d", location.getZ());
		setField(tp, "e", getPacketRotation(location.getYaw()));
		setField(tp, "f", getPacketRotation(location.getPitch()));
		setField(tp, "g", handle.isOnGround());
		sync(() -> sendPacket(tp, player));
		sync(() -> sendPacket(new PacketPlayOutEntityHeadRotation(handle, getPacketRotation(location.getYaw())), player));
	}
	
	public void setDisplayNameAboveHead(String name) throws IOException {
		if(name.length() > 16) throw new IOException("Name cannot be longer than 16 characters.");
		this.displayName = name;
	}
	
	public void setDisplayName(String name) throws IOException {
		this.setDisplayNameAboveHead(name);
	}

	public void setTablistName(String name) {
		this.tablistName = name;
		this.updateToTablist();
	}
	
	public void reload() {
		if(!this.isDestroyed) {
			PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[] { handle.getId() });
			this.sendPacket(packet);
			this.spawn();
		}	
	}
	
	public void setSkin(String texture, String signature) {
		this.texture = texture.strip();
		this.signature = signature.strip();
	}

	public void removeFromTablist() {
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
		PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.handle.getProfile(), 0,
				EnumGamemode.NOT_SET, CraftChatMessage.fromString(tablistName)[0]);
		@SuppressWarnings("unchecked")
		List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getField(packet, "b");
		players.add(data);
		this.setField(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
		this.setField(packet, "b", players);
		this.sendPacket(packet);
	}

	public void removeFromTablistFor(Player player) {
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
		PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.handle.getProfile(), 0,
				EnumGamemode.NOT_SET, CraftChatMessage.fromString(tablistName)[0]);
		@SuppressWarnings("unchecked")
		List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getField(packet, "b");
		players.add(data);
		this.setField(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
		this.setField(packet, "b", players);
		this.sendPacket(packet, player);
	}
	
	public void updateToTablist() {
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
		PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.handle.getProfile(), 0,
				EnumGamemode.NOT_SET, CraftChatMessage.fromString( ChatColor.BLACK + "" + ChatColor.DARK_GRAY + "NPC: " + ChatColor.stripColor(tablistName))[0]);
		@SuppressWarnings("unchecked")
		List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getField(packet, "b");
		players.add(data);
		this.setField(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME);
		this.setField(packet, "b", players);
		this.sendPacket(packet);
	}

	public void addToTablist() {
		PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
		PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.handle.getProfile(), 0,
				EnumGamemode.NOT_SET, CraftChatMessage.fromString(tablistName)[0]);
		@SuppressWarnings("unchecked")
		List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getField(packet, "b");
		players.add(data);
		this.setField(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
		this.setField(packet, "b", players);
		this.sendPacket(packet);
	}

	public void setEquipment(EquipmentSlot slot, org.bukkit.inventory.ItemStack item) {
		PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
		this.setField(packet, "a", handle.getId());
		this.setField(packet, "b", CraftEquipmentSlot.getNMS(slot));
		this.setField(packet, "c", CraftItemStack.asNMSCopy(item));
		this.sendPacket(packet);
	}
	
	/*
	 * Does NOT delete the Dragons NPC underlying it;
	 * use NPC#remove to do both.
	 */
	public void destroy() {
		registry.unregister(this);
		PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[] { handle.getId() });
		this.removeFromTablist();
		this.sendPacket(packet);
		handle.killEntity();
		this.isDestroyed = true;
	}

	public void setStatus(byte status) {
		PacketPlayOutEntityStatus packet = new PacketPlayOutEntityStatus();
		this.setField(packet, "a", handle.getId());
		this.setField(packet, "b", status);
		this.sendPacket(packet);
	}
	
	public void setStatus(NPCStatus status) {
		this.setStatus((byte) status.getId());
	}

	public void setEffect(MobEffect effect) {
		this.sendPacket(new PacketPlayOutEntityEffect(handle.getId(), effect));
	}

	public void setAnimation(byte animation) {
		PacketPlayOutAnimation packet = new PacketPlayOutAnimation();
		this.setField(packet, "a", handle.getId());
		this.setField(packet, "b", animation);
		this.sendPacket(packet);
	}
	
	public void setAnimation(NPCAnimation animation) {
		this.setAnimation((byte) animation.getId());
	}

	public void teleport(Location location, boolean onGround) {
		PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport();
		this.setField(packet, "a", handle.getId());
		this.setField(packet, "b", location.getX());
		this.setField(packet, "c", location.getY());
		this.setField(packet, "d", location.getZ());
		this.setField(packet, "e", getPacketRotation(location.getYaw()));
		this.setField(packet, "f", getPacketRotation(location.getPitch()));
		this.setField(packet, "g", onGround);
		this.sendPacket(packet);
		this.rotateHead(location.getPitch(), location.getYaw());
		this.location = location;
	}

	public void rotateHead(float pitch, float yaw) {
		PacketPlayOutEntity.PacketPlayOutEntityLook packet = new PacketPlayOutEntity.PacketPlayOutEntityLook(handle.getId(), getPacketRotation(yaw), 
				getPacketRotation(pitch), true);
		PacketPlayOutEntityHeadRotation packet_1 = new PacketPlayOutEntityHeadRotation();
		this.setField(packet_1, "a", handle.getId());
		this.setField(packet_1, "b", getPacketRotation(yaw));
		this.sendPacket(packet);
		this.sendPacket(packet_1);
	}
	
	private byte getPacketRotation(float yawpitch) {
		return (byte) ((int) (yawpitch * 256.0F / 360.0F));
	}

	private Object getField(Object obj, String field_name) {
		try {
			Field field = obj.getClass().getDeclaredField(field_name);
			field.setAccessible(true);
			return field.get(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void setField(Object obj, String field_name, Object value) {
		try {
			Field field = obj.getClass().getDeclaredField(field_name);
			field.setAccessible(true);
			field.set(obj, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendPacket(Packet<?> packet, Player player) {
		if(recipientType == Recipient.LISTED_RECIPIENTS && !recipients.contains(player)) return;
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
	}

	private void sendPacket(Packet<?> packet) {
		World world = getLocation().getWorld();
		for (Player p : (this.recipientType == Recipient.ALL ? Bukkit.getOnlinePlayers() : this.recipients)) {
			if(p.getWorld().equals(world)) {
				this.sendPacket(packet, p);
			}
		}

	}
	
	public NPC getDragonsNPC() {
		return npc;
	}

	public Entity getEntity() {
		return handle.getBukkitEntity();
	}
}