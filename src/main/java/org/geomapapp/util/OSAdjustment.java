package org.geomapapp.util;

/*
import java.nio.file.Files;
import java.nio.file.Paths;
*/

public class OSAdjustment {
	public static enum OS {
		//LINUX("current"), MACOS("current_mac"), WINDOWS("current_win");
		LINUX, MACOS, WINDOWS;
		//public final String gmrt_current;
		private OS(/*String gmrt_cur_in*/) {
			//gmrt_current = gmrt_cur_in;
		}
	}
	private static OS whichOs = null;
	private OSAdjustment() {}
	public static OS getOS() {
		String osName = System.getProperty("os.name");
		if(null == whichOs) {
			if(osName.matches(".*Mac.*")) {
				whichOs = OS.MACOS;
			}
			else if(osName.equals("Linux")) {
				whichOs = OS.LINUX;
			}
			else {
				whichOs = OS.WINDOWS;
			}
		}
		return whichOs;
	}
}
