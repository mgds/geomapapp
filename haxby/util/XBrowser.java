package haxby.util;

import javax.swing.*;
import javax.swing.event.*;

import haxby.map.MapApp;

import java.util.*;
import java.net.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;

/*
 * Not used in GMA as of 6/10/09 JOC
 */
public class XBrowser extends WindowAdapter
			implements ActionListener,
				FocusListener,
				HyperlinkListener {
	public final static String BASE_URL = haxby.map.MapApp.TEMP_BASE_URL + "MapApp/help/";
	private JEditorPane ep;
	String url;
	Vector links;
	int currentLink;
	JFrame frame;
	JButton back, fwd, home, close;
	JLabel info;
	JComboBox cb=null;
	public XBrowser() {
		ep = null;
		links = null;
		url = BASE_URL+"help.html";
	}
	public XBrowser( String url ) {
		this();
		this.url = url;
	}
	public void setHelpURL() {
		url = BASE_URL+"help.html";
	}
	public void setURL( String url ) {
		this.url = url;
	}
	public void showBrowser() throws IOException {
		links = new Vector();
		currentLink = 0;
		links.add( URLFactory.url(url) );
		ep = new JEditorPane( URLFactory.url(url) );
		ep.setEditable( false );
		ep.setBackground( new Color( 204, 204, 204) );
		ep.addHyperlinkListener( this );
		frame = new JFrame("MapApp Help");
		frame.addWindowListener( this );
		frame.addFocusListener( this );
		Box box = Box.createHorizontalBox();
		back = new JButton("Back");
		box.add( back );
		back.addActionListener( this );
		fwd = new JButton("Forward");
		box.add( fwd );
		fwd.addActionListener( this );
		home = new JButton("Home");
		box.add( home );
		home.addActionListener( this );
		cb = new JComboBox();
		updateCB();
		box.add(cb);
		
		close = new JButton("Close");
		close.addActionListener(this);
		box.add(close);
	//	cb.addActionListener( this );
		info = new JLabel(BASE_URL+"help.html");
		info.setForeground( Color.black );
		info.setFont( new Font("SansSerif", Font.PLAIN, 12));
		frame.getContentPane().add( info, "South");
		frame.getContentPane().add(box,"North");
		frame.getContentPane().add( new JScrollPane(ep), "Center" );
		frame.pack();
		frame.setSize( 780, 800 );
		DisplayUtil.setRelativeLocation(frame, 40, 40, MapApp.anchor);
		frame.show();
		setEnabled();
	}
	public void hyperlinkUpdate(HyperlinkEvent e) {
		String s = e.getURL().toString().toLowerCase();
		String file = e.getURL().getFile();

		if( !s.startsWith(BASE_URL) && !(file.endsWith(".htm")
					|| file.endsWith(".html")
					|| file.endsWith("/") ) ) return;
		try {
			if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
				ep.setPage( e.getURL() );
				for( int k=links.size()-1 ; k> currentLink ; k--) links.remove(k);
				links.add( e.getURL() );
				currentLink = links.size()-1;
				updateCB();
				info.setText( s +"   \""+ e.getURL().getRef() +"\"" );
			} else if( e.getEventType() == HyperlinkEvent.EventType.ENTERED ) {
				info.setText( s +"   \""+ e.getURL().getRef() +"\"" );
			} else if( e.getEventType() == HyperlinkEvent.EventType.EXITED ) {
				info.setText( null );
			}
		} catch(IOException ex) {
			showErrorMeassage(ex);
		}
		setEnabled();
	}
	void showErrorMeassage(Exception ex) {
		JOptionPane.showMessageDialog( frame, ex.getMessage() );
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==back ) {
			if( currentLink==0 )return;
			currentLink--;
			try {
				ep.setPage( (URL)links.get(currentLink) );
				updateCB();
			} catch(IOException ex) {
				currentLink++;
				showErrorMeassage(ex);
			}
		} else if( evt.getSource()==fwd ) {
			if( currentLink==links.size()-1 )return;
			currentLink++;
			try {
				ep.setPage( (URL)links.get(currentLink) );
				updateCB();
			} catch(IOException ex) {
				currentLink--;
				showErrorMeassage(ex);
			}
		} else if( evt.getSource()==home ) {
			currentLink = 0;
			try {
				ep.setPage( (URL)links.get(currentLink) );
				updateCB();
			} catch(IOException ex) {
				showErrorMeassage(ex);
			}
		} else if( evt.getSource()==cb ) {
			currentLink = cb.getSelectedIndex();
			try {
				ep.setPage( (URL)links.get(currentLink) );
				updateCB();
			} catch(IOException ex) {
			}
		}
		else if (evt.getSource() == close)
			frame.dispose();
		setEnabled();
	}
	void updateCB() {
		cb.removeAllItems();
		for( int k=0 ; k<links.size() ; k++) {
			String title=null;
			try {
				URL url = (URL)links.get(k);
				if( url.getRef() != null ) {
					title = url.getRef();
				} else {
					URLConnection con = url.openConnection();
					con.connect();
					title = con.getHeaderField("TITLE");
					if( title==null ) title = url.getFile();
				}
			} catch( Exception ex) {
				ex.printStackTrace();
			}
			cb.addItem( title );
		}
		cb.setSelectedIndex( currentLink );
	}
	void setEnabled() {
		if( currentLink==0 ) {
			back.setEnabled( false );
		} else {
			back.setEnabled( true );
		}
		if( currentLink==links.size()-1 ) {
			fwd.setEnabled( false );
		} else {
			fwd.setEnabled( true );
		}
	}
	public void windowClosing(WindowEvent evt) {
		frame.dispose();
		ep.removeHyperlinkListener( this );
		ep = null;
		links = null;
		fwd = back = null;
	}
	public void focusGained( FocusEvent evt ) {
	}
	public void focusLost( FocusEvent evt ) {
		frame.toFront();
	}
}
