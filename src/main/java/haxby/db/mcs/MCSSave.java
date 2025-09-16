package haxby.db.mcs;

import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class MCSSave implements ActionListener, Runnable {
	static JFileChooser chooser = null;
	MCSImage2 image;
	BufferedInputStream in = null;
	BufferedOutputStream out = null;
	JDialog frame = null;
	File file = null;
	boolean canceled=false;
	Thread thread = null;
	boolean history = false;
	JRadioButton nav  = null;
	
	public MCSSave(MCSImage2 image) {
		this.image = image;
		if( chooser==null )chooser = new JFileChooser( System.getProperty("user.home") );
	}
	public void save() {
		JPanel panel = new JPanel( new GridLayout(0,1) );
		JLabel label = new JLabel("Save/Download");
		label.setForeground(Color.black);
		panel.add( label );
		ButtonGroup group = new ButtonGroup();
		JRadioButton jpeg = new JRadioButton("JPEG image");
		group.add(jpeg);
		panel.add( jpeg);
		JRadioButton segy = new JRadioButton("SEGY file");
		group.add(segy);
		panel.add( segy);
		nav = new JRadioButton("CDP Navigation");
		group.add(nav);
		panel.add( nav);
		JRadioButton history = new JRadioButton("processing history");
		group.add(history);
		panel.add( history);
		jpeg.setSelected(true);
		int ok = JOptionPane.showOptionDialog( image.getTopLevelAncestor(),
			panel,
			"cruise "+ image.getCruiseID() +", line "+ image.getID(),
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			null, null, null);
		if( ok== JOptionPane.CANCEL_OPTION) return;
		if( nav.isSelected() ||
			segy.isSelected()  
			|| history.isSelected() )saveSegy(history.isSelected());
		else saveJPEG();
	}
	void saveJPEG() {
		try {
			chooser.setSelectedFile( new File( chooser.getCurrentDirectory(), 
						image.getCruiseID()+"_"+image.getID()+".jpg" ));
			int ok = chooser.showSaveDialog( image.getTopLevelAncestor() );
			if( ok==chooser.CANCEL_OPTION ) return;
			file = chooser.getSelectedFile();
			while(file.exists()) {
				ok = JOptionPane.showOptionDialog( image.getTopLevelAncestor(),
					"File  - "+ file.getName() +" - exists, overwrite?",
					"Overwrite?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null, null, null);
				if(ok==JOptionPane.YES_OPTION) break;
				ok = chooser.showSaveDialog( image.getTopLevelAncestor() );
				if( ok==chooser.CANCEL_OPTION ) return;
				file = chooser.getSelectedFile();
			}
			BufferedOutputStream jpgout = new BufferedOutputStream(
				new FileOutputStream( file ), 32768);
			image.saveJPEG( jpgout );
		} catch (Exception ex) {
			JOptionPane.showMessageDialog( image.getTopLevelAncestor(), ex.getMessage() );
		}
	}
	void saveSegy(boolean history) {
		if( thread!=null && thread.isAlive() && !canceled ) {
			JOptionPane.showMessageDialog( image.getTopLevelAncestor(), "save in progress, try again later");
			return;
		}
		this.history = history;
		thread = new Thread(this);
		thread.start();
	}
	public boolean isAlive() {
		return thread.isAlive();
	}
	public void run() {
		try {
			URL url = null;
			if( nav.isSelected() ) {
				url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "cgi-bin"+
					"/MCS/get_nav?" + image.getCruiseID().trim()
					+"+"+ image.getID().trim());
			} else {
				url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "cgi-bin"+
					"/MCS/get_segy?" + image.getCruiseID().trim()
					+"+"+ image.getID().trim());
			}
			BufferedReader bin = new BufferedReader(
				new InputStreamReader( url.openStream() ));
			String s = bin.readLine();
			bin.close();
			if( !s.startsWith("http") ) {
				JOptionPane.showMessageDialog( image.getTopLevelAncestor(), s);
				return;
			}
			if(history) s += ".history";
			url = URLFactory.url(s);
			String filename = url.getFile();
			chooser.setSelectedFile( new File( chooser.getCurrentDirectory(), filename ));
			int ok = chooser.showSaveDialog( image.getTopLevelAncestor() );
			if( ok==chooser.CANCEL_OPTION ) return;
			file = chooser.getSelectedFile();
			while(file.exists()) {
				ok = JOptionPane.showOptionDialog( image.getTopLevelAncestor(),
					"File  - "+ file.getName() +" - exists, overwrite?",
					"Overwrite?",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null, null, null);
				if(ok==JOptionPane.YES_OPTION) break;
				ok = chooser.showSaveDialog( image.getTopLevelAncestor() );
				if( ok==chooser.CANCEL_OPTION ) return;
				file = chooser.getSelectedFile();
			}
			frame = new JDialog( (JFrame)image.getTopLevelAncestor(), "download progress");
			JLabel label = new JLabel("Opening Connection...");
			label.setBorder(BorderFactory.createEmptyBorder( 20,20,20,20 ));
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(this);
			frame.getContentPane().add(label, "Center");
			frame.getContentPane().add( cancel, "South");
			frame.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE);
			frame.setTitle( "Be Patient" );
			label.setText("Retrieving"+ filename +" from Mass Store"
					+" (it often takes a few seconds)");
			frame.pack();
	Container comp = (Container)image.getParent();
	Point point = comp.getLocationOnScreen();
	frame.setLocation( point.x+20, point.y+20 );
			frame.show();
			canceled = false;
			byte[] buf = new byte[32768];
			URLConnection con = url.openConnection();
			int size = con.getContentLength();
			if( canceled ) {
				if(frame != null )frame.dispose();
				return;
			}
			int tran = 0;
			label.setText("size = " + size +", "+ tran +" bytes transfered");
			frame.pack();
			in = new BufferedInputStream( url.openStream(), 32768 );
			out = new BufferedOutputStream( 
					new FileOutputStream( file ), 32768 );
			int len;
			while( (len=in.read(buf)) != -1 ) {
				if( canceled ) {
					if(frame != null )frame.dispose();
					return;
				}
				out.write( buf, 0, len );
				tran += len;
				label.setText("size = " + size +", "+ tran +" bytes transfered");
				frame.pack();
			}
			frame.dispose();
			in.close();
			out.flush();
			out.close();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog( image.getTopLevelAncestor(), ex.getMessage() );
			System.out.println( ex.getClass().getName() );
		}
	}
	public void actionPerformed(ActionEvent evt) {
		canceled=true;
		if( frame!=null) frame.dispose();
		try {
			in.close();
			file.delete();
		} catch (Exception ex) {
		}
	}
}
