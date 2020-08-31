package mc.dragons.core.util;

import java.nio.charset.Charset;

import org.bukkit.ChatColor;

public class HiddenStringUtil {
	private static final String SEQUENCE_HEADER = ChatColor.RESET + stringToColors("SeqHdr") + ChatColor.RESET;

	private static final String SEQUENCE_FOOTER = ChatColor.RESET + stringToColors("SeqFtr") + ChatColor.RESET;

	public static String encodeString(String hiddenString) {
		return quote(stringToColors(hiddenString));
	}

	public static boolean hasHiddenString(String input) {
		if (input == null)
			return false;
		return (input.indexOf(SEQUENCE_HEADER) > -1 && input.indexOf(SEQUENCE_FOOTER) > -1);
	}

	public static String extractHiddenString(String input) {
		return colorsToString(extract(input));
	}

	public static String replaceHiddenString(String input, String hiddenString) {
		if (input == null)
			return null;
		int start = input.indexOf(SEQUENCE_HEADER);
		int end = input.indexOf(SEQUENCE_FOOTER);
		if (start < 0 || end < 0)
			return null;
		return String.valueOf(input.substring(0, start + SEQUENCE_HEADER.length())) + stringToColors(hiddenString) + input.substring(end, input.length());
	}

	private static String quote(String input) {
		if (input == null)
			return null;
		return String.valueOf(SEQUENCE_HEADER) + input + SEQUENCE_FOOTER;
	}

	private static String extract(String input) {
		if (input == null)
			return null;
		int start = input.indexOf(SEQUENCE_HEADER);
		int end = input.indexOf(SEQUENCE_FOOTER);
		if (start < 0 || end < 0)
			return null;
		return input.substring(start + SEQUENCE_HEADER.length(), end);
	}

	private static String stringToColors(String normal) {
		if (normal == null)
			return null;
		byte[] bytes = normal.getBytes(Charset.forName("UTF-8"));
		char[] chars = new char[bytes.length * 4];
		for (int i = 0; i < bytes.length; i++) {
			char[] hex = byteToHex(bytes[i]);
			chars[i * 4] = '§';
			chars[i * 4 + 1] = hex[0];
			chars[i * 4 + 2] = '§';
			chars[i * 4 + 3] = hex[1];
		}
		return new String(chars);
	}

	private static String colorsToString(String colors) {
		if (colors == null)
			return null;
		colors = colors.toLowerCase().replace("§", "");
		if (colors.length() % 2 != 0)
			colors = colors.substring(0, colors.length() / 2 * 2);
		char[] chars = colors.toCharArray();
		byte[] bytes = new byte[chars.length / 2];
		for (int i = 0; i < chars.length; i += 2)
			bytes[i / 2] = hexToByte(chars[i], chars[i + 1]);
		return new String(bytes, Charset.forName("UTF-8"));
	}

	private static int hexToUnsignedInt(char c) {
		if (c >= '0' && c <= '9')
			return c - 48;
		if (c >= 'a' && c <= 'f')
			return c - 87;
		throw new IllegalArgumentException("Invalid hex char: out of range");
	}

	private static char unsignedIntToHex(int i) {
		if (i >= 0 && i <= 9)
			return (char) (i + 48);
		if (i >= 10 && i <= 15)
			return (char) (i + 87);
		throw new IllegalArgumentException("Invalid hex int: out of range");
	}

	private static byte hexToByte(char hex1, char hex0) {
		return (byte) ((hexToUnsignedInt(hex1) << 4 | hexToUnsignedInt(hex0)) + -128);
	}

	private static char[] byteToHex(byte b) {
		int unsignedByte = b - -128;
		return new char[] { unsignedIntToHex(unsignedByte >> 4 & 0xF), unsignedIntToHex(unsignedByte & 0xF) };
	}
}
