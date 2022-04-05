package mc.dragons.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import mc.dragons.core.Dragons;
import mc.dragons.core.exception.DragonsInternalException;

/**
 * 
 * @author comphenix
 * @author Adam
 */
public class NametagUtil {
	private static PacketContainer createPacket(Player player, ChatColor nameColor, String prefix, String suffix) {
		PacketContainer packet = ProtocolLibrary.getProtocolManager()
				.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
		String name = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		packet.getIntegers().write(1, 0);
		packet.getStrings().write(0, name);
		packet.getEnumModifier(ChatColor.class, MinecraftReflection.getMinecraftClass("EnumChatFormat")).write(0, nameColor);
		packet.getChatComponents().write(0, WrappedChatComponent.fromText(player.toString()));
		packet.getSpecificModifier(Collection.class).write(0, Collections.singletonList(player.getName()));
		packet.getChatComponents().write(1,
				WrappedChatComponent.fromText(ChatColor.translateAlternateColorCodes('&', prefix) + " "));
		packet.getChatComponents().write(2,
				WrappedChatComponent.fromText(" " + ChatColor.translateAlternateColorCodes('&', suffix)));
		return packet;
	}
	
	public static void setNameTag(Player player, ChatColor nameColor, String prefix, String suffix) {
		PacketContainer packet = createPacket(player, nameColor, prefix, suffix);
		for (Player p : Bukkit.getOnlinePlayers()) {
			if(!p.canSee(player)) continue;
			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
			} catch (InvocationTargetException e) {
				throw new DragonsInternalException("Cannot send packet " + packet, e);
			}
		}
	}
	
	public static void updateNameTagFor(Player of, Player to, ChatColor nameColor, String prefix, String suffix) {
		PacketContainer packet = createPacket(of, nameColor, prefix, suffix);
		if(!to.canSee(of))
			throw new DragonsInternalException("Tried to update name tag for player who cannot see them", Dragons.getInstance().getLogger().newCID());
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(to, packet);
		} catch (InvocationTargetException e) {
			throw new DragonsInternalException("Cannot send packet " + packet, e);
		}
	}
}
