package haxby.map;

import haxby.util.PathUtil;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BaseMapSelect implements ActionListener {
	public BaseMapSelect() {
	}
	public static final Map<Integer, String> IMAGE_PATH = new HashMap<Integer, String>();
	public static final Map<Integer, String> NAMES = new HashMap<Integer, String>();
	static {
		IMAGE_PATH.put(new Integer(MapApp.MERCATOR_MAP), "smallMercV3.jpg");
		IMAGE_PATH.put(new Integer(MapApp.SOUTH_POLAR_MAP), "smallSPV3.jpg");
		IMAGE_PATH.put(new Integer(MapApp.NORTH_POLAR_MAP), "smallNPV3.jpg");
		IMAGE_PATH.put(new Integer(MapApp.WORLDWIND), "smallWW.jpg");

		NAMES.put(new Integer(MapApp.MERCATOR_MAP), "Mercator");
		NAMES.put(new Integer(MapApp.SOUTH_POLAR_MAP), "South Polar");
		NAMES.put(new Integer(MapApp.NORTH_POLAR_MAP), "North Polar");
		NAMES.put(new Integer(MapApp.WORLDWIND), "Globe");
	}

	JLabel selected,
		   infoLabel;

	//Make initially selected map mercator
	int initialMapSelection = 0;

	JToggleButton[] mapsTB;
	int[] maps;
	public int getBaseMap() {
		ButtonGroup bg = new ButtonGroup();
		mapsTB = new JToggleButton[MapApp.SUPPORTED_MAPS.size()];
		maps = new int[MapApp.SUPPORTED_MAPS.size()];
		JPanel panel2 = new JPanel( new FlowLayout() );

		UIManager.put("ToggleButton.shadow", Color.decode("#7EA5C6"));
		UIManager.put ("ToggleButton.select",
				(Color) UIManager.get ("ToggleButton.shadow"));
		int i = 0;
		for (Iterator<Integer> iter = MapApp.SUPPORTED_MAPS.iterator(); iter.hasNext();) {
			int map = iter.next().intValue();
			mapsTB[i] = getMapToggleButton(map);
			maps[i] = map;

			bg.add(mapsTB[i]);
			mapsTB[i].addActionListener( this);
			panel2.add( mapsTB[i] );
			i++;
		}

		mapsTB[initialMapSelection].setSelected(true);
		JPanel panel = new JPanel( new BorderLayout() );
		panel.add(panel2);

		JLabel label = new JLabel( "Choose a Base Map Projection:  ");
		label.setForeground( Color.black);

		selected = new JLabel( NAMES.get(new Integer(maps[initialMapSelection])).toString() + " Selected" + '\n');
		selected.setForeground( Color.decode("#496781"));

		JPanel panelN = new JPanel( new BorderLayout() );
		panelN.add(label, "West");
		panelN.add(selected);
		panelN.add(new JLabel("version " + haxby.map.MapApp.VERSION), "East");
		panel.add( panelN,"North" );

		JPanel southPanel1 = new JPanel( new GridLayout(0,1) );

		infoLabel = new JLabel("<html>Created By: William F. Haxby <font size=2>('03-'06)</font>, William B.F. Ryan <font size=2>('03-'12)</font></font>" +
				"<br>Development and Design: "
				+ "Alex Strong <font size=2>('23-present)</font>, "
				+ "Neville Shane <font size=2>('16-'22)</font>, "
				+ "John Morton <font size=2>('10-present)</font>, "
				+ "Sze-Man(Samantha) Chan <font size=2>('09-'15)</font>, "
				+ "<br>&#09;"
				+ "Ben Barg <font size=2>('13-'15)</font>, "
				+ "Donald E. Pomeroy <font size=2>('11-'12)</font>, "
				+ "Justin Coplan <font size=2>('04-'11)</font>, "
				+ "Andrew K. Melkonian <font size=2>('06-'09)</font>, "
				+ "<br>&#09;"
				+ "Bob Arko <font size=2>('04-'17)</font>, "
				+ "Ed Bohl <font size=2>('11-21)</font>, "
				+ "Suzanne Carbotte <font size=2>('03-'23)</font>, "
				+ "Vicki Ferrini <font size=2>('06-present)</font>, "
				+ "<br>&#09;"
				+ "Andrew Goodwillie <font size=2>('06-present)</font>, " 
				+ "Rose Anne Weissel <font size=2>('04-present)</font> " 
				+ "<br>&#09;" 
				+ "<br>Funded By: National Science Foundation & Trustees of Columbia University" 
				+ "<br><br><center><font color=#CC3333>The Displayed Maps, Images, Data Tables are not to be used for Navigation Purposes.</font></center></html>");
		southPanel1.add(infoLabel);

		JPanel panelS = new JPanel( new BorderLayout() );
		panelS.add(southPanel1,"North");

		panel.add(panelS,"South");
		// Control over button dialogs. Default to Agree Selection
		Object[] options = {"Agree","Cancel"};
		
		// Signify if running in Development Mode
		String devText = "";
		if(MapApp.BASE_URL.matches(MapApp.DEV_URL))
			 devText = "**DEVELOPMENT MODE** ";
		
		/*int ok = JOptionPane.showOptionDialog( MapApp.anchor, panel, devText + "Choose a Base Map Projection",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				options,
				options[0]);*/
		Object defaultOption = options[0];
		JOptionPane jop = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, defaultOption);
		JDialog dialog = jop.createDialog(devText + "Choose a Base Map Projection");
		dialog.setVisible(true);
		Object val = jop.getValue();
		boolean isOk = (val instanceof String)?(val.equals(defaultOption)):(val instanceof Integer && (Integer)val == JOptionPane.OK_OPTION);
		//int ok = (Integer)jop.getValue();
		MapApp.anchor = dialog;
		/*devText + "Choose a Base Map Projection",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				options,
				options[0]);*/
		int whichMap = -1;
		if(isOk) {
			for (i = 0; i < mapsTB.length; i++) {
				if (mapsTB[i].isSelected())
					whichMap = maps[i];
				mapsTB[i] = null;
			}
		}
		return whichMap;
	}

	/*
	 * Gets starup images from server it will be needed to track stats.
	 * If the server fails to fetch images then go into resources directory in application.
	 * Samantha
	 */
	private JToggleButton getMapToggleButton(int map) {
		ClassLoader cl = getClass().getClassLoader();
		try {
			String imagePath = PathUtil.getPath("STARTUP_PATH", MapApp.BASE_URL+"/gma_startup/") +
					IMAGE_PATH.get(new Integer(map));
				BufferedInputStream in = new BufferedInputStream((haxby.util.URLFactory.url(imagePath)).openStream());
					//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
					//BufferedImage image = decoder.decodeAsBufferedImage();
					BufferedImage image = ImageIO.read(in);
					in.close();
			JToggleButton tb = new JToggleButton( new ImageIcon(image));
			return tb;
			
		} catch (Exception ex) {
			String imagePath = "org/geomapapp/resources/maps/startup/" +
									IMAGE_PATH.get(new Integer(map));
			URL url = cl.getResource(imagePath);
			JToggleButton tb = new JToggleButton( new ImageIcon(url));
			return tb;
			//return new JToggleButton( NAMES.get(new Integer(map)).toString() );
		}
	}

	public void actionPerformed(ActionEvent evt) {
		for (int i = 0; i < mapsTB.length; i++) {
			if (mapsTB[i].isSelected())
				selected.setText(NAMES.get(new Integer(maps[i])).toString() + " Selected");
		}
	}
}