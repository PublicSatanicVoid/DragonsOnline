package mc.dragons.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * https://github.com/FisheyLP/TableGenerator/blob/master/TableGenerator.java\
 * Copyright by FisheyLP, Version 1.3 (12.08.16)
 * 
 * @author FisheyLP
 * @author Adam (minor modifications)
 *
 */
public class TableGenerator {

	private static String delimiter = "  ";
	private static List<Character> char7 = Arrays.asList('°', '~', '@');
	private static List<Character> char5 = Arrays.asList('"', '{', '}', '(', ')', '*', 'f', 'k', '<', '>');
	private static List<Character> char4 = Arrays.asList('I', 't', ' ', '[', ']', '€');
	private static List<Character> char3 = Arrays.asList('l', '`', '³', '\'');
	private static List<Character> char2 = Arrays.asList(',', '.', '!', 'i', '´', ':', ';', '|');
	private static char char1 = '\u17f2';
	private static Pattern regex = Pattern.compile(char1 + "(?:§r)?(\\s*)" + "(?:§r§0)?" + char1 + "(?:§r)?(\\s*)" + "(?:§r§0)?" + char1 + "(?:§r)?(\\s*)" + "(?:§r§0)?" + char1);
	private static String colors = "[&§][0-9a-fA-Fk-oK-OrR]";
	private Alignment[] alignments;
	private List<Row> table = new ArrayList<>();
	private int columns;

	public TableGenerator(Alignment... alignments) {
		if (alignments == null || alignments.length < 1)
			throw new IllegalArgumentException("Must atleast provide 1 alignment.");

		this.columns = alignments.length;
		this.alignments = alignments;
	}

