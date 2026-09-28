package haxby.util;

import java.awt.MenuShortcut;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

public class KeyShortcuts {
	static Hashtable<String,MenuShortcut> keyToShortcutHash;

	public static MenuShortcut getShortcutForKey(String key) {
		if ( keyToShortcutHash == null || keyToShortcutHash.size() < 1 ) {
			initHashtable();
		}
		return keyToShortcutHash.get(key.toLowerCase());
	}

	public static void initHashtable() {
		keyToShortcutHash = new Hashtable<String,MenuShortcut>();
		keyToShortcutHash.put("a", 
			new MenuShortcut(KeyEvent.VK_A));

		keyToShortcutHash.put("b", 
			new MenuShortcut(KeyEvent.VK_B));

		keyToShortcutHash.put("c", 
			new MenuShortcut(KeyEvent.VK_C));

		keyToShortcutHash.put("d", 
			new MenuShortcut(KeyEvent.VK_D));

		keyToShortcutHash.put("e", 
			new MenuShortcut(KeyEvent.VK_E));

		keyToShortcutHash.put("f", 
			new MenuShortcut(KeyEvent.VK_F));

		keyToShortcutHash.put("g", 
			new MenuShortcut(KeyEvent.VK_G));

		keyToShortcutHash.put("h", 
			new MenuShortcut(KeyEvent.VK_H));

		keyToShortcutHash.put("i", 
			new MenuShortcut(KeyEvent.VK_I));

		keyToShortcutHash.put("j", 
			new MenuShortcut(KeyEvent.VK_J));

		keyToShortcutHash.put("k", 
			new MenuShortcut(KeyEvent.VK_K));

		keyToShortcutHash.put("l", 
			new MenuShortcut(KeyEvent.VK_L));

		keyToShortcutHash.put("m", 
			new MenuShortcut(KeyEvent.VK_M));

		keyToShortcutHash.put("n", 
			new MenuShortcut(KeyEvent.VK_N));

		keyToShortcutHash.put("o", 
			new MenuShortcut(KeyEvent.VK_O));

		keyToShortcutHash.put("p", 
			new MenuShortcut(KeyEvent.VK_P));

		keyToShortcutHash.put("q", 
			new MenuShortcut(KeyEvent.VK_Q));

		keyToShortcutHash.put("r", 
			new MenuShortcut(KeyEvent.VK_R));

		keyToShortcutHash.put("s", 
			new MenuShortcut(KeyEvent.VK_S));

		keyToShortcutHash.put("t", 
			new MenuShortcut(KeyEvent.VK_T));

		keyToShortcutHash.put("u", 
			new MenuShortcut(KeyEvent.VK_U));

		keyToShortcutHash.put("v", 
			new MenuShortcut(KeyEvent.VK_V));

		keyToShortcutHash.put("w", 
			new MenuShortcut(KeyEvent.VK_W));

		keyToShortcutHash.put("x", 
			new MenuShortcut(KeyEvent.VK_X));

		keyToShortcutHash.put("y", 
			new MenuShortcut(KeyEvent.VK_Y));

		keyToShortcutHash.put("z", 
			new MenuShortcut(KeyEvent.VK_Z));

		keyToShortcutHash.put("0", 
			new MenuShortcut(KeyEvent.VK_0));

		keyToShortcutHash.put("1", 
			new MenuShortcut(KeyEvent.VK_1));

		keyToShortcutHash.put("2", 
			new MenuShortcut(KeyEvent.VK_2));

		keyToShortcutHash.put("3", 
			new MenuShortcut(KeyEvent.VK_3));
		
		keyToShortcutHash.put("4", 
			new MenuShortcut(KeyEvent.VK_4));

		keyToShortcutHash.put("5", 
			new MenuShortcut(KeyEvent.VK_5));

		keyToShortcutHash.put("6", 
			new MenuShortcut(KeyEvent.VK_6));
		
		keyToShortcutHash.put("7", 
			new MenuShortcut(KeyEvent.VK_7));

		keyToShortcutHash.put("8", 
			new MenuShortcut(KeyEvent.VK_8));

		keyToShortcutHash.put("9", 
			new MenuShortcut(KeyEvent.VK_9));
	}
}
