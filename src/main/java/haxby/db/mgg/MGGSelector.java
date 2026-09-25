package haxby.db.mgg;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import haxby.map.MapApp;
import haxby.nav.ControlPt;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.DrawLine;
import haxby.util.PathUtil;
import haxby.util.ProcessingDialog;
import haxby.util.URLFactory;

public class MGGSelector implements ActionListener {
	MGG tracks;
	JPanel dialogPane;
	JLabel textDate;

	// 1.4.4: Add radio buttons to allow the user to switch the currently loaded
	// control file(s)
	JButton switchControlFiles;
	JRadioButton[] cruiseDataSource;

	// GMA 1.4.8: Button now defined throughout class instead of locally
	JButton noneSelected;
	JCheckBox topoCB, gravCB, magCB;
	String loadedControlFiles;

	final static String LAMONT_CONTROL_LOADED = "LDEO";
	final static String NGDC_CONTROL_LOADED = "NGDC";
	final static String ADGRAV_CONTROL_LOADED = "ADGRAV";
	final static String SIOEXPLORER_LOADED = "SIOExplorer";
	final static String USAP_LOADED = "USAP";
	static String MGD77_PATH = PathUtil.getPath("PORTALS/MGD77_PATH",
			MapApp.BASE_URL+"/data/portals/mgd77/");
	protected Object processingTaskLock = new Object();
	protected ProcessingDialog processingDialog = null;

	protected boolean[]loadedTracks = {false,false,false,false,false};
	protected MGGTrack[][]cachedTracks = new MGGTrack[5][];

	private static Dimension compDim = new Dimension(230, 30); 
	private int imported = 0;
	
	public MGGSelector(MGG tracks) {
		this.tracks = tracks;
		initDialog();
	}

	void alignComponent(JComponent comp) {
		comp.setAlignmentX(Component.LEFT_ALIGNMENT);
		comp.setPreferredSize(compDim);
		comp.setMaximumSize(compDim);
	}
	
	void initDialog() {
		
		JPanel dialog = new JPanel( new BorderLayout() );
		dialog.setPreferredSize(new Dimension(230,300));
		dialog.setMaximumSize(new Dimension(230,300));
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel panel2a = new JPanel();
		panel2a.setLayout(new BoxLayout(panel2a, BoxLayout.Y_AXIS));
		JPanel panel2b = new JPanel();
		panel2b.setLayout(new BoxLayout(panel2b, BoxLayout.Y_AXIS));
		
		JButton b;
		// ***** 1.4.4: Add new button to allow creation of user control files
		// from MGD-77 data files
		b = new JButton("<html><body><center>"
				+"Import your own <br>"
				+"MGD77 and MGD77T <br>data files"
				+"</center></body></html>");
		b.setToolTipText("Import one or more MGD77 and MGD77T header and data files");
		b.setActionCommand("Import MGD77");
		b.addActionListener(this);
		b.setPreferredSize(new Dimension(230, 60));
		b.setMaximumSize(new Dimension(230, 60));
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		// GMA 1.4.8: Use new panel
		panel1.add(b);
		
		try {
			String updateURL = PathUtil.getPath("PORTALS/MGD77_PATH")  + "last_NCEI_update_date.txt";
			URL url = URLFactory.url(updateURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));

			//if(in.ready()) {
				// Get Dataset Information
				String updateDate = in.readLine();
				String dateText = "<html>NCEI Content Last Updated:<br><p style='text-align:center'>" + updateDate + "</p></html>";
				textDate = new JLabel(dateText, SwingConstants.CENTER);
				textDate.setFont( new Font( "SansSerif", Font.PLAIN, 13));
				textDate.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

				panel1.add(textDate);
			//}
			in.close();
		} catch(Exception e) {
			System.out.println("NCEI update date file not found");
		}

