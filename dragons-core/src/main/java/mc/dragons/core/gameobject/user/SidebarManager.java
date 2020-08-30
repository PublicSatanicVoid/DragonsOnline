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
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.region.Region;

public class SidebarManager {
	private Dragons instance;

	private Map<Player, Map<String, Integer>> scoreboardScrollIndices = new HashMap<>();

	public SidebarManager(Dragons instance) {
		this.instance = instance;
	}

	private String getScrolledFrameAndIncrement(Player player, String entry, String value) {
		if (value.length() <= 16)
			return value;
		if (!this.scoreboardScrollIndices.containsKey(player))
			this.scoreboardScrollIndices.put(player, new HashMap<>());
		int shift = this.scoreboardScrollIndices.get(player).getOrDefault(entry, Integer.valueOf(0)).intValue();
		if (shift + 16 > value.length())
			shift = 0;
		((Map<String, Integer>) this.scoreboardScrollIndices.get(player)).put(entry, Integer.valueOf(shift + 1));
		return value.substring(shift, 16 + shift);
	}

	public Scoreboard createScoreboard(Player player) {
		if (player == null)
			return null;
		String[] scoreboardLayout = { "FLOOR", "REGION", "LOCATION", "LEVEL", "XP", "RANK", "GOLD", "ONLINE", "SERVER" };
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
		if (player == null)
			return;
		Scoreboard scoreboard = player.getScoreboard();
		if (scoreboard == null) {
			this.instance.getLogger().warning("Attempted to update scoreboard for " + player.getName() + " but they did not have a scoreboard");
			return;
		}
		RegionLoader regionLoader = (RegionLoader) GameObjectType.REGION.<Region, RegionLoader>getLoader();
		User user = UserLoader.fromPlayer(player);
		Team floor = scoreboard.getTeam("FLOOR");
		floor.setPrefix(ChatColor.GRAY + "Floor: ");
		Floor currentFloor = FloorLoader.fromWorld(player.getWorld());
		String floorName = (currentFloor == null) ? (ChatColor.RED + "E R R O R") : currentFloor.getDisplayName();
		floor.setSuffix(getScrolledFrameAndIncrement(player, "Floor", floorName));
		Team server = scoreboard.getTeam("SERVER");
		server.setPrefix(ChatColor.GRAY + "Server: ");
		server.setSuffix(this.instance.getServerName());
		Team online = scoreboard.getTeam("ONLINE");
		online.setPrefix(ChatColor.GRAY + "Online: ");
		online.setSuffix(String.valueOf(Bukkit.getOnlinePlayers().stream().filter(p -> p.canSee(p)).count()) + " / " + Bukkit.getMaxPlayers());
		Team level = scoreboard.getTeam("LEVEL");
		level.setPrefix(ChatColor.GRAY + "Level: ");
		level.setSuffix(((user.getLevelColor() == ChatColor.GRAY) ? "" : user.getLevelColor().toString()) + user.getLevel());
		Team xp = scoreboard.getTeam("XP");
		xp.setPrefix(ChatColor.GRAY + "XP: ");
		xp.setSuffix(String.valueOf(user.getXP()) + " (" + (int) Math.floor((user.getLevelProgress() * 100.0F)) + "%)");
		Team rank = scoreboard.getTeam("RANK");
		rank.setPrefix(ChatColor.GRAY + "Rank: ");
		rank.setSuffix(user.getRank().getNameColor() + user.getRank().getShortName());
		Team gold = scoreboard.getTeam("GOLD");
		gold.setPrefix(ChatColor.GRAY + "Gold: ");
		gold.setSuffix(ChatColor.GOLD + "" + user.getGold());
		Team location = scoreboard.getTeam("LOCATION");
		location.setPrefix(ChatColor.GRAY + "Location: ");
		location.setSuffix(String.valueOf(player.getLocation().getBlockX()) + ", " + player.getLocation().getBlockZ());
		Team region = scoreboard.getTeam("REGION");
		Region smallestRegion = regionLoader.getSmallestRegionByLocation(player.getLocation(), false);
		String regionName = (smallestRegion == null) ? "None" : smallestRegion.getFlags().getString("fullname");
		region.setPrefix(ChatColor.GRAY + "Region: ");
		region.setSuffix(getScrolledFrameAndIncrement(player, "Region", regionName));
	}

	private String getEntryString(int index) {
		String result = "";
		int len = (ChatColor.values()).length;
		while (index > 0) {
			result = String.valueOf(result) + ChatColor.values()[index % len];
			index -= len;
		}
		return String.valueOf(result) + ChatColor.RESET;
	}
}
