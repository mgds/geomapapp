package haxby.util;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ErrorMonitor implements Runnable,
				ActionListener {
	JFrame frame;
	Process process;
	BufferedReader in;
	JDialog dialog;
	JTextArea text;
	JScrollPane sp;
	Thread thread;
	JButton abort;
	public ErrorMonitor( JFrame frame ) {
		this.frame = frame;
		process = null;
		in = null;
		dialog = null;
		text = null;
		sp = null;
		thread = null;
	}
	public void setProcess( Process p ) {
		process = p;
		in = new BufferedReader(
			new InputStreamReader( p.getErrorStream() ));
		initDialog();
		thread = new Thread( this );
		thread.start();
	}
	void initDialog() {
		dialog = new JDialog( frame, "Error Monitor" );
		text = new JTextArea( 20, 80 );
		text.setFont(new Font("MonoSpaced", Font.PLAIN, 12));
		sp = new JScrollPane( text );
		dialog.getContentPane().add( sp, "Center" );

		abort = new JButton("abort process");
		abort.addActionListener( this );
		dialog.getContentPane().add( abort, "North" );

		dialog.pack();
		dialog.setDefaultCloseOperation( dialog.HIDE_ON_CLOSE );
	}
	public void run() {
		String s;
		boolean ok=true;
		try {
			while( (s=in.readLine()) != null ) {
				ok = false;
				if( !dialog.isShowing() ) dialog.show();
				synchronized( text.getTreeLock() ) {
					text.append( s +"\n" );
					JScrollBar sb = sp.getVerticalScrollBar();
					sb.setValue( sb.getMaximum() );
				}
			}
		} catch (Exception ex) {
			ok = false;
			PrintStream err = new PrintStream(
				new ByteArrayOutputStream());
			ex.printStackTrace( err );
			try {
				while( (s=in.readLine()) != null ) {
					if( !dialog.isShowing() ) dialog.show();
					synchronized( text.getTreeLock() ) {
						text.append( s +"\n" );
						JScrollBar sb = sp.getVerticalScrollBar();
						sb.setValue( sb.getMaximum() );
					}
				}
			} catch( Exception e ) {
			}
		}
		abort.removeActionListener( this );
		if( !ok ) {
			JOptionPane.showMessageDialog( dialog, "Error Monitor will close" );
		}
		dialog.dispose();
		process = null;
		in = null;
		dialog = null;
		text = null;
		sp = null;
		abort = null;
	}
	public void actionPerformed( ActionEvent evt ) {
		int ok = JOptionPane.showConfirmDialog( dialog, "really abort process",
						"abort", JOptionPane.YES_NO_OPTION );
		if( ok==JOptionPane.YES_OPTION ) {
			process.destroy();
		}
	}
}
