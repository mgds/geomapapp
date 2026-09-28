package org.geomapapp.io;

import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import java.awt.Component;

public class ShowStackTrace {
	public static void showTrace(Throwable e) {
		showTrace(e, null);
	}
	public static void showTrace(Throwable e, Component comp) {
		StackTraceElement[] trace = e.getStackTrace();
		StringBuffer sb = new StringBuffer();
		sb.append( "ERROR\n"+ e.getClass().getName() +" "+ e.getMessage());
		for( int i=0 ; i<trace.length ; i++) sb.append( "\n"+ trace[i].toString() );
		JTextArea t = new JTextArea(sb.toString());
		JOptionPane.showMessageDialog( comp, new JScrollPane(t), "", JOptionPane.ERROR_MESSAGE, null);
	}
}
