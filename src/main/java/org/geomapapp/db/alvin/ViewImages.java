package org.geomapapp.db.alvin;

import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.geomapapp.util.ImageComponent;
import org.geomapapp.util.Zoomer;

public class ViewImages extends ImageComponent {
	Calendar cal;
	String baseURL;
	File imageList;
	JComboBox cb;
	File dir;
	String dive;
	JComboBox imageCB;
	JFrame frame;

	int imageNumber;
	BufferedImage[] image;
	
	public ViewImages(File dir) throws IOException {
		imageNumber = 0;
		image = new BufferedImage[2];
		this.dir = dir;
		baseURL = "https://4dgeo.whoi.edu/DAQ/" + dir.getName() +"/";
		File[] files = dir.listFiles( new FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".images");
			}
		});
		if( files.length==0 ) throw new IOException("no image lists");
		cb = new JComboBox();
		for( int k=0 ; k<files.length ; k++)cb.addItem( files[k].getName() );
		cb.setSelectedIndex( 0 );
		JPanel panel = new JPanel(new GridLayout(0,1));
		panel.add(cb);
		cb.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange()!=e.SELECTED )return;
				select();
			}
		});
		imageCB = new JComboBox();
		imageCB.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange()!=e.SELECTED )return;
				try {
					selectImage();
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		panel.add( imageCB );
		JPanel panel1 = new JPanel(new BorderLayout());
		panel1.add(panel, "North");
		select();
	//	zoomOut(new Point(1,1));
		setFocusable(true);
		frame = new JFrame("Alvin Images");
		frame.getContentPane().add(panel1,"West");
		frame.getContentPane().add( new JScrollPane(this), "Center" );
		Zoomer z = new Zoomer(this);
		addMouseListener(z);
		addKeyListener(z);
		addMouseMotionListener(z);
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if( e.getKeyCode()==e.VK_E ) showColorDialog();
				else if( e.getKeyCode()==e.VK_SPACE ) otherImage();
				else if( e.getKeyCode()==e.VK_R ) resetTransform();
			}
		});
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
	void select() {
		String name = cb.getSelectedItem().toString();
		dive = name.substring( 0, name.indexOf(".images") );
		File file = new File(dir,cb.getSelectedItem().toString());
		try {
			BufferedReader in = new BufferedReader(
				new FileReader( file ));
			imageCB.removeAllItems();
			String s;
			while( (s=in.readLine()) != null )imageCB.addItem(s);
			imageCB.setSelectedIndex( 0 );
			selectImage();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	void back() {
		int i = imageCB.getSelectedIndex()-1;
		if(i<0)return;
		imageCB.setSelectedIndex(i);
		try {
			selectImage();
		} catch(IOException e) {
		}
	}
	void forward() {
		int i = imageCB.getSelectedIndex()+1;
		if(i>=imageCB.getItemCount())return;
		imageCB.setSelectedIndex(i);
		try {
			selectImage();
		} catch(IOException e) {
		}
	}
	void selectImage() throws IOException {
		String name = imageCB.getSelectedItem().toString();
		try {
			image[0] = ImageIO.read( URLFactory.url(baseURL + dive +"/Src1/Images0001/" + name ));
		} catch(Exception e) {
		}
		name = "SubSea2" + name.substring(7);
		try {
			image[1] = ImageIO.read( URLFactory.url(baseURL + dive +"/Src2/Images0001/" + name ));
		} catch(Exception e) {
		}
		setImage(image[imageNumber]);
	}
	void initCalendar() {
		if( cal!=null )return;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	}
	public double getTime(String name) {
		initCalendar();
		cal.set(cal.YEAR, Integer.parseInt(name.substring(8,12)));
		cal.set(cal.MONTH, Integer.parseInt(name.substring(12,14))-1);
		cal.set(cal.DATE, Integer.parseInt(name.substring(14,16))-1);
		cal.set(cal.HOUR_OF_DAY, Integer.parseInt(name.substring(17,19)));
		cal.set(cal.MINUTE, Integer.parseInt(name.substring(19,21)));
		cal.set(cal.SECOND, Integer.parseInt(name.substring(21,23)));
		cal.set(cal.MILLISECOND, 0);
		return cal.getTimeInMillis()*.001;
	}
	void otherImage() {
		imageNumber = (imageNumber+1)%2;
		setImage(image[imageNumber]);
	}
// https://4dgeo.whoi.edu/DAQ/AT11-07/Alvin-D3961/Src2/Images0001/SubSea2.20040203_172415.jpg
	public static void main(String[] args) {
		try {
			new ViewImages( new File(args[0]) );
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