		// ***** 1.4.4: Add radio buttons that allow user to switch control
		// files
		JLabel label1 = new JLabel("Display:");
		alignComponent(label1);
		panel1.add(label1);
		cruiseDataSource = new JRadioButton[4];
		cruiseDataSource[0] = new JRadioButton("NCEI (NGDC) Tracks");
		cruiseDataSource[1] = new JRadioButton("LDEO Tracks");
		cruiseDataSource[2] = new JRadioButton("USAP Antarctic Tracks");
		cruiseDataSource[3] = new JRadioButton("SIO Explorer Tracks");
		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < cruiseDataSource.length; i++) {
			alignComponent(cruiseDataSource[i]);
			cruiseDataSource[i].addActionListener(this);
			bg.add(cruiseDataSource[i]);
			panel1.add(cruiseDataSource[i]);
		}
		cruiseDataSource[0].setSelected(true);

		
		// Add a key to explain that the yellow track is the one selected
		// and that the red region is the displayed part of the profile
		JPanel key1 = new JPanel();
		alignComponent(key1);
		key1.setLayout(new BoxLayout(key1, BoxLayout.LINE_AXIS));
		JLabel selectedL = new JLabel("Selected Track   ");
		key1.add(selectedL);
		DrawLine selectedLine = new DrawLine(20, 15, 80, 15, MGGData.OFF_COLOR, 6f);
		key1.add(selectedLine);
		JPanel key2 = new JPanel();
		alignComponent(key2);
		key2.setLayout(new BoxLayout(key2, BoxLayout.LINE_AXIS));
		JLabel displayedL = new JLabel("Displayed Profile");
		key2.add(displayedL);
		DrawLine displayedLine = new DrawLine(20, 15, 80, 15, MGGData.ON_COLOR, 6f);
		key2.add(displayedLine);
		// add a bit of space