	public List<TextComponent> generate(Receiver receiver, boolean ignoreColors, boolean coloredDistances) {
		if (receiver == null) {
			throw new IllegalArgumentException("Receiver must not be null.");
		}

		Integer[] columWidths = new Integer[columns];

		for (Row r : table) {
			for (int i = 0; i < columns; i++) {
				String text = r.texts.get(i);
				int length;

				if (ignoreColors)
					length = getCustomLength(text.replaceAll(colors, ""), receiver);
				else
					length = getCustomLength(text, receiver);

				if (columWidths[i] == null) {
					columWidths[i] = length;
				}

				else if (length > columWidths[i]) {
					columWidths[i] = length;
				}
			}
		}

		List<TextComponent> lines = new ArrayList<>();

		for (Row r : table) {
			StringBuilder sb = new StringBuilder();

			if (r.empty) {
				lines.add(new TextComponent(""));
				continue;
			}

			for (int i = 0; i < columns; i++) {
				Alignment agn = alignments[i];
				String text = r.texts.get(i);
				int length;

				if (ignoreColors)
					length = getCustomLength(text.replaceAll(colors, ""), receiver);
				else
					length = getCustomLength(text, receiver);

				int empty = columWidths[i] - length;
				int spacesAmount = empty;
				if (receiver == Receiver.CLIENT)
					spacesAmount = (int) Math.floor(empty / 4d);
				int char1Amount = 0;
				if (receiver == Receiver.CLIENT)
					char1Amount = empty - 4 * spacesAmount;

				String spaces = concatChars(' ', spacesAmount);
				String char1s = concatChars(char1, char1Amount);

				if (coloredDistances)
					char1s = "§r§0" + char1s + "§r";

				if (agn == Alignment.LEFT) {
					sb.append(text);
					if (i < columns - 1)
						sb.append(char1s).append(spaces);
				}
				if (agn == Alignment.RIGHT) {
					sb.append(spaces).append(char1s).append(text);
				}
				if (agn == Alignment.CENTER) {
					int leftAmount = empty / 2;
					int rightAmount = empty - leftAmount;

					int spacesLeftAmount = leftAmount;
					int spacesRightAmount = rightAmount;
					if (receiver == Receiver.CLIENT) {
						spacesLeftAmount = (int) Math.floor(spacesLeftAmount / 4d);
						spacesRightAmount = (int) Math.floor(spacesRightAmount / 4d);
					}

					int char1LeftAmount = 0;
					int char1RightAmount = 0;
					if (receiver == Receiver.CLIENT) {
						char1LeftAmount = leftAmount - 4 * spacesLeftAmount;
						char1RightAmount = rightAmount - 4 * spacesRightAmount;
					}

					String spacesLeft = concatChars(' ', spacesLeftAmount);
					String spacesRight = concatChars(' ', spacesRightAmount);
					String char1Left = concatChars(char1, char1LeftAmount);
					String char1Right = concatChars(char1, char1RightAmount);

					if (coloredDistances) {
						char1Left = "§r§0" + char1Left + "§r";
						char1Right = "§r§0" + char1Right + "§r";
					}

					sb.append(spacesLeft).append(char1Left).append(text);
					if (i < columns - 1)
						sb.append(char1Right).append(spacesRight);
				}

				if (i < columns - 1)
					sb.append("§r" + delimiter);
			}

			String line = sb.toString();
			if (receiver == Receiver.CLIENT) {
				for (int i = 0; i < 2; i++) {
					Matcher matcher = regex.matcher(line);
					line = matcher.replaceAll("$1$2$3 ").replace("§r§0§r", "§r").replaceAll("§r(\\s*)§r", "§r$1");
				}
			}
			TextComponent tc = new TextComponent(line);
			if(r.command != null || r.hover != null) {
				tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, r.command));
				tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(r.hover)));
			}
			lines.add(tc);
		}
		return lines;
	}

	protected static int getCustomLength(String text, Receiver receiver) {
		if (text == null) {
			throw new IllegalArgumentException("Text must not be null.");
		}
		if (receiver == null) {
			throw new IllegalArgumentException("Receiver must not be null.");
		}
		if (receiver == Receiver.CONSOLE)
			return text.length();

		int length = 0;
		for (char c : text.toCharArray())
			length += getCustomCharLength(c);

		return length;
	}

	protected static int getCustomCharLength(char c) {
		if (char1 == c)
			return 1;
		if (char2.contains(c))
			return 2;
		if (char3.contains(c))
			return 3;
		if (char4.contains(c))
			return 4;
		if (char5.contains(c))
			return 5;
		if (char7.contains(c))
			return 7;

		return 6;
	}

	protected String concatChars(char c, int length) {
		String s = "";
		if (length < 1)
			return s;

		for (int i = 0; i < length; i++)
			s += Character.toString(c);
		return s;
	}

	public void addRow(String... texts) {
		if (texts == null) {
			throw new IllegalArgumentException("Texts must not be null.");
		}
		if (texts != null && texts.length > columns) {
			throw new IllegalArgumentException("Too big for the table.");
		}

		Row r = new Row(texts);

		table.add(r);
	}
	
	public void addRowEx(String command, String hover, String... texts) {
		if (texts == null) {
			throw new IllegalArgumentException("Texts must not be null.");
		}
		if (texts != null && texts.length > columns) {
			throw new IllegalArgumentException("Too big for the table.");
		}

		Row r = new Row(command, hover, texts);

		table.add(r);
	}
	
	public void display(CommandSender sender) {
		Receiver receiver = sender instanceof Player ? Receiver.CLIENT : Receiver.CONSOLE;
		for(TextComponent line : generate(receiver, true, true)) {
			sender.spigot().sendMessage(line);
		}
	}

	private class Row {

		public List<String> texts = new ArrayList<>();
		public String command;
		public String hover;
		public boolean empty = true;

		public Row(String... texts) {
			if (texts == null) {
				for (int i = 0; i < columns; i++)
					this.texts.add("");
				return;
			}

			for (String text : texts) {
				if (text != null && !text.isEmpty())
					empty = false;

				this.texts.add(text);
			}

			for (int i = 0; i < columns; i++) {
				if (i >= texts.length)
					this.texts.add("");
			}
		}
		
		public Row(String command, String hover, String... texts) {
			this(texts);
			this.command = command;
			this.hover = hover;
		}
	}

	public enum Receiver {

		CONSOLE, CLIENT
	}

	public enum Alignment {

		CENTER, LEFT, RIGHT
	}
}