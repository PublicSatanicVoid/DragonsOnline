package mc.dragons.core.gameobject.user;

import static mc.dragons.core.util.BukkitUtil.sync;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import mc.dragons.core.Dragons;
import mc.dragons.core.bridge.Bridge;

/**
 * Manages each player's view of the tablist, sorted by rank and then by name.
 * 
 * @author Bear53 (TabAPI plugin)
 * @author Adam (Heavy modifications)
 *
 */
public class TablistManager {
	private static final int DEFAULT_PING = 10;
	
	private ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
	private Map<Player, ArrayList<PacketContainer>> packetBuffer = new HashMap<>();
	private Map<Player, WrappedGameProfile[]> prevTab = new HashMap<>();
	private Bridge bridge;
	
	public TablistManager(Dragons instance) {
		bridge = instance.getBridge();
	}

	/**
	 * Clear the default tablist for the player
	 * 
	 * @implNote Needs separate logic from {@code clearTab} because we haven't yet
	 *           rendered the tablist
	 * 
	 * @param p
	 */
	public void handleJoin(Player p) {
		WrappedGameProfile[] tab = Bukkit.getOnlinePlayers().stream().filter(t -> p.canSee(t))
				.map(t -> WrappedGameProfile.fromPlayer(p)).collect(Collectors.toList())
				.toArray(new WrappedGameProfile[0]);
		clearTab(p, tab);
	}

	private User[] getUnfilteredTablist() {
		User[] utab = Bukkit.getOnlinePlayers().stream().map(t -> UserLoader.fromPlayer(t)).filter(Objects::nonNull)
				.collect(Collectors.toList()).toArray(new User[0]);
		Arrays.sort(utab, (a, b) -> {
			int rankcmp = b.getRank().compareTo(a.getRank());
			if (rankcmp == 0) {
				return a.getName().compareTo(b.getName());
			}
			return rankcmp;
		});
		return utab;
	}

	/**
	 * Update the tablist for all players
	 * 
	 * @apiNote It is not necessary to run this when a player leaves.
	 */
	public void updateAll() {
		User[] utab = getUnfilteredTablist();
		Bukkit.getOnlinePlayers().forEach(p -> updatePlayerHideInvis(p, utab));
	}

	/**
	 * Update the tablist for all players who satisfy the provided predicate.
	 * 
	 * @param updateIf
	 */
	public void updateAll(Predicate<Player> updateIf) {
		User[] utab = getUnfilteredTablist();
		Bukkit.getOnlinePlayers().stream().filter(updateIf).forEach(p -> updatePlayerHideInvis(p, utab));
	}

	/**
	 * Update the tablist for a given player, setting the entries to the given
	 * users, in the order provided, omitting users the player cannot see.
	 * 
	 * @param p
	 */
	public void updatePlayerHideInvis(Player p, User[] tab) {
		updatePlayer(p, Stream.of(tab).filter(u -> u.getPlayer() == null || p.canSee(u.getPlayer()))
				.collect(Collectors.toList()).toArray(new User[0]));
	}

	/**
	 * Update the tablist for a given player, setting the entries to the given
	 * users, in the order provided.
	 * 
	 * @param p
	 * @param tab
	 */
	public void updatePlayer(Player p, User[] tab) {
		if (!p.isOnline())
			return;
		clearTab(p, prevTab.get(p));
		prevTab.put(p, new WrappedGameProfile[tab.length]);
		for (int i = 0; i < tab.length; i++) {
			User u = tab[i];
			String msg = u.getListName();
			GameProfile gameProfile = new GameProfile(tab[i].getUUID(), u.getName());
			addProperties(gameProfile, u.getUUID());
			WrappedGameProfile wrapped = WrappedGameProfile.fromHandle(gameProfile);
			createAndBufferPacket(p, msg, i, wrapped, true, u.getPlayer() == null ? DEFAULT_PING : bridge.getPing(u.getPlayer()));
		}
		sync(() -> flushPackets(p), 5);
	}

	private void createAndBufferPacket(Player p, String listName, int slotId, WrappedGameProfile gameProfile,
			boolean add, int ping) {
		EnumWrappers.PlayerInfoAction action;
		PacketContainer message = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
		String nameToShow = listName;
		String nameToSortBy = String.format("%03d", slotId);
		if (add) {
			action = EnumWrappers.PlayerInfoAction.ADD_PLAYER;
		} else {
			action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER;
		}
		message.getPlayerInfoAction().write(0, action);
		List<PlayerInfoData> pInfoData = new ArrayList<>();
		if (gameProfile != null) {
			gameProfile = gameProfile.withName(nameToSortBy);
			prevTab.get(p)[slotId] = gameProfile;
			pInfoData.add(new PlayerInfoData(gameProfile, 10, EnumWrappers.NativeGameMode.SURVIVAL,
					WrappedChatComponent.fromText(nameToShow)));
		}
		message.getPlayerInfoDataLists().write(0, pInfoData);
		ArrayList<PacketContainer> packetList = packetBuffer.get(p);
		if (packetList == null) {
			packetList = new ArrayList<>();
			packetBuffer.put(p, packetList);
		}
		packetList.add(message);
	}

	private void addProperties(GameProfile profile, UUID uuid) {
		try {
			URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
			URLConnection uc = url.openConnection();
			uc.setUseCaches(false);
			uc.setDefaultUseCaches(false);
			uc.addRequestProperty("User-Agent", "Mozilla/5.0");
			uc.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
			uc.addRequestProperty("Pragma", "no-cache");
			Scanner sc = new Scanner(uc.getInputStream(), "UTF-8");
			sc.useDelimiter("\\A");
			String json = sc.next();
			sc.close();
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(json);
			JSONArray properties = (JSONArray) ((JSONObject) obj).get("properties");
			for (int i = 0; i < properties.size(); i++) {
				try {
					JSONObject property = (JSONObject) properties.get(i);
					String name = (String) property.get("name");
					String value = (String) property.get("value");
					String signature = property.containsKey("signature") ? (String) property.get("signature") : null;
					if (signature != null) {
						profile.getProperties().put(name, new Property(name, value, signature));
					} else {
						profile.getProperties().put(name, new Property(value, name));
					}
				} catch (Exception e) {
					Bukkit.getLogger().log(Level.WARNING, "Failed to apply auth property", e);
				}
			}
		} catch (Exception exception) {
		}
	}

	private void clearTab(Player p, WrappedGameProfile[] tab) {
		if (!p.isOnline())
			return;
		if (tab != null)
			for (int b = 0; b < tab.length; b++) {
				WrappedGameProfile gameProfile = tab[b];
				String msg = gameProfile.getName();
				createAndBufferPacket(p, msg, b, gameProfile, false, DEFAULT_PING);
			}
		sync(() -> flushPackets(p));
	}

	private void flushPackets(final Player p) {
		if (!packetBuffer.containsKey(p) || packetBuffer.get(p) == null)
			return;
		final PacketContainer[] packets = packetBuffer.get(p).toArray(new PacketContainer[0]);
		sync(() -> {
			if (p.isOnline()) {
				for (PacketContainer packet : packets) {
					try {
						protocolManager.sendServerPacket(p, packet);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
						Bukkit.getLogger().warning("Could not update tablist for " + p.getName());
					}
				}
			}
		}, 5);
		packetBuffer.remove(p);
	}
}
