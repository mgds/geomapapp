package haxby.db.ship;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import haxby.db.mgg.MGG;
import haxby.db.mgg.MGGTrack;
import haxby.map.MapApp;
import haxby.nav.ControlPt;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.SwingWorker;


public class ShipSelector{

	class Task extends SwingWorker<Void, Void> {
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			Random random = new Random();
			int progress = 0;
			//Initialize progress property.
			setProgress(0);
			while (progress < 100) {
				//Sleep for up to one second.
				try {
					Thread.sleep(random.nextInt(50));
				} catch (InterruptedException ignore) {}
				//Make random progress.
				progress += random.nextInt(10);
				setProgress(Math.min(progress, 100));
			}
			return null;
		}
	}

	ArrayList<ShipTrack> trackAL; 
	Task task;
	JTextField keywordText;
	JList keywordJList;
	Ship tracks;
	JScrollPane dialogPane;
	JButton switchControlFiles;
	JRadioButton[] cruiseDataSource;
	static String SHIP_PATH = PathUtil.getPath("PORTALS/SHIP_PATH",
			MapApp.BASE_URL+"/data/portals/ship/");
	
	static String SHIP_PATH_CONTROL = PathUtil.getPath("PORTALS/SHIP_PATH_CONTROL",
			MapApp.BASE_URL+"/data/portals/ship/control/");
	public ShipSelector(Ship tracks) {
		this.tracks = tracks;
		initDialog();
		keywordJList = new JList();
	}

	void initDialog() {

		keywordText = new JTextField();
		keywordText.setText("Enter Comma Separated Keywords Here");
		keywordText.addMouseListener( new MouseListener(){
			
			public void mouseClicked(MouseEvent e) {
				keywordText.selectAll();
			}

			public void mouseEntered(MouseEvent e) {
				
			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		TitledBorder title;
		Border blackline = BorderFactory.createLineBorder(Color.black);
		title = BorderFactory.createTitledBorder(blackline,"Search by Keyword");
		title.setTitleJustification(TitledBorder.CENTER);

		final JPanel panel1 = new JPanel(new GridBagLayout());
		panel1.setBorder(title);
		GridBagConstraints constr = new GridBagConstraints();


		JPanel infoPanel = new JPanel(new GridLayout(1,1));
		JPanel loadPanel = new JPanel(new GridLayout(1,1));
		JPanel textPanel = new JPanel(new GridLayout(1,1));
		JPanel resetPanel = new JPanel(new GridLayout(1,1));
		JPanel radioPanel = new JPanel(new GridLayout(1,2));
		JButton load = new JButton("Search");
		JButton reset = new JButton("Reset");
		JButton cruiseInfo = new JButton("Help");
		/*
		final String helpText = "Cruise Table shows basic cruise information as well as indication of online status for\n"
			+"data types collected during the cruise. Limit cruises displayed on map by entering\n"
			+"keywords in search using parameters displayed in cruise table below (e.g. Chief Scientist\n"
			+"name (e.g. Schouten, etc), data type (e.g. Sidescan, Multibeam, ADCP, etc), expedition ID\n"
			+"(AT11-07), Ship (e.g. Atlantis, Melville), Year of cruise, Initiative (e.g. GeoPRISMS).\n"
			+"Select cruise in the table of by clicking on the ship track in the map. Click on cruise\n"
			+"URL to access data and  additional cruise information.\n"
			+"To save results, select rows of interest and use Copy/Paste.";*/
		
		cruiseInfo.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				String helpText = "";
				String s = "";
				URL helpURL = null;
				try {
					helpURL = URLFactory.url(MapApp.BASE_URL+"/data/portals/ship/help_text.html");
					BufferedReader in = new BufferedReader( new InputStreamReader( helpURL.openStream() ) );
					
					while((s=in.readLine())!=null){
						helpText = helpText.concat(s);
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				JOptionPane.showMessageDialog(MapApp.anchor, helpText);
				
			}
			
		});
		
		
		constr.anchor = GridBagConstraints.PAGE_START;
		constr.weightx = .5;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridx = 0;
		constr.gridy = 0;
		panel1.add(textPanel,constr);
		
		
		final JRadioButton orButton = new JRadioButton("OR");
		orButton.setSelected(true);
		final JRadioButton andButton = new JRadioButton("AND");
		ButtonGroup radioGroup = new ButtonGroup();
		radioGroup.add(orButton);
		radioGroup.add(andButton);
		radioPanel.add(orButton);
		radioPanel.add(andButton);
		
		
		constr.anchor = GridBagConstraints.PAGE_START;
		constr.weightx = .5;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridx = 0;
		constr.gridy = 1;
		panel1.add(radioPanel,constr);
		
		constr.anchor = GridBagConstraints.PAGE_START;
		constr.weightx = .5;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridx = 0;
		constr.gridy = 2;
		panel1.add(loadPanel,constr);
		
		
		constr.anchor = GridBagConstraints.PAGE_START;
		constr.weightx = .5;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridx = 0;
		constr.gridy = 3;		
		panel1.add(resetPanel,constr);
		

		constr.weightx = .5;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.gridx = 0;
		constr.gridy = 4;
		constr.weighty = 1.0;
		constr.anchor = GridBagConstraints.PAGE_START;
		panel1.add(infoPanel,constr);

		textPanel.add(keywordText);
		loadPanel.add(load);
		resetPanel.add(reset);
		infoPanel.add(cruiseInfo);
		final ArrayList<String> keyAL = new ArrayList<String>();
		
		
		
		load.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				task = new Task();
				tracks.map.getMapTools().getApp().addProcessingTask("Search", task);
				//tracks.list.clearSelection();
				tracks.shipTracks.dataT.clearSelection();
				StringTokenizer strtok = new StringTokenizer(keywordText.getText(), ",");
				keyAL.clear();
				while(strtok.hasMoreTokens())
				{
					String tempKey = strtok.nextToken();
					tempKey = tempKey.replaceAll("^\\s+", "");
					tempKey = tempKey.replaceAll("\\b\\s+\\b", " ");
					tempKey = tempKey.trim();
					if(!tempKey.equalsIgnoreCase(""))
						keyAL.add(tempKey);
				}
				tracks.disposeDB();
				
				if(andButton.isSelected())
					loadDB(keyAL, true);
				else
					loadDB(keyAL, false);
				
				
				
			
				tracks.map.getTopLevelAncestor().repaint();
			}
		});
		
		final ShipSelector temp =this;
		reset.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				task = new Task();
				tracks.map.getMapTools().getApp().addProcessingTask("Reset", task);
				tracks.shipTracks.dataT.clearSelection();
				System.gc();
				resetDB();
				
				tracks.map.getTopLevelAncestor().repaint();	
				keywordText.setText("Enter Comma Separated Keywords Here");
				
				
			}
		});
		dialogPane = new JScrollPane(panel1);
	}
	
	
	
	public JComponent getDialog() {
		return dialogPane;
	}

	public void loadDB() {
		trackAL = new ArrayList<ShipTrack>();
		tracks.trim();
		//tracks.shipTracks.disposeDB();
		try {

				int k=0;
				Point2D.Double pt = new Point2D.Double();
				double wrap = tracks.map.getWrap();
				double wraptest = wrap/2.;
				double xtest = 0d;
				Projection proj = tracks.map.getProjection();
				String name = "";
				Dimension mapDim = tracks.map.getDefaultSize();

				URL ControlFileURL;
				DataInputStream in;

				String s = "";

				ControlFileURL = URLFactory.url(SHIP_PATH_CONTROL + "Ship_All_Cruises.control.gz");
				in = new DataInputStream( new GZIPInputStream( ControlFileURL.openStream() ) );
				
				while (true) {

					pt = new Point2D.Double();
					wrap = tracks.map.getWrap();
					wraptest = wrap/2.;
					xtest = 0d;
					proj = tracks.map.getProjection();
					name = "";
					mapDim = tracks.map.getDefaultSize();
					
						try {
							name = in.readUTF();
						} catch (EOFException ex) {
							break;
						}
					
					int nseg = in.readInt();
					ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
					int start = in.readInt();						
					int end = in.readInt();
					Rectangle2D.Double bounds = new Rectangle2D.Double();
					for( int i=0 ; i<nseg ; i++) {						
						int a = in.readInt();
						cpt[i] = new ControlPt.Float[a];
						for( int j=0 ; j<cpt[i].length ; j++) {
							pt.x = in.readDouble();
							pt.y = in.readDouble();
							
							Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
							if(j==0&&i==0) {
								bounds.x = p.x;
								bounds.y = p.y;
								bounds.width = 0.;
								bounds.height = 0.;
								xtest = p.x;
							}else {
								if( wrap>0.) {
									while(p.x>xtest+wraptest) p.x-=wrap;
									while(p.x<xtest-wraptest) p.x+=wrap;
									}
								if(p.x<bounds.x) {
									bounds.width += bounds.x-p.x;
									bounds.x = p.x;
									xtest = bounds.x + .5*bounds.width;
								} else if( p.x>bounds.x+bounds.width ) {
									bounds.width = p.x-bounds.x;
									xtest = bounds.x + .5*bounds.width;
								}
								if(p.y<bounds.y) {
									bounds.height += bounds.y-p.y;
									bounds.y = p.y;
								} else if( p.y> bounds.y+bounds.height ) {
									bounds.height = p.y-bounds.y;
								}
							}
							cpt[i][j] = new ControlPt.Float((float)p.x, (float)p.y );
						}
					}
					
					String keywordString = in.readUTF();
					String infoStr = in.readUTF();
					StringTokenizer strtok = new StringTokenizer(keywordString, ",");
					HashSet<String> trackKeywordList = new HashSet<String>();
					while(strtok.hasMoreTokens())
					{
						String tempKey = strtok.nextToken();
						trackKeywordList.add(tempKey.toLowerCase().trim());
					}

					ArrayList<String>keysToAdd = new ArrayList<String>();
					for(String kw: trackKeywordList){
						StringTokenizer moreTokens = new StringTokenizer(kw, " :/");
						while(moreTokens.hasMoreTokens())
							keysToAdd.add(moreTokens.nextToken().toLowerCase());
						
					}
					
					trackKeywordList.addAll(keysToAdd);
					
					
					
					byte types = 010;
					
					ShipTrack t = new ShipTrack( new TrackLine( name, bounds, cpt , start, end, types, (int)wrap), trackKeywordList, infoStr);
					trackAL.add(t);
					trackKeywordList = null;
					infoStr = null;
					boolean addTrack = true;
					for(ShipTrack strack: tracks.model.tracks)
					{
						if(strack.getName().equalsIgnoreCase(t.nav.getName())){
							addTrack = false;
							break;
						}
							
					}

					if(addTrack){
						tracks.add( t);
						tracks.shipTracks.add(t);
					}
					t = null;
				}				
			
		}catch(Exception e){e.printStackTrace();}
		System.gc();
		tracks.trim();
		tracks.model.clearTracks();
		//tracks.shipTableModel.clearTracks();
		for( int i=0 ; i<tracks.tracks.length ; i++) {
			tracks.model.addTrack(tracks.tracks[i], i);
			tracks.shipTableModel.addTrack(tracks.tracks[i],i);
		}			
	} 
	
	public void loadDB(ArrayList<String> keywords, boolean exclusive) {
		for(ShipTrack t:trackAL){
					boolean addTrack = true;

					if(exclusive==true) {
						if(addTrack && t.hasAllKeywords(keywords)){
								tracks.add(t);
								tracks.shipTracks.add(t);
								}
					}
					else {
						if(addTrack && t.hasOneKeyword(keywords))
							tracks.add(t);
							tracks.shipTracks.add(t);
					}
				}
		System.gc();
		tracks.loaded = true;
		tracks.trim();
		tracks.model.clearTracks();
		tracks.shipTracks.model.clearTracks();
		for( int i=0 ; i<tracks.tracks.length ; i++) {
			tracks.model.addTrack(tracks.tracks[i], i);
			tracks.shipTableModel.addTrack(tracks.tracks[i],i);
		}
	}

	public void resetDB(){
		tracks.disposeDB();
		
		for(ShipTrack t:trackAL){
			tracks.add(t);
			tracks.shipTracks.add(t);
		}

		//System.gc();
		tracks.loaded = true;
		tracks.trim();
		tracks.model.clearTracks();
		tracks.shipTracks.model.clearTracks();
		for( int i=0 ; i<tracks.tracks.length ; i++) {
			tracks.model.addTrack(tracks.tracks[i], i);
			tracks.shipTableModel.addTrack(tracks.tracks[i],i);
		}
	}
}