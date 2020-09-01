package mc.dragons.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

public class StringUtil {
	public static String locToString(Location loc) {
		return String.valueOf(MathUtil.round(loc.getX())) + ", " + MathUtil.round(loc.getY()) + ", " + MathUtil.round(loc.getZ());
	}

	public static String vecToString(Vector vec) {
		return String.valueOf(MathUtil.round(vec.getX())) + ", " + MathUtil.round(vec.getY()) + ", " + MathUtil.round(vec.getZ());
	}

	public static String docToString(Document doc) {
		String result = "";
		for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) doc.entrySet())
			result = String.valueOf(result) + entry.getKey() + "=" + entry.getValue().toString() + "; ";
		if (result.equals(""))
			return "";
		return result.substring(0, result.length() - 1);
	}

	public static String entityToString(Entity e) {
		return e.getType() + " " + e + " (#" + e.getEntityId() + ")";
	}

	public static String concatArgs(String[] args, int startIndex, int endIndex) {
		if (endIndex <= startIndex)
			return "";
		if (startIndex >= args.length)
			return "";
		String result = "";
		for (int i = startIndex; i < Math.min(endIndex, args.length); i++)
			result = String.valueOf(result) + args[i] + " ";
		return result.trim();
	}

	public static String concatArgs(String[] args, int startIndex) {
		return concatArgs(args, startIndex, args.length);
	}

	public static int getFlagIndex(String[] args, String flag, int startIndex) {
		for (int i = startIndex; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(flag))
				return i;
		}
		return -1;
	}

	public static long parseTimespanToSeconds(String timespan) {
		if (timespan.equals(""))
			return -1L;
		long timespanSeconds = 0L;
		int buffer = 0;
		char[] chars = timespan.toCharArray();
		for(char ch : chars) {
			switch (ch) {
			case 'y':
				timespanSeconds += (31536000 * buffer);
				buffer = 0;
				break;
			case 'w':
				timespanSeconds += (604800 * buffer);
				buffer = 0;
				break;
			case 'd':
				timespanSeconds += (86400 * buffer);
				buffer = 0;
				break;
			case 'h':
				timespanSeconds += (3600 * buffer);
				buffer = 0;
				break;
			case 'm':
				timespanSeconds += (60 * buffer);
				buffer = 0;
				break;
			case 's':
				timespanSeconds += buffer;
				buffer = 0;
				break;
			default:
				buffer *= 10;
				buffer += Integer.parseInt(new String(new char[] { ch }));
				break;
			}
		}
		return timespanSeconds;
	}

	public static String parseSecondsToTimespan(long seconds) {
		int days = (int) Math.floor((seconds / 86400L));
		seconds %= 86400L;
		int hours = (int) Math.floor((seconds / 3600L));
		seconds %= 3600L;
		int minutes = (int) Math.floor((seconds / 60L));
		seconds %= 60L;
		return String.valueOf(days) + "d " + hours + "h " + minutes + "m " + seconds + "s";
	}

	public static <T> String parseList(List<T> list) {
		return parseList(list, ", ");
	}

	public static <T> String parseList(List<T> list, String separator) {
		return list.stream().map(elem -> elem.toString()).collect(Collectors.joining(separator));
	}

	public static <T> String parseList(Object[] array) {
		return parseList(array, ", ");
	}

	public static <T> String parseList(Object[] array, String separator) {
		return parseList(Arrays.asList(array), separator);
	}

	public static Material parseMaterialType(CommandSender sender, String str) {
		Material type = null;
		try {
			type = Material.valueOf(str.toUpperCase());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid material type! For a full list, see " + ChatColor.UNDERLINE + "https://papermc.io/javadocs/paper/1.12/org/bukkit/Material.html");
		}
		return type;
	}

	public static EntityType parseEntityType(CommandSender sender, String str) {
		EntityType type = null;
		try {
			type = EntityType.valueOf(str.toUpperCase());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid entity type! For a full list, see " + ChatColor.UNDERLINE + "https://papermc.io/javadocs/paper/1.12/org/bukkit/entity/EntityType.html");
		}
		return type;
	}

	public static String toHdFont(String input) {
		String returnString = "";
		boolean skip = false;
		boolean skipNext = false;
		for (int t = 0; t < input.length(); t++) {
			if (input.charAt(t) == '§') {
				skip = true;
				skipNext = true;
			}
			if (!skip) {
				if (input.charAt(t) == '/') {
					returnString = String.valueOf(String.valueOf(returnString)) + '/';
				} else if (input.charAt(t) == ' ') {
					returnString = String.valueOf(String.valueOf(returnString)) + ' ';
				} else if (input.charAt(t) == '[') {
					returnString = String.valueOf(String.valueOf(returnString)) + '[';
				} else if (input.charAt(t) == ']') {
					returnString = String.valueOf(String.valueOf(returnString)) + ']';
				} else if (input.charAt(t) == '(') {
					returnString = String.valueOf(String.valueOf(returnString)) + '(';
				} else if (input.charAt(t) == ')') {
					returnString = String.valueOf(String.valueOf(returnString)) + ')';
				} else if (input.charAt(t) <= '' && input.charAt(t) >= ' ') {
					returnString = String.valueOf(String.valueOf(returnString)) + (char) (65248 + input.charAt(t));
				}
			} else {
				returnString = String.valueOf(String.valueOf(returnString)) + input.charAt(t);
				if (!skipNext) {
					skip = false;
				} else {
					skipNext = false;
				}
			}
		}
		return returnString;
	}
}