//		key.add(Box.createRigidArea(new Dimension(0,1)));
//		key.add(Box.createRigidArea(new Dimension(0,1)));
		panel1.add(key1);
		panel1.add(key2);
		
		// ***** 1.6.2: Change the titles of the radio buttons to more clearly
		// convey the data type being selected.
		topoCB = new JCheckBox("Depth", true);
		gravCB = new JCheckBox("Gravity", true);
		magCB = new JCheckBox("Magnetics", true);
		// ***** 1.6.2

		topoCB.addActionListener(this);
		gravCB.addActionListener(this);
		magCB.addActionListener(this);

		// GMA 1.4.8: Utilize new panels and put things in new locations
		panel2a.add(topoCB);
		panel2a.add(gravCB);
		panel2a.add(magCB);

		b = new JButton("All");
		panel2b.add(b);
		b.addActionListener(this);
		noneSelected = new JButton("None");
		panel2b.add(noneSelected);
		noneSelected.addActionListener(this);
		panel2.add(panel2a, BorderLayout.WEST);
		panel2.add(panel2b, BorderLayout.CENTER);

		panel1.add(panel2);
		dialog.add(panel1, "Center");
		dialogPane = dialog;
		loadedControlFiles = NGDC_CONTROL_LOADED;
	}


	public void actionPerformed(ActionEvent evt) {

		String cmd = evt.getActionCommand();
		if (cmd.equals("All")) {
			topoCB.setSelected(true);
			gravCB.setSelected(true);
			magCB.setSelected(true);
			for(int i = 0; i < cruiseDataSource.length; i++) {
				if(cruiseDataSource[i].isSelected()){
					tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					cmd = cruiseDataSource[i].getActionCommand();
					
				}
			}
		} else if (cmd.equals("None")) {
			topoCB.setSelected(false);
			gravCB.setSelected(false);
			magCB.setSelected(false);
			for(int i = 0; i < cruiseDataSource.length; i++) {
				if(cruiseDataSource[i].isSelected()){
					tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					cmd = cruiseDataSource[i].getActionCommand();
					
				}
			}
		}else if(cmd.equals("Depth")){
			for (int i = 0; i < cruiseDataSource.length; i++) {
				if(cruiseDataSource[i].isSelected()){
					tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					cmd = cruiseDataSource[i].getActionCommand();
					
				}
			}
		}else if(cmd.equals("Gravity")){
			for (int i = 0; i < cruiseDataSource.length; i++) {
				if(cruiseDataSource[i].isSelected()){
					tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					cmd = cruiseDataSource[i].getActionCommand();
					
				}
			}
		}else if(cmd.equals("Magnetics")){
			for (int i = 0; i < cruiseDataSource.length; i++) {
				if(cruiseDataSource[i].isSelected()){
					tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					cmd = cruiseDataSource[i].getActionCommand();
					
				}
			}
		}

		// 1.4.4: Allows selection of multiple MGD-77 data files and creates
		// control file from them, loading it immediately into the current environment
		if (cmd.equals("Import MGD77")) {

			FileNameExtensionFilter filter = new FileNameExtensionFilter("*.a77, *.m77, *.m77t, *.mgd77, *.mgdt77, *.h77, *.h77t", 
					"a77", "m77", "m77t", "mgd77", "mgd77t", "h77", "h77t");			
			JFileChooser jfc = MapApp.getFileChooser();
			jfc.resetChoosableFileFilters();
			jfc.setDialogTitle("Choose MGD77 data files to create control file for");
			jfc.setFileFilter(filter);
			jfc.setMultiSelectionEnabled(true);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int c = jfc.showOpenDialog(dialogPane);
			if (c == JFileChooser.CANCEL_OPTION
					|| c == JFileChooser.ERROR_OPTION)
				return;
			File[] selectedFiles = jfc.getSelectedFiles();
			ArrayList<String> loadedFiles = new ArrayList<String>();
			MGG.MGG_control_dir.mkdir();
			
			for (File inFile : selectedFiles) {
				String extension = inFile.getName().substring(inFile.getName().lastIndexOf('.'));
				String leg = inFile.getName().replace(extension, "");
				
				File outputControlFile = new File(MGG.MGG_control_dir, "/mgg_control_"+ leg);
				
				boolean isHeader  = extension.equals(".h77") || extension.equals(".h77t");
				
				
				File outputDataFile = new File(MGG.MGG_data_dir, "/mgg_data_"+ leg);
				File outputHeaderFile = new File(MGG.MGG_header_dir, inFile.getName());
				if(!isHeader && outputDataFile.exists()) {
					int o = JOptionPane.showConfirmDialog(jfc, "A data file for " + leg + " has already been imported, Overwrite?", 
							"File exists", JOptionPane.YES_NO_OPTION);
					if(o == JOptionPane.NO_OPTION)
						continue;
					tracks.removeImported(leg);
					removeImportedFromCache(leg);
					imported = 0;
				}
				if(isHeader && outputHeaderFile.exists()) {
					int o = JOptionPane.showConfirmDialog(jfc, "A header file for " + leg + " has already been imported, Overwrite?", 
							"File exists", JOptionPane.YES_NO_OPTION);
					if(o == JOptionPane.NO_OPTION)
						continue;
				}
				
				CreateMGGControlFile mggControl = new CreateMGGControlFile(
						inFile, MGG.MGG_control_dir, outputControlFile);
				try {
					if (mggControl.createControlFile())	loadedFiles.add(inFile.getName());
					else if (new File(MGG.MGG_header_dir, leg+".h77").exists()) {
						// not able to create control file, but did extract the header
						isHeader = true;
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(dialogPane, "Not able to import: " + inFile, "Import Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (isHeader) {
					if (!tracks.contains(leg)) {
						if (!tracks.getContainsImported()) {
							tracks.add(MGG.IMPORT_TRACK_LINE);
							addToCachedTracks(MGG.IMPORT_TRACK_LINE);
						}
						
						MGGTrack newLine = new MGGTrack( new TrackLine( leg + " (header only)",new Rectangle2D.Double(),
								new ControlPt.Float[0][0] , 0, 0, (byte)0x7, 0));
						tracks.add(newLine);
						addToCachedTracks(newLine);
					}
				
				}
				else {
					try {
						
						DataInputStream in = new DataInputStream(
								new BufferedInputStream(new FileInputStream(
										outputControlFile)));
		
						Point2D.Double pt = new Point2D.Double();
						double wrap = tracks.map.getWrap();
						double wraptest = wrap / 2.;
						double xtest = 0d;
						Projection proj = tracks.map.getProjection();
						String name = "";
	
						while (true) {
							try {
								name = in.readUTF();
							} catch (EOFException ex) {
								break;
							}
		
							int nseg = in.readInt();
							ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
							byte types = 0;
							int type1 = 999;
							int type2 = 999;
							int type3 = 999;
							type1 = in.readInt();
							type2 = in.readInt();
							type3 = in.readInt();
		//					System.out.println(name + "\t" + type1 + "\t" + type2 + "\t" + type3);
		
							if (type1 != 0)
								types |= (byte) 0x4;
							if (type2 != 0)
								types |= (byte) 0x2;
							if (type3 != 0)
								types |= (byte) 0x1;
		
							int start = in.readInt();
							int end = in.readInt();
		
							Rectangle2D.Double bounds = new Rectangle2D.Double();
							for (int i = 0; i < nseg; i++) {
								int a = in.readInt();
								cpt[i] = new ControlPt.Float[a];
		
								for (int j = 0; j < cpt[i].length; j++) {
									pt.x = 1.e-6 * (double) in.readInt();
									pt.y = 1.e-6 * (double) in.readInt();
		
									Point2D.Double p = (Point2D.Double) proj
											.getMapXY(pt);
		
									if (j == 0 && i == 0) {
										bounds.x = p.x;
										bounds.y = p.y;
										bounds.width = 0.;
										bounds.height = 0.;
										xtest = p.x;
									} else {
										if (wrap > 0.) {
											while (p.x > xtest + wraptest)
												p.x -= wrap;
											while (p.x < xtest - wraptest)
												p.x += wrap;
										}
										if (p.x < bounds.x) {
											bounds.width += bounds.x - p.x;
											bounds.x = p.x;
											xtest = bounds.x + .5 * bounds.width;
										} else if (p.x > bounds.x + bounds.width) {
											bounds.width = p.x - bounds.x;
											xtest = bounds.x + .5 * bounds.width;
										}
										if (p.y < bounds.y) {
											bounds.height += bounds.y - p.y;
											bounds.y = p.y;
										} else if (p.y > bounds.y + bounds.height) {
											bounds.height = p.y - bounds.y;
										}
									}
									cpt[i][j] = new ControlPt.Float((float) p.x,
											(float) p.y);
								}
							}
							if (!tracks.isValidBounds(bounds))
								continue;							
							
							if (!tracks.getContainsImported()) {
								tracks.add(MGG.IMPORT_TRACK_LINE);
								addToCachedTracks(MGG.IMPORT_TRACK_LINE);
							}
	
							// remove any header only files for this track that might already be in the list
							if (tracks.contains(leg + " (header only)")) {
								tracks.removeImported(leg + " (header only)");
								removeImportedFromCache(leg + " (header only)");
//								imported--;
							}
								
							MGGTrack newLine = new MGGTrack(new TrackLine(name, bounds, cpt,
									start, end, types, (int) wrap));
							tracks.add(newLine);
							addToCachedTracks(newLine);
							in.close();
						}
					} catch (IOException ex) {}
				}
			}
			
			
			//update the list box

			//rebuild the model with the added tracks(s)
			tracks.rebuildModel();
			
			//for some reason, need to request the focus back if importing a second time
			//otherwise scroll box is blank
			if (imported > 0) {
				dialogPane.requestFocus();
			}
			int lastIndex = tracks.display.cruiseL.getModel().getSize() - 1;
			if (lastIndex >=0) {	
				tracks.display.cruiseL.setSelectedIndex(lastIndex);		
				tracks.display.cruiseL.ensureIndexIsVisible(lastIndex);
				imported++;
			}
			
			if (loadedFiles.size() > 0) {
				String filesString = "";
				for (String file : loadedFiles) {
					String extension = file.substring(file.lastIndexOf('.'));
					String leg = file.replace(extension, "");					
					boolean isHeader  = extension.equals(".h77") || extension.equals(".h77t");
					//if header, check whether we have the corresponding data file
					if (isHeader) {
						File dataFile = new File(MGG.MGG_data_dir, "/mgg_data_"+ leg);
						if (dataFile.exists()) {
							filesString += "\n" + file;
						} else {
							filesString += "\n" + file + " (Header file only: Import corresponding data file to display profile)";
						}
					} else filesString += "\n" + file;;
					
				}
						
				JOptionPane.showMessageDialog(tracks.getDataDisplay(), "The following file(s) have been imported: " + filesString);
				//need to request the focus again to get the cruise list to scroll to the bottom
				if (imported > 1) {
					dialogPane.requestFocus();
				}
			}
		} else if ( cmd.equals("LDEO Tracks") || cmd.equals("NCEI (NGDC) Tracks") ||
					cmd.equals("Display ADGRAV Tracks") || cmd.equals("SIO Explorer Tracks") ||
					cmd.equals("USAP Antarctic Tracks")) {

			final URL url[] = { null, null, null, null, null };
			url[0] = null;
			boolean loadNewControlFile = false;
			if (cmd.equals("LDEO Tracks")) {
				textDate.setVisible(false);
				if (loadedControlFiles.compareTo(LAMONT_CONTROL_LOADED) != 0) {
					try {
						url[0] = URLFactory.url(MGD77_PATH + MGGSelector.LAMONT_CONTROL_LOADED.toLowerCase() + "-mgd77/control/mgg_control_" + MGGSelector.LAMONT_CONTROL_LOADED);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					loadNewControlFile = true;
					
					if(loadedTracks[0]==false){
						loadedTracks[0]=true;
					}
					else{
						tracks.tracks = new MGGTrack[2900];
						tracks.size = 0;
						tracks.loaded = false;
						tracks.model.clearTracks();
						loadNewControlFile = false;
						tracks.tracks=cachedTracks[0];
						tracks.size = cachedTracks[0].length;
						tracks.loaded = true;
						tracks.trim();
						tracks.model.clearTracks();
						for (int i = 0; i < tracks.tracks.length; i++) {
							tracks.model.addTrack(tracks.tracks[i], i);
						}

						tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					}

					loadedControlFiles = LAMONT_CONTROL_LOADED;
					tracks.newTrackSet(loadedControlFiles);
				}
			}
			else if (cmd.equals("Display ADGRAV Tracks")) {
				textDate.setVisible(false);
				if (loadedControlFiles.compareTo(ADGRAV_CONTROL_LOADED) != 0) {
					try {
						url[0] = URLFactory.url(MGD77_PATH + MGGSelector.ADGRAV_CONTROL_LOADED.toLowerCase() + "-mgd77/control/mgg_control_" + MGGSelector.ADGRAV_CONTROL_LOADED);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					loadNewControlFile = true;

					if(loadedTracks[2]==false){
						loadedTracks[2]=true;
					}
					else{
						tracks.tracks = new MGGTrack[2900];
						tracks.size = 0;
						tracks.loaded = false;
						tracks.model.clearTracks();
						loadNewControlFile = false;
						tracks.tracks = cachedTracks[2];
						tracks.size = cachedTracks[2].length;
						tracks.loaded = true;
						tracks.trim();
						tracks.model.clearTracks();
						for (int i = 0; i < tracks.tracks.length; i++) {
							tracks.model.addTrack(tracks.tracks[i], i);
						}

						tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					}

					loadedControlFiles = ADGRAV_CONTROL_LOADED;
					tracks.newTrackSet(loadedControlFiles);
				}
			}
			else if (cmd.equals("SIO Explorer Tracks")) {
				textDate.setVisible(false);
				if (loadedControlFiles.compareTo(SIOEXPLORER_LOADED) != 0) {
					try {
						url[0] = URLFactory.url(MGD77_PATH + MGGSelector.SIOEXPLORER_LOADED.toLowerCase() + "-mgd77/control/mgg_control_" + MGGSelector.SIOEXPLORER_LOADED);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					loadNewControlFile = true;
					if(loadedTracks[3]==false){
						loadedTracks[3]=true;
					}
					else{
						tracks.tracks = new MGGTrack[2900];
						tracks.size = 0;
						tracks.loaded = false;
						tracks.model.clearTracks();
						tracks.tracks = cachedTracks[3];
						tracks.size = cachedTracks[3].length;
						loadNewControlFile=false;
						tracks.loaded = true;
						tracks.trim();
						tracks.model.clearTracks();
						for (int i = 0; i < tracks.tracks.length; i++) {
							tracks.model.addTrack(tracks.tracks[i], i);
						}

						tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					}

					loadedControlFiles = SIOEXPLORER_LOADED;
					tracks.newTrackSet(loadedControlFiles);
				}
			}
			else if (cmd.equals("USAP Antarctic Tracks")) {
				textDate.setVisible(false);
				if (loadedControlFiles.compareTo(USAP_LOADED) != 0) {
					try {
						url[0] = URLFactory.url(MGD77_PATH + MGGSelector.USAP_LOADED.toLowerCase() + "-mgd77/control/mgg_control_" + MGGSelector.USAP_LOADED);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

					loadNewControlFile = true;

					if(loadedTracks[4]==false){
						loadedTracks[4] = true;
					}
					else{

						tracks.tracks = new MGGTrack[2900];
						tracks.size = 0;
						tracks.loaded = false;
						tracks.model.clearTracks();
						tracks.tracks = cachedTracks[4];
						tracks.size = cachedTracks[4].length;
						loadNewControlFile=false;
						tracks.loaded = true;
						tracks.trim();
						tracks.model.clearTracks();
						for (int i = 0; i < tracks.tracks.length; i++) {
							tracks.model.addTrack(tracks.tracks[i], i);
						}

						tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					}

					loadedControlFiles = USAP_LOADED;
					tracks.newTrackSet(loadedControlFiles);
				}
			}
			else {
				if (loadedControlFiles.compareTo(NGDC_CONTROL_LOADED) != 0) {
					textDate.setVisible(true);
					try {
						url[0] = URLFactory.url(MGD77_PATH + MGGSelector.NGDC_CONTROL_LOADED.toLowerCase() + "-mgd77/control/mgg_control_" + MGGSelector.NGDC_CONTROL_LOADED );
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}

					loadNewControlFile = true;
					if(loadedTracks[1]==false){
						loadedTracks[1]=true;
					}else{
						tracks.tracks = new MGGTrack[2900];
						tracks.size = 0;
						tracks.loaded = false;
						tracks.model.clearTracks();
						tracks.tracks = cachedTracks[1];
						tracks.size = cachedTracks[1].length;
						loadNewControlFile = false;
						tracks.loaded = true;
						tracks.trim();
						tracks.model.clearTracks();
						for (int i = 0; i < tracks.tracks.length; i++) {
							tracks.model.addTrack(tracks.tracks[i], i);
						}

						tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
					}

					loadedControlFiles = NGDC_CONTROL_LOADED;
					tracks.newTrackSet(loadedControlFiles);
				}
			}

			if (loadNewControlFile) {
				System.gc();
				// Add progress bar while retrieving list of data
					ProcessingDialog ld = new ProcessingDialog(MapApp.getApp().getFrame(), MapApp.getApp().getMap());
					ld.addTask("Retrieving Data", new Thread( new Runnable() {
					public void run() {
				int m = 0;
				tracks.tracks = new MGGTrack[2900];
				tracks.size = 0;
				tracks.loaded = false;
				tracks.model.clearTracks();

				while (m < url.length && url[m] != null) {

					try {
						int nPer360 = 20480;
						DataInputStream in = new DataInputStream(
								new BufferedInputStream(url[m].openStream()));
						int k = 0;
						Point2D.Double pt = new Point2D.Double();
						double wrap = tracks.map.getWrap();
						double wraptest = wrap / 2.;
						double xtest = 0d;
						Projection proj = tracks.map.getProjection();
						String name = "";
						Dimension mapDim = tracks.map.getDefaultSize();

						while (true) {
							try {
								name = in.readUTF();
							} catch (EOFException ex) {
								break;
							}

							int nseg = in.readInt();
							ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
							byte types = 0;

							int type1 = 999;
							int type2 = 999;
							int type3 = 999;
							type1 = in.readInt();
							type2 = in.readInt();
							type3 = in.readInt();
							if (type1 != 0)
								types |= (byte) 0x4;
							if (type2 != 0)
								types |= (byte) 0x2;
							if (type3 != 0)
								types |= (byte) 0x1;

							int start = in.readInt();
							int end = in.readInt();

							Rectangle2D.Double bounds = new Rectangle2D.Double();

							for (int i = 0; i < nseg; i++) {
								int a = in.readInt();
								cpt[i] = new ControlPt.Float[a];

								for (int j = 0; j < cpt[i].length; j++) {
									pt.x = 1.e-6 * (double) in.readInt();
									pt.y = 1.e-6 * (double) in.readInt();

									Point2D.Double p = (Point2D.Double) proj
											.getMapXY(pt);

									if (j == 0 && i == 0) {
										bounds.x = p.x;
										bounds.y = p.y;
										bounds.width = 0.;
										bounds.height = 0.;
										xtest = p.x;
									} else {
										if (wrap > 0.) {
											while (p.x > xtest + wraptest)
												p.x -= wrap;
											while (p.x < xtest - wraptest)
												p.x += wrap;
										}
										if (p.x < bounds.x) {
											bounds.width += bounds.x - p.x;
											bounds.x = p.x;
											xtest = bounds.x + .5
													* bounds.width;
										} else if (p.x > bounds.x
												+ bounds.width) {
											bounds.width = p.x - bounds.x;
											xtest = bounds.x + .5
													* bounds.width;
										}
										if (p.y < bounds.y) {
											bounds.height += bounds.y - p.y;
											bounds.y = p.y;
										} else if (p.y > bounds.y
												+ bounds.height) {
											bounds.height = p.y - bounds.y;
										}
									}
									cpt[i][j] = new ControlPt.Float(
											(float) p.x, (float) p.y);
								}
							}
							if (!tracks.isValidBounds(bounds))
								continue;
//							if (!bounds.intersects(0., 0., mapDim.getWidth(),
//									mapDim.getHeight()))
//								continue;
							tracks.add(new MGGTrack(new TrackLine(name, bounds,
									cpt, start, end, types, (int) wrap)));
							k++;
						}
						in.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					m++;
					} // end while

				DataInputStream in;
				int nPer360 = 20480;
				int k = 0;
				Point2D.Double pt = new Point2D.Double();
				double wrap = tracks.map.getWrap();
				double wraptest = wrap / 2.;
				double xtest = 0d;
				Projection proj = tracks.map.getProjection();
				String name = "";
				Dimension mapDim = tracks.map.getDefaultSize();

				if (MGG.MGG_control_dir.exists()) {
					tracks.add( MGG.IMPORT_TRACK_LINE );
					File[] MGG_control_files = MGG.MGG_control_dir.listFiles();
					for (int nucfs = 0; nucfs < MGG_control_files.length; nucfs++) {
						if (MGG_control_files[nucfs].getName().indexOf(
								"mgg_control") != -1) {
							try {
								in = new DataInputStream(
										new BufferedInputStream(
												new FileInputStream(
														MGG_control_files[nucfs])));
								k = 0;
								pt = new Point2D.Double();
								wrap = tracks.map.getWrap();
								wraptest = wrap / 2.;
								xtest = 0d;
								proj = tracks.map.getProjection();
								name = "";
								mapDim = tracks.map.getDefaultSize();
								while (true) {
									try {
										name = in.readUTF();
									} catch (EOFException ex) {
										break;
									}
									int nseg = in.readInt();
									ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
									byte types = 0;
									int type1 = 999;
									int type2 = 999;
									int type3 = 999;
									type1 = in.readInt();
									type2 = in.readInt();
									type3 = in.readInt();
									if (type1 != 0)
										types |= (byte) 0x4;
									if (type2 != 0)
										types |= (byte) 0x2;
									if (type3 != 0)
										types |= (byte) 0x1;
									int start = in.readInt();
									int end = in.readInt();
									Rectangle2D.Double bounds = new Rectangle2D.Double();
									for (int i = 0; i < nseg; i++) {
										int a = in.readInt();
										cpt[i] = new ControlPt.Float[a];
										for (int j = 0; j < cpt[i].length; j++) {
											pt.x = 1.e-6 * (double) in
													.readInt();
											pt.y = 1.e-6 * (double) in
													.readInt();
											Point2D.Double p = (Point2D.Double) proj
													.getMapXY(pt);
											if (j == 0 && i == 0) {
												bounds.x = p.x;
												bounds.y = p.y;
												bounds.width = 0.;
												bounds.height = 0.;
												xtest = p.x;
											} else {
												if (wrap > 0.) {
													while (p.x > xtest
															+ wraptest)
														p.x -= wrap;
													while (p.x < xtest
															- wraptest)
														p.x += wrap;
												}
												if (p.x < bounds.x) {
													bounds.width += bounds.x
															- p.x;
													bounds.x = p.x;
													xtest = bounds.x + .5
															* bounds.width;
												} else if (p.x > bounds.x
														+ bounds.width) {
													bounds.width = p.x
															- bounds.x;
													xtest = bounds.x + .5
															* bounds.width;
												}
												if (p.y < bounds.y) {
													bounds.height += bounds.y
															- p.y;
													bounds.y = p.y;
												} else if (p.y > bounds.y
														+ bounds.height) {
													bounds.height = p.y
															- bounds.y;
												}
											}
											cpt[i][j] = new ControlPt.Float(
													(float) p.x, (float) p.y);
										}
									}
									if (!tracks.isValidBounds(bounds))
										continue;
//									if (!bounds.intersects(0., 0., mapDim
//											.getWidth(), mapDim.getHeight()))
//										continue;
									tracks.add(new MGGTrack(new TrackLine(name,
											bounds, cpt, start, end, types,
											(int) wrap)));
									k++;
								}
								in.close();
							} catch (IOException ex) {
								ex.printStackTrace();
							}
						}
					}
				}				
				tracks.loaded = true;
				tracks.trim();
				tracks.model.clearTracks();
				for (int i = 0; i < tracks.tracks.length; i++) {
					tracks.model.addTrack(tracks.tracks[i], i);
				}

				tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
				
				if (loadedControlFiles.compareTo(LAMONT_CONTROL_LOADED) == 0){
					cachedTracks[0]=tracks.tracks;
				}
				if(loadedControlFiles.compareTo(NGDC_CONTROL_LOADED)==0){
					cachedTracks[1]=tracks.tracks;
				}
				if(loadedControlFiles.compareTo(ADGRAV_CONTROL_LOADED)==0){
					cachedTracks[2]=tracks.tracks;
				}
				if(loadedControlFiles.compareTo(SIOEXPLORER_LOADED)==0){
					cachedTracks[3]=tracks.tracks;
				}
				if(loadedControlFiles.compareTo(USAP_LOADED)==0){
					cachedTracks[4]=tracks.tracks;
				}
				tracks.newTrackSet(loadedControlFiles);


				}
			}));
			} // end if
			} // end else if
//		if ( evt.getSource().equals(topoCB) || evt.getSource().equals(gravCB) || evt.getSource().equals(magCB) ) {
		//tracks.setTypes(topoCB.isSelected(), gravCB.isSelected(), magCB.isSelected());
		//tracks.newTrackSet(loadedControlFiles);
//		}
	} // end

	public JComponent getDialog() {
		return dialogPane;
	}

	public String getLoadedControlFiles() {
		return loadedControlFiles;
	}

	/*
	 * add a new track to any cached track arrays
	 */
	private void addToCachedTracks(MGGTrack track) {
		for (int i=0; i<cachedTracks.length; i++) {
			MGGTrack[] tracks = cachedTracks[i]; 
			if (tracks != null) {
				int size = tracks.length;
				MGGTrack[] tmp = new MGGTrack[size + 1];
				System.arraycopy(tracks, 0, tmp, 0, size);
				tmp[size] = track;
				cachedTracks[i] = tmp;
			}
		}	
	}
	
	/*
	 * remove any imported tracks that match the input name from the cached tracks
	 */
	private void removeImportedFromCache(String name) {
		
		for (int i=0; i<cachedTracks.length; i++) {
			MGGTrack[] tracks = cachedTracks[i]; 
			if (tracks != null) {
				ArrayList<MGGTrack> tracksList = new ArrayList<MGGTrack>();
				boolean importedFiles = false;
				for (MGGTrack t :  tracks) {
					if ( t.getName().equals(MGG.IMPORT_TRACK_LINE.getName()) ) importedFiles = true;
					if ( importedFiles && t.getName().equals(name) ) continue;
					tracksList.add(t);
				}
				
				tracks = new MGGTrack[tracksList.size()];
				tracks = tracksList.toArray(tracks);
				cachedTracks[i] = tracks;
			}
		}	
	}
}