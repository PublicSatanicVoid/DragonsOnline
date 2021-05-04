package mc.dragons.core.util;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Utilities related to string manipulation and object representation.
 * 
 * @author Adam
 *
 */
public class StringUtil {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	
	public static String locToString(Location loc) {
		return String.valueOf(MathUtil.round(loc.getX())) + ", " + MathUtil.round(loc.getY()) + ", " + MathUtil.round(loc.getZ());
	}

	public static String vecToString(Vector vec) {
		return String.valueOf(MathUtil.round(vec.getX())) + ", " + MathUtil.round(vec.getY()) + ", " + MathUtil.round(vec.getZ());
	}

	public static String docToString(Document doc) {
		String result = "";
		for (Entry<String, Object> entry : (Iterable<Entry<String, Object>>) doc.entrySet()) {
			result = result + entry.getKey() + "=" + entry.getValue().toString() + "; ";
		}
		if (result.equals("")) {
			return "";
		}
		return result.substring(0, result.length() - 1);
	}

	public static String entityToString(Entity e) {
		if(e == null) return "NULL";
		return e.getType() + " " + e + " (#" + e.getEntityId() + ")";
	}

	public static String concatArgs(String[] args, int startIndex, int endIndex) {
		if (endIndex <= startIndex) {
			return "";
		}
		if (startIndex >= args.length) {
			return "";
		}
		String result = "";
		for (int i = startIndex; i < Math.min(endIndex, args.length); i++) {
			result = result + args[i] + " ";
		}
		return result.trim();
	}

	public static String concatArgs(String[] args, int startIndex) {
		return concatArgs(args, startIndex, args.length);
	}

	public static int getFlagIndex(String[] args, String flag, int startIndex) {
		for (int i = startIndex; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(flag)) {
				return i;
			}
		}
		return -1;
	}

	public static long parseTimespanToSeconds(String timespan) {
		if (timespan.equals("")) {
			return -1L;
		}
		long timespanSeconds = 0L;
		int buffer = 0;
		char[] chars = timespan.toCharArray();
		for(char ch : chars) {
			switch (ch) {
			case 'y':
				timespanSeconds += 31536000 * buffer;
				buffer = 0;
				break;
			case 'w':
				timespanSeconds += 604800 * buffer;
				buffer = 0;
				break;
			case 'd':
				timespanSeconds += 86400 * buffer;
				buffer = 0;
				break;
			case 'h':
				timespanSeconds += 3600 * buffer;
				buffer = 0;
				break;
			case 'm':
				timespanSeconds += 60 * buffer;
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
		long remaining = seconds;
		int days = (int) Math.floor(remaining / 86400L);
		String sDays = days == 0 ? "" : days + "d ";
		remaining %= 86400L;
		int hours = (int) Math.floor(remaining / 3600L);
		String sHours = hours == 0 ? "" : hours + "h ";
		remaining %= 3600L;
		int minutes = (int) Math.floor(remaining / 60L);
		String sMinutes = minutes == 0 ? "" : minutes + "m ";
		remaining %= 60L;
		return sDays + sHours + sMinutes + remaining + "s";
	}

	public static <T> String parseList(List<T> list) {
		return parseList(list, ", ");
	}

	public static <T> String parseList(List<T> list, String separator) {
		return list.stream().map(elem -> elem.toString()).collect(Collectors.joining(separator));
	}

	public static <T> String parseList(T[] array) {
		return parseList(array, ", ");
	}

	public static <T> String parseList(T[] array, String separator) {
		return parseList(Arrays.asList(array), separator);
	}

	public static Material parseMaterialType(CommandSender sender, String str) {
		try {
			return Material.valueOf(str.toUpperCase());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid material type! For a full list, see " + ChatColor.UNDERLINE + "https://papermc.io/javadocs/paper/1.16/org/bukkit/Material.html");
		}
		return null;
	}

	public static EntityType parseEntityType(CommandSender sender, String str) {
		try {
			return EntityType.valueOf(str.toUpperCase());
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid entity type! For a full list, see " + ChatColor.UNDERLINE + "https://papermc.io/javadocs/paper/1.16/org/bukkit/entity/EntityType.html");
		}
		return null;
	}
	
	public static <T extends Enum<T>> T parseEnum(CommandSender sender, Class<T> enumClass, String str) {
		try {
			return Enum.valueOf(enumClass, str);
		}
		catch(Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid entry! Valid types are " + parseList(enumClass.getEnumConstants()));
		}
		
		return null;
	}
	
	public static ChatColor parseChatColor(CommandSender sender, String str) {
		try {
			return ChatColor.valueOf(str.toUpperCase());
		}
		catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid chat color! Valid colors are " + 
				Arrays.stream(ChatColor.values())
					.filter(c -> c.isColor())
					.map(c -> c + c.name())
					.reduce((a,b) -> a + ChatColor.GRAY + ", " + b)
					.get());
		}
		return null;
	}
	
	public static UUID parseUUID(CommandSender sender, String str) {
		try {
			return UUID.fromString(str);
		}
		catch(Exception e) {
			sender.sendMessage(ChatColor.RED + "Invalid UUID! An example UUID is " + UUID.randomUUID());
		}
		return null;
	}
	
	public static String dateFormatNow() {
		return DATE_FORMAT.format(Date.from(Instant.now()));
	}

	public static String encodeBase64(String str) {
		return Base64.getEncoder().encodeToString(str.getBytes());
	}
	
	public static String decodeBase64(String str) {
		return new String(Base64.getDecoder().decode(str));
	}
	
	// Decompiled from the public HDFont plugin.
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
					returnString += '/';
				} else if (input.charAt(t) == ' ') {
					returnString += ' ';
				} else if (input.charAt(t) == '[') {
					returnString += '[';
				} else if (input.charAt(t) == ']') {
					returnString += ']';
				} else if (input.charAt(t) == '(') {
					returnString += '(';
				} else if (input.charAt(t) == ')') {
					returnString += ')';
				} else if (input.charAt(t) <= '' && input.charAt(t) >= ' ') {
					returnString += (char) (65248 + input.charAt(t));
				}
			} else {
				returnString += input.charAt(t);
				if (!skipNext) {
					skip = false;
				} else {
					skipNext = false;
				}
			}
		}
		return returnString;
	}

	public static boolean equalsAnyIgnoreCase(String input, String... tests) {
		for(String test : tests) {
			if(input.equalsIgnoreCase(test)) return true;
		}
		return false;
	}
	
	public static TextComponent clickableHoverableText(String text, String command, String...hover ) {
		return clickableHoverableText(text, command, false, hover);
	}
	
	public static TextComponent clickableHoverableText(String text, String command, boolean suggestCommandOnly, String... hover) {
		TextComponent tc = new TextComponent(text);
		Text hoverText = new Text(StringUtil.parseList(hover, "\n"));
		tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
		if(suggestCommandOnly) {
			tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
		}
		else {
			tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
		}
		return tc;
	}
	
	public static TextComponent hoverableText(String text, String... hover) {
		TextComponent tc = new TextComponent(text);
		tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(StringUtil.parseList(hover, "\n"))));
		return tc;
	}
	
	public static String truncateWithEllipsis(String text, int length) {
		if(text.length() <= length) return text;
		return text.substring(0, length - 3) + "...";
	}
	
	public static boolean isIntegral(String str) {
		try {
			Integer.parseInt(str);
			return true;
		}
		catch(Exception e) {
			return false;
		}
	}

	public static String colorize(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}
}
