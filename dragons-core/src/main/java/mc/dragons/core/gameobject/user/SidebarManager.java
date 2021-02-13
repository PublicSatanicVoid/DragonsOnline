package mc.dragons.core.gameobject.user;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.floor.FloorLoader;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.region.RegionLoader;

/**
 * The sidebar displays contextual information to the user.
 * This is non-trivial to implement as it is essentially a
 * (mis)use of Minecraft's scoreboard functionality.
 * 
 * @author Adam
 *
 */
public class SidebarManager {
	private Dragons instance;
	
	// Implement scrolling text when we overflow the character limit per row.
	private Map<Player, Map<String, Integer>> scoreboardScrollIndices = new HashMap<>();

	public SidebarManager(Dragons instance) {
		this.instance = instance;
	}

	private String getScrolledFrameAndIncrement(Player player, String entry, String value) {
		if (value.length() <= 16) {
			return value;
		}
		if (!scoreboardScrollIndices.containsKey(player)) {
			scoreboardScrollIndices.put(player, new HashMap<>());
		}
		int shift = scoreboardScrollIndices.get(player).getOrDefault(entry, Integer.valueOf(0)).intValue();
		if (shift + 16 > value.length()) {
			shift = 0;
		}
		scoreboardScrollIndices.get(player).put(entry, Integer.valueOf(shift + 1));
		return value.substring(shift, 16 + shift);
	}

	public Scoreboard createScoreboard(Player player) {
		if (player == null) {
			return null;
		}
		String[] scoreboardLayout = {
			"FLOOR", 
			"REGION", 
			"LOCATION", 
			"LEVEL", 
			"XP", 
			"RANK", 
			"GOLD", 
			"ONLINE", 
			"SERVER" 
		};
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		player.setScoreboard(scoreboard);
		Objective objective = scoreboard.registerNewObjective("CustomObjective", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dragons" + ChatColor.LIGHT_PURPLE + " " + ChatColor.BOLD + "Online");
		for (int i = scoreboardLayout.length; i > 0; i--) {
			String scoreName = scoreboardLayout[scoreboardLayout.length - i];
			Team team = scoreboard.registerNewTeam(scoreName);
			String entryName = getEntryString(i);
			team.addEntry(entryName);
			team.setPrefix("Loading...");
			Score score = objective.getScore(entryName);
			score.setScore(i);
		}
		return scoreboard;
	}

	public void updateScoreboard(Player player) {
		if (player == null) {
			return;
		}
		Scoreboard scoreboard = player.getScoreboard();
		if (scoreboard == null) {
			instance.getLogger().warning("Attempted to update scoreboard for " + player.getName() + " but they did not have a scoreboard");
			return;
		}
		RegionLoader regionLoader = GameObjectType.REGION.<Region, RegionLoader>getLoader();
		User user = UserLoader.fromPlayer(player);
		Team floor = scoreboard.getTeam("FLOOR");
		floor.setPrefix(ChatColor.GRAY + "Floor: ");
		Floor currentFloor = FloorLoader.fromWorld(player.getWorld());
		String floorName = currentFloor == null ? ChatColor.RED + "Unknown" : currentFloor.getDisplayName();
		floor.setSuffix(getScrolledFrameAndIncrement(player, "Floor", floorName));
		Team server = scoreboard.getTeam("SERVER");
		server.setPrefix(ChatColor.GRAY + "Server: " + ChatColor.WHITE);
		server.setSuffix(instance.getServerName());
		Team online = scoreboard.getTeam("ONLINE");
		online.setPrefix(ChatColor.GRAY + "Online: " + ChatColor.WHITE);
		online.setSuffix(String.valueOf(Bukkit.getOnlinePlayers().stream().filter(p -> p.canSee(p)).count()) + " / " + Bukkit.getMaxPlayers());
		Team level = scoreboard.getTeam("LEVEL");
		level.setPrefix(ChatColor.GRAY + "Level: " + ChatColor.WHITE);
		level.setSuffix((user.getLevelColor() == ChatColor.GRAY ? "" : user.getLevelColor().toString()) + user.getLevel());
		Team xp = scoreboard.getTeam("XP");
		xp.setPrefix(ChatColor.GRAY + "XP: " + ChatColor.WHITE);
		xp.setSuffix(String.valueOf(user.getXP()) + " (" + (int) Math.floor(user.getLevelProgress() * 100.0F) + "%)");
		Team rank = scoreboard.getTeam("RANK");
		rank.setPrefix(ChatColor.GRAY + "Rank: " + ChatColor.WHITE);
		rank.setSuffix((user.getRank().getNameColor() == ChatColor.GRAY ? "" : user.getRank().getNameColor()) + user.getRank().getShortName());
		Team gold = scoreboard.getTeam("GOLD");
		gold.setPrefix(ChatColor.GRAY + "Gold: " + ChatColor.WHITE);
		gold.setSuffix(ChatColor.GOLD + "" + user.getGold());
		Team location = scoreboard.getTeam("LOCATION");
		location.setPrefix(ChatColor.GRAY + "Location: " + ChatColor.WHITE);
		location.setSuffix(String.valueOf(player.getLocation().getBlockX()) + ", " + player.getLocation().getBlockZ());
		Team region = scoreboard.getTeam("REGION");
		Region smallestRegion = regionLoader.getSmallestRegionByLocation(player.getLocation(), false);
		String regionName = smallestRegion == null ? "None" : smallestRegion.getFlags().getString("fullname");
		region.setPrefix(ChatColor.GRAY + "Region: " + ChatColor.WHITE);
		region.setSuffix(getScrolledFrameAndIncrement(player, "Region", regionName));
	}

	private String getEntryString(int index) {
		String result = "";
		int len = ChatColor.values().length;
		while (index > 0) {
			result = String.valueOf(result) + ChatColor.values()[index % len];
			index -= len;
		}
		return String.valueOf(result) + ChatColor.RESET;
	}
}
