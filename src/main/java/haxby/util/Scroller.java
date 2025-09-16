package haxby.util;

import javax.swing.*;
import java.awt.event.*;
import java.awt.Point;

public class Scroller implements ActionListener {
	JScrollPane pane;
	JScrollBar hBar;
	JScrollBar vBar;
	int condition;
	static KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP,0);
	static KeyStroke pageUp = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,0);
	static KeyStroke smallUp = KeyStroke.getKeyStroke(up.getKeyCode(), java.awt.Event.SHIFT_MASK);
	static KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0);
	static KeyStroke pageDown = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,0);
	static KeyStroke smallDown = KeyStroke.getKeyStroke(down.getKeyCode(), java.awt.Event.SHIFT_MASK);
	static KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,0);
	static KeyStroke smallLeft = KeyStroke.getKeyStroke(left.getKeyCode(), java.awt.Event.SHIFT_MASK);
	static KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0);
	static KeyStroke smallRight = KeyStroke.getKeyStroke(right.getKeyCode(), java.awt.Event.SHIFT_MASK);
	static KeyStroke home = KeyStroke.getKeyStroke(KeyEvent.VK_HOME,0);
	static KeyStroke end = KeyStroke.getKeyStroke(KeyEvent.VK_END,0);
	boolean enabled;

	public Scroller(JScrollPane p, int condition) {
		pane = p;
		hBar = p.getHorizontalScrollBar();
		vBar = p.getVerticalScrollBar();
		this.condition = condition;
		enabled = false;
	}
	public void enable() {
		if(enabled)return;
		pane.registerKeyboardAction(this, "up", up, condition);
		pane.registerKeyboardAction(this, "page up", pageUp, condition);
		pane.registerKeyboardAction(this, "small up", smallUp, condition);
		pane.registerKeyboardAction(this, "down", down, condition);
		pane.registerKeyboardAction(this, "page down", pageDown, condition);
		pane.registerKeyboardAction(this, "small down", smallDown, condition);
		pane.registerKeyboardAction(this, "left", left, condition);
		pane.registerKeyboardAction(this, "small left", smallLeft, condition);
		pane.registerKeyboardAction(this, "right", right, condition);
		pane.registerKeyboardAction(this, "small right", smallRight, condition);
		pane.registerKeyboardAction(this, "home", home, condition);
		pane.registerKeyboardAction(this, "end", end, condition);
		enabled = true;
	}
	public void disable() {
		if(!enabled)return;
		pane.unregisterKeyboardAction(up);
		pane.unregisterKeyboardAction(pageUp);
		pane.unregisterKeyboardAction(smallUp);
		pane.unregisterKeyboardAction(down);
		pane.unregisterKeyboardAction(pageDown);
		pane.unregisterKeyboardAction(smallDown);
		pane.unregisterKeyboardAction(left);
		pane.unregisterKeyboardAction(smallLeft);
		pane.unregisterKeyboardAction(right);
		pane.unregisterKeyboardAction(smallRight);
		pane.unregisterKeyboardAction(home);
		pane.unregisterKeyboardAction(end);
		enabled = false;
	}

	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();
		if(s.equals("up")) {
			vBar.setValue(vBar.getValue()-vBar.getUnitIncrement(-1));
		} else if(s.equals("page up")) {
			vBar.setValue(vBar.getValue()-vBar.getBlockIncrement(-1));
		} else if(s.equals("small up")) {
			vBar.setValue(vBar.getValue()-1);
		} else if(s.equals("down")) {
			vBar.setValue(vBar.getValue()+vBar.getUnitIncrement(1));
		} else if(s.equals("page down")) {
			vBar.setValue(vBar.getValue()+vBar.getBlockIncrement(1));
		} else if(s.equals("small down")) {
			vBar.setValue(vBar.getValue()+1);
		} else if(s.equals("right")) {
			hBar.setValue(hBar.getValue()+hBar.getUnitIncrement(1));
		} else if(s.equals("small right")) {
			hBar.setValue(hBar.getValue()+1);
		} else if(s.equals("left")) {
			hBar.setValue(hBar.getValue()-hBar.getUnitIncrement(-1));
		} else if(s.equals("small left")) {
			hBar.setValue(hBar.getValue()-1);
		} else if(s.equals("home")) {
			hBar.setValue(0);
			vBar.setValue(0);
		} else if(s.equals("end")) {
			vBar.setValue(vBar.getMaximum());
		}
	}
	public void validate() {
		pane.validate();
	}
	public void scrollTo(Point p) {
		hBar.setValue(p.x);
		vBar.setValue(p.y);
	}
}
