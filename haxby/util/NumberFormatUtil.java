package haxby.util;

public class NumberFormatUtil {
	private static String[] suffixes = new String[] {
		"th", "st", "nd", "rd"
	};
	
	private static String getSuffix(int num) {
		if(num < 0) num = -num;
		if(num%100 > 10 && num%100 < 20) {
			return "th";
		}
		else if(num % 10 < suffixes.length) {
			return suffixes[num % 10];
		}
		return "th";
	}
	public static String cardinalToOrdinal(int num) {
		String prefix = (num < 0)?("-"):("");
		if(num < 0) num = -num;
		String suffix = getSuffix(num);
		return prefix + num + suffix;
	}
}
