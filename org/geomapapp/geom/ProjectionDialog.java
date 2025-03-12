package org.geomapapp.geom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import haxby.proj.PolarStereo;
import haxby.util.GeneralUtils;

public class ProjectionDialog implements ItemListener, ChangeListener {
	public static int GEOGRAPHIC_PROJECTION = 0;
	public static int UTM_PROJECTION = 1; 
	Object origMin, origMax;
	JComboBox<String> type;
	JTextField zone,
				maxEdit,
				minEdit,
				maxEditC,
				minEditC,
				noDataField,
				zUnits,
				dataType,
				zScale;
	JCheckBox applyForAll,
			  editSingle,
			  flipGrid,
			  switchToGridAlignment;
	JButton resetToOriginalZ,
			fileInfo;
	JLabel name,
		   wesnLabel,
		   allWESNLabel,
		   minMaxL,
		   maxMinTitle,
		   allRangeWESNTitle,
		   minFloorLabel,
		   maxCeilingLabel,
		   maxMinE,
		   fileName,
		   nxLabel,
		   nyLabel,
		   dxLabel,
		   dyLabel;
	JLabel minZTitle = new JLabel("Min z: ");				// single grid import
	JLabel maxZTitle = new JLabel("Max z: ");				// single grid import
	JLabel floorZTitle = new JLabel("Min Z:    ");			// multi grid import
	JLabel ceilingZTitle = new JLabel("     Max Z:    ");	// multi grid import
	JTextField offsetTF;
	NumberFormat fmt;
	String initialZScale = null;
	String offsetStr = null;
	String[] fileAtt = null;
	int width, height;
	double minZ = 0.0;
	double maxZ = 0.0;
	double floorZ = 0.0;
	double noData = Double.NaN;
	double floorZAllGrids = 0.0;
	double ceilingZAllGrids = 0.0;
	double ceilingZ = 0.0;
	double[] wesn;
	double[] wesnRangeAllGrids;
	double dx, dy;
	JPanel panel,
		   panel1,
		   panel2,
		   panel1a,
		   panel1b,
		   panel1c,
		   panel1ca,
		   panel1d,
		   panel1e,
		   panel1f,
		   panel1g,
		   panel1h,
		   panel1i,
		   panel1j,
		   panel2a,
		   panelUTM,
		   panelZone,
		   edit;
//	***** GMA 1.6.6: Add radio buttons to set hemisphere when UTM projection is selected
//	 Make "panel1" have class scope so radio buttons can be added when UTM projection is selected
	JRadioButton northRB = null;
	JRadioButton southRB = null;
	ButtonGroup hemisphereBG = null;
	JLabel hemisphereLabel = null;
	JLabel emptyLabel = null;

//	***** GMA 1.6.6

	public ProjectionDialog() {
		type = new JComboBox<String>();
		type.addItem("Geographic");
		type.addItem("UTM");
		type.setSelectedIndex(GEOGRAPHIC_PROJECTION);
		
		panel = new JPanel( new BorderLayout() );

//		***** GMA 1.6.6: "panel1" changed to have class scope so radio buttons can be added when UTM projection is selected
		panel1 = new JPanel( new GridLayout(0,1) );
//		***** GMA 1.6.6
		name = new JLabel();

		panel1a = new JPanel(new BorderLayout());
		panel1a.add(new JLabel("File: " ), "West");
		fileName = (new JLabel(""));
		panel1a.add(fileName, "Center");
		fileInfo = new JButton("More Info");
		panel1a.add(fileInfo, "East");
		panel1.add( panel1a );

		panel1.add( new JLabel("Projection:") );
		panel1.add( type );
		
		panelZone = new JPanel(new FlowLayout(0,0,0));
		JLabel zoneLabel = new JLabel("Zone (1-60):");
		zone = new JTextField("", 5);
		panelZone.add(zoneLabel);
		panelZone.add(zone);

		panelUTM = new JPanel( new GridLayout(0,2,22,0) );
		JPanel panelUTMa = new JPanel( new FlowLayout(0,0,0) );
		panelUTMa.add(panelZone);
		
		// Add UTM Section
		hemisphereLabel = new JLabel("UTM Hemisphere: ");
		northRB = new JRadioButton("N");
		southRB = new JRadioButton("S");
		northRB.addChangeListener(this);
		southRB.addChangeListener(this);
		northRB.setSelected(true);
		hemisphereBG = new ButtonGroup();
		hemisphereBG.add(northRB);
		hemisphereBG.add(southRB);
		panelUTMa.add(hemisphereLabel);
		panelUTMa.add(northRB);
		panelUTMa.add(southRB);
		panelUTM.add(panelZone);
		panelUTM.add(panelUTMa);
		panelUTM.setVisible(false);
		panel1.add(panelUTM);
		
		flipGrid = new JCheckBox("Tick to flip grid along horizontal axis", false);
		flipGrid.setVisible(false);
		panel1.add(flipGrid);
		
		switchToGridAlignment = new JCheckBox("Tick to force grid node registration (default: pixel registration)");
		switchToGridAlignment.setVisible(false);
		panel1.add(switchToGridAlignment);
		
		panel1f = new JPanel( new GridLayout(0,2) );
		panel1f.add ( new JLabel("Data type:") );
		dataType = new JTextField("Elevation",5);
		panel1f.add(dataType);
		
		panel1g = new JPanel( new GridLayout(0,2) );
		panel1g.add ( new JLabel("Units for Z values:") );
		zUnits = new JTextField("m",5);
		panel1g.add(zUnits);
		panel1.add(panel1f);
		panel1.add(panel1g);
				
		panel1h = new JPanel( new GridLayout(0,2) );
		panel1h.add( new JLabel("Scale Z values by:") );
		zScale = new JTextField("1", 5);
		panel1h.add(zScale);

		panel1i = new JPanel( new GridLayout(0,2) );
		panel1i.add( new JLabel("Add offset:") );
		offsetTF = new JTextField("0" , 5);
		panel1i.add(offsetTF);
		
		panel1j = new JPanel( new GridLayout(0,2) );
		panel1j.add( new JLabel("NoData value:") );
		noDataField = new JTextField("0" , 5);
		panel1j.add(noDataField);
		panel1.add(panel1h);
		panel1.add(panel1i);
		panel1.add(panel1j);

		// Add WESN Section
		wesnLabel = new JLabel("");
		panel1.add(new JLabel("WESN:"));
		panel1.add(wesnLabel);
		panel1.add(new JLabel(""));

		panel1b = new JPanel( new GridLayout(0,2) );
		panel1b.add(maxZTitle);
		maxEdit = new JTextField("");
		maxEdit.setDisabledTextColor(Color.GRAY);
		maxEdit.setEditable(false);
		maxEdit.setForeground(Color.GRAY);
		panel1b.add(maxEdit);

		panel1d = new JPanel( new GridLayout(0,2) );
		panel1d.add(minZTitle);
		minEdit = new JTextField("");
		minEdit.setEditable(false);
		minEdit.setForeground(Color.GRAY);
		panel1d.add(minEdit);
		

		edit = new JPanel(new BorderLayout());
		editSingle = new JCheckBox("Edit", false);
		minMaxL = new JLabel("");
		edit.add(new JLabel("Current Grid:"), "West");
		resetToOriginalZ = new JButton("Reset");
		edit.add(resetToOriginalZ, "East");
		edit.add(editSingle, "Center");
		panel1.add(edit);
		panel1.add(panel1d);
		panel1.add(panel1b);

		nxLabel = new JLabel("");
		nyLabel = new JLabel("");
		dxLabel = new JLabel("");
		dyLabel = new JLabel("");
		JPanel nodePanel = new JPanel( new GridLayout(0, 3));
		nodePanel.add(new JLabel("Number of Nodes:"));
		nodePanel.add(nxLabel);
		nodePanel.add(nyLabel);
		nodePanel.add(new JLabel("Increments:"));
		nodePanel.add(dxLabel);
		nodePanel.add(dyLabel);
		panel1.add(nodePanel);
		
		//Edit z actions on select and unselect
		editSingle.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(editSingle.isSelected()){
					maxEdit.setEditable(true);
					maxEdit.setForeground(Color.BLACK);
					minEdit.setEditable(true);
					minEdit.setForeground(Color.BLACK);
					// If edit is selected and apply all grids is available then must select
					if(panel2.isShowing()){
						applyForAll.setSelected(true);
					}
					panel.repaint();
				} else {
					maxEdit.setEditable(false);
					maxEdit.setForeground(Color.GRAY);
					minEdit.setEditable(false);
					minEdit.setForeground(Color.GRAY);
					if(panel2.isShowing() && applyForAll.isSelected()) {
						// do nothing
					} else {
						resetOriginalMaxMin(); // Edit off sets back to original value
					}
					panel.repaint();
				}
			}
		});

		//Reset button actions to original max min
		resetToOriginalZ.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					resetOriginalMaxMin();
			}
		});

		// More info button action
		fileInfo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// getting file details to display can set fileAtt
				fileDetails(fileAtt);
			}
		});

		//	***** GMA 1.6.6: Add item listener to "type" to add radio buttons if selected projection is UTM
		type.addItemListener(this);

		type.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setProjection();
			}
		});

		//Update multiple grid Min Z if Min Z is edited
		minEdit.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				minFloorLabel.setText("" + minEdit.getText());			
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				minFloorLabel.setText("" + minEdit.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
		//Update multiple grid Max Z if Max Z is edited
		maxEdit.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				maxCeilingLabel.setText("" + maxEdit.getText());			
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				maxCeilingLabel.setText("" + maxEdit.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
		
		zone.getDocument().addDocumentListener( new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				setProjection();
				if (getProjection() == null) {
					zone.setBackground(Color.YELLOW);
				} else {
					zone.setBackground(Color.WHITE);
				}	
			}
			public void insertUpdate(DocumentEvent e) {
				setProjection();
				if (getProjection() == null) {
					zone.setBackground(Color.YELLOW);
				} else {
					zone.setBackground(Color.WHITE);
				}	
				
			}
			public void changedUpdate(DocumentEvent e) {
				setProjection();
				if (getProjection() == null) {
					zone.setBackground(Color.YELLOW);
				} else {
					zone.setBackground(Color.WHITE);
				}	
			}
		});

		// Set Border Style
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Confirm Projection & Bounds" );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );
		panel1.setBorder(compBorder);

		// Panel 2
		panel2 = new JPanel(new GridLayout(0,1));

		panel1c = new JPanel( new FlowLayout(FlowLayout.LEFT, 0, 0) );
		panel1c.add(floorZTitle);
		minFloorLabel = new JLabel("");
		panel1c.add(minFloorLabel);
		panel1c.add(ceilingZTitle);
		maxCeilingLabel = new JLabel("");
		panel1c.add(maxCeilingLabel); 
		
		panel1ca = new JPanel( new FlowLayout(FlowLayout.LEFT, 0, 0) );
		allWESNLabel = new JLabel("");
		panel1ca.add(allWESNLabel);
		panel1ca.add(new JLabel(""));

		maxMinTitle = new JLabel("All Grids Z Range:");
		allRangeWESNTitle = new JLabel("All Grids WESN Range:");
		panel2.add(maxMinTitle);
		panel2.add(panel1c);
		panel2.add(allRangeWESNTitle);
		panel2.add(panel1ca);
		maxMinE = new JLabel("");


		applyForAll = new JCheckBox("Select To Import All Grids At Once", false);
		panel2.add(applyForAll);
		applyForAll.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!applyForAll.isSelected() && editSingle.isSelected()){
					editSingle.setSelected(false);
					resetOriginalMaxMin();
					maxEdit.setEditable(false);
					maxEdit.setForeground(Color.GRAY);
					minEdit.setEditable(false);
					minEdit.setForeground(Color.GRAY);
				}
				if(applyForAll.isSelected()) {
					maxEdit.setText("" + getCeilingZAll());
					minEdit.setText("" + getFloorZAll());
				} else {
					resetOriginalMaxMin();
				}
			}
		});

		panel.add(panel1, "North");
		panel.add(panel2, "Center");
		Border titledBorder2 = BorderFactory.createTitledBorder( lineBorder, "Importing Multiple Grids: WESN and Z Range" );
		Border compBorder2 = BorderFactory.createCompoundBorder( titledBorder2, emptyBorder );
		panel2.setBorder(compBorder2);

		setProjection();
	}

	public void resetOriginalMaxMin(){
		maxEdit.setText(origMax.toString());
		minEdit.setText(origMin.toString());
	}

	public void fileDetails(String[] fileAtt2){
		final String newline = "\n";
		JTextArea text = new JTextArea(15,100);
		text.setLineWrap(true);

		if(fileAtt2==null){
			String messageNan = "No file details available.\n";
			text.append(messageNan);
		} else {
		/* Goes through the attributes and prints each row on newline.
		 * If line contains extra quotes,tab,newline strips that out.
		 * Rules wont apply in a textarea.
		 */
		for(int p =0; p<fileAtt2.length; p++) {
			String attributeItem=fileAtt2[p];
			attributeItem=attributeItem.replace("\"", "");
			attributeItem=attributeItem.replace("\\t", "");
			if(attributeItem.contains("\\n")) {
				attributeItem=attributeItem.replace("\\n", "SC123");
				String[] itemsArray = attributeItem.split("SC123");
				String itemFormat = itemsArray[0];
				for(int q=1; q<itemsArray.length; q++) {
					itemFormat += " " + itemsArray[q];
				}
				attributeItem=itemFormat;
			}
			text.append(attributeItem + newline);
		}
	}
		text.setEditable(false);
		JScrollPane scroll = new JScrollPane(text);
		scroll.setPreferredSize(new Dimension(350, 200));
		//JOptionPane.showMessageDialog(null, scroll); // with JTextArea
		JOptionPane.showMessageDialog(null, scroll, "File Details", JOptionPane.DEFAULT_OPTION);
	}

	public double getZScale() {
		return Double.parseDouble(zScale.getText());
	}

	public String getZUnits() {
		return zUnits.getText();
	}
	
	public String getDataType() {
		return dataType.getText();
	}
	
	public Boolean getFlipGrid() {
		return flipGrid.isSelected();
	}
	
	public void showFlipGridCheckBox(Boolean tf) {
		flipGrid.setVisible(tf);
	}
	
	public boolean shouldSwitchRegistration() {
		return switchToGridAlignment.isSelected();
	}
	
	public void showPixelNodeCheckBox(boolean shouldShow) {
		switchToGridAlignment.setVisible(shouldShow);
	}
	
	public void setInitialZScale( String inputInitialZScale ) {
		initialZScale = inputInitialZScale;
	}

	public double getOffset() {
		return Double.parseDouble(offsetTF.getText());
	}

	public void setOffset( String inputOffset ) {
		offsetStr = inputOffset;
	}

	public void setfileAtt( String[] gAttributes ) {
		fileAtt = gAttributes;
	}

	public void setMinMaxZ( double inputMinZ, double inputMaxZ ) {
		minZ = inputMinZ;
		maxZ = inputMaxZ;
		//System.out.println("setMinMax " + minZ + " " + maxZ);
	}

	public void removeEditFeature() {
		edit.remove(editSingle);
		}

	public void removeResetFeature() {
		edit.remove(resetToOriginalZ);
		}

	// Used to set the floor and ceiling if there is more then 1 grid
	public void setFloorCeilingZ( double inputFloorZ, double inputCeilingZ ) {
		floorZ = inputFloorZ;
		ceilingZ = inputCeilingZ;
	}

	public void setFloorZAll(double floorOfAllGrids) {
		floorZAllGrids = floorOfAllGrids;
	}

	public double getFloorZAll() {
		return floorZAllGrids;
	}

	public void setWESNRange(double inputMostWest,double inputMostEast, double inputMostSouth, double inputMostNorth) {
		wesnRangeAllGrids = new double[4];
		wesnRangeAllGrids[0] = inputMostWest;
		wesnRangeAllGrids[1] = inputMostEast;
		wesnRangeAllGrids[2] = inputMostSouth;
		wesnRangeAllGrids[3] = inputMostNorth;
	}

	public double[] getWESNAll() {
		return wesnRangeAllGrids;
	}

	public void setCeilingZAll(double celingOfAllGrids) {
		ceilingZAllGrids = celingOfAllGrids;
	}

	public double getCeilingZAll() {
		return ceilingZAllGrids;
	}
	
	public double getMaxEdit() {
		Double a = Double.parseDouble(maxEdit.getText());
		return a;
	}

	public double getMinEdit() {
		Double b = Double.parseDouble(minEdit.getText());
		return b;
	}

	public double[] getWESN(MapProjection proj) {
		if( proj instanceof RectangularProjection ) {
			return wesn;
		}
		proj = (UTM)((UTMProjection)proj).getUTM();
		double[] bounds =new double[4];
		Point2D p = proj.getRefXY( new Point2D.Double(wesn[0],wesn[2]) );
		bounds[0] = bounds[1] = p.getX();
		bounds[2] = bounds[3] = p.getY();
		p = proj.getRefXY( new Point2D.Double(wesn[1],wesn[2]) );
		if( p.getX()<bounds[0] )bounds[0]=p.getX();
		if( p.getX()>bounds[1] )bounds[1]=p.getX();
		if( p.getY()<bounds[2] )bounds[2]=p.getY();
		if( p.getY()>bounds[3] )bounds[3]=p.getY();
		p = proj.getRefXY( new Point2D.Double(wesn[1],wesn[3]) );
		if( p.getX()<bounds[0] )bounds[0]=p.getX();
		if( p.getX()>bounds[1] )bounds[1]=p.getX();
		if( p.getY()<bounds[2] )bounds[2]=p.getY();
		if( p.getY()>bounds[3] )bounds[3]=p.getY();
		p = proj.getRefXY( new Point2D.Double(wesn[0],wesn[3]) );
		if( p.getX()<bounds[0] )bounds[0]=p.getX();
		if( p.getX()>bounds[1] )bounds[1]=p.getX();
		if( p.getY()<bounds[2] )bounds[2]=p.getY();
		if( p.getY()>bounds[3] )bounds[3]=p.getY();
		if( wesn[0]<500000. && wesn[1]>500000. ) {
			p = proj.getRefXY( new Point2D.Double(500000.,wesn[3]) );
			if( p.getX()<bounds[0] )bounds[0]=p.getX();
			if( p.getX()>bounds[1] )bounds[1]=p.getX();
			if( p.getY()<bounds[2] )bounds[2]=p.getY();
			if( p.getY()>bounds[3] )bounds[3]=p.getY();
			p = proj.getRefXY( new Point2D.Double(500000.,wesn[2]) );
			if( p.getX()<bounds[0] )bounds[0]=p.getX();
			if( p.getX()>bounds[1] )bounds[1]=p.getX();
			if( p.getY()<bounds[2] )bounds[2]=p.getY();
			if( p.getY()>bounds[3] )bounds[3]=p.getY();
		}
		if( wesn[2]<0. && wesn[3]>0. ) {
			p = proj.getRefXY( new Point2D.Double( wesn[0], 0. ) );
			if( p.getX()<bounds[0] )bounds[0]=p.getX();
			if( p.getX()>bounds[1] )bounds[1]=p.getX();
			if( p.getY()<bounds[2] )bounds[2]=p.getY();
			if( p.getY()>bounds[3] )bounds[3]=p.getY();
			p = proj.getRefXY( new Point2D.Double( wesn[1], 0. ) );
			if( p.getX()<bounds[0] )bounds[0]=p.getX();
			if( p.getX()>bounds[1] )bounds[1]=p.getX();
			if( p.getY()<bounds[2] )bounds[2]=p.getY();
			if( p.getY()>bounds[3] )bounds[3]=p.getY();
		}
		return bounds;
	}

	void setProjection() {
		// Add formatter to restrict size of WESN strings for display
		NumberFormat fmtWESN = NumberFormat.getInstance();
		fmtWESN.setMaximumFractionDigits(3);
		fmtWESN.setMinimumFractionDigits(3);

		if( type.getSelectedIndex() == GEOGRAPHIC_PROJECTION ) {
			zone.setEnabled(false);
			if( wesn!=null ) {
				// Format WESN strings
				wesnLabel.setText(fmtWESN.format(wesn[0]) + ",\t      " +	// W
								fmtWESN.format(wesn[1]) + ",\t     " +		// E
								fmtWESN.format(wesn[2]) + ",\t     " +		// S
								fmtWESN.format(wesn[3]) );					// N
			}
			if(wesnRangeAllGrids!=null) {
				allWESNLabel.setText(fmtWESN.format(wesnRangeAllGrids[0]) + ",\t      " +	// W
						fmtWESN.format(wesnRangeAllGrids[1]) + ",\t     " +					// E
						fmtWESN.format(wesnRangeAllGrids[2]) + ",\t     " +					// S
						fmtWESN.format(wesnRangeAllGrids[3]) );								// N
			}
		} else if (type.getSelectedIndex() == UTM_PROJECTION) {
			zone.setEnabled(true);
			if( wesn!=null ) {
				wesnLabel.setText(fmtWESN.format(wesn[0]) + ",\t      " +	// W
						fmtWESN.format(wesn[1]) + ",\t     " +				// E
						fmtWESN.format(wesn[2]) + ",\t     " +				// S
						fmtWESN.format(wesn[3]) + " ");						// N
			}
			if(wesnRangeAllGrids!=null) {
				allWESNLabel.setText(fmtWESN.format(wesnRangeAllGrids[0]) + ",\t      " +	// W
						fmtWESN.format(wesnRangeAllGrids[1]) + ",\t     " +					// E
						fmtWESN.format(wesnRangeAllGrids[2]) + ",\t     " +					// S
						fmtWESN.format(wesnRangeAllGrids[3]) );								// N
			}
			
		}
	}

	public MapProjection getProjection() {
		if( type.getSelectedIndex() == GEOGRAPHIC_PROJECTION ) {
//			Geographic projection logic
			return new RectangularProjection( wesn, width, height );
		} else { //if (type.getSelectedIndex() == 1){
//			UTM projection logic
//			***** GMA 1.6.6: Set hemisphere for UTM projection
			boolean northHemisphere = true;
			if ( northRB != null && southRB != null ) {
				if ( southRB.isSelected() ) {
					northHemisphere = false;
				}
			}

//			Assure the UTM zone is an integer between 1 and 60
			int z = -1;
			try {
				z = Integer.parseInt(zone.getText());
				if( z<1 || z>60 )return null;
			} catch(Exception e) {
				return null;
			}

//			***** GMA 1.6.6: Testing hemisphere for the UTM projection
//			UTM utm = new UTM(z, 2, MapProjection.NORTH );

			UTM utm = null;
			if ( northHemisphere ) {
				utm = new UTM(z, 2, MapProjection.NORTH );
//				System.out.println("UTM NORTH");
			}
			else {
				utm = new UTM(z, 2, MapProjection.SOUTH );
//				System.out.println("UTM SOUTH");
			}
//			***** GMA 1.6.6

			return new UTMProjection( wesn[0], 
									wesn[3], 
									(wesn[1] - wesn[0]) / width,
									(wesn[3] - wesn[2]) / height, 
									utm );
		}
	}

	public MapProjection getProjection(double[] wesn, int width, int height) {
		if( type.getSelectedIndex() == GEOGRAPHIC_PROJECTION ) {
//			Geographic projection logic
			return new RectangularProjection( wesn, width, height );
		} else { //if (type.getSelectedIndex() == 1){

//			UTM projection logic
//			***** GMA 1.6.6: Set hemisphere for UTM projection
			boolean northHemisphere = true;
			if ( northRB != null && southRB != null ) {
				if ( southRB.isSelected() ) {
					northHemisphere = false;
				}
			}
//			***** GMA 1.6.6

//			Assure the UTM zone is an integer between 1 and 60
			int z = -1;
			try {
				z = Integer.parseInt(zone.getText());
				if( z<1 || z>60 )return null;
			} catch(Exception e) {
				return null;
			}

//			***** GMA 1.6.6: Testing hemisphere for the UTM projection
//			UTM utm = new UTM(z, 2, MapProjection.NORTH );

			UTM utm = null;
			if ( northHemisphere ) {
				utm = new UTM(z, 2, MapProjection.NORTH );
//				System.out.println("UTM NORTH");
			}
			else {
				utm = new UTM(z, 2, MapProjection.SOUTH );
//				System.out.println("UTM SOUTH");
			}
//			***** GMA 1.6.6

			return new UTMProjection( wesn[0],
										wesn[3],
										(wesn[1] - wesn[0]) / width,
										(wesn[3] - wesn[2]) / height,
										utm );
		}
	}

	public MapProjection getProjection(Component comp, double[] wesn,
			double defaultZScale, int width, int height,
			MapProjection inputPrj, String name) {
		if (name == null)
			this.name.setText("");
		else
			this.name.setText(name);
		return getProjection(comp, wesn, defaultZScale, width, height, inputPrj);
	}

	public MapProjection getProjection(Component comp, double[] wesn, double defaultZScale, int width, int height, MapProjection inputProj) {
		if(switchToGridAlignment.isVisible()) {
			wesn[0] -= dx/2;
			wesn[1] -= dx/2;
			wesn[2] -= dy/2;
			wesn[3] -= dy/2;
		}
		// Check whether the projection looks like UTM, based on the x and y_range values 
		this.wesn = wesn;
		if (wesn[1] > 360 || wesn[3] > 90) {
			setProjectionType(ProjectionDialog.UTM_PROJECTION);
		}
		
		if ( initialZScale != null ) {
			zScale.setText(initialZScale);
		}
		initialZScale = null;

		if ( offsetStr != null ) {
			offsetTF.setText(offsetStr);
		}
		offsetStr = null;

		applyForAll.setSelected(false);

		minMaxL.setText("Max z: " + maxZ + "    Min z: " + minZ);
		origMax = maxZ;
		origMin = minZ;
		maxEdit.setText(GeneralUtils.formatToSignificant(maxZ,5));
		minEdit.setText(GeneralUtils.formatToSignificant(minZ,5));
		minZ = 0.0;
		maxZ = 0.0;
		
		noDataField.setText(Double.toString(noData));

		// If both ceiling and floor is zero file length is only 1
		if((ceilingZ == 0) && (floorZ == 0)){
			//Remove all CF max min from panel
			panel1.remove(maxMinTitle);
			panel1.remove(panel2);
			//panel1.remove(maxMinE);

			// Remove edit checkbox for max/min Z
			editSingle.setSelected(false);
			maxEdit.setEditable(false);
			maxEdit.setForeground(Color.GRAY);
			minEdit.setEditable(false);
			minEdit.setForeground(Color.GRAY);

			// Remove apply to all grid option
			panel.remove(panel2);
			panel.repaint();
		} else if (ceilingZ != floorZ) {
			maxCeilingLabel.setText("" + ceilingZ);
			minFloorLabel.setText("" + floorZ);
			setFloorZAll(floorZ);
			setCeilingZAll(ceilingZ);

			ceilingZ = 0.0;
			floorZ = 0.0;
		}

		nxLabel.setText("nx: " + width);
		nyLabel.setText("ny: " + height);
		
		this.width = width;
		this.height = height;
		this.zScale.setText(defaultZScale+"");
		this.fileName.setText( this.name.getText());

		if (inputProj instanceof UTMProjection) { 
			type.setSelectedIndex(UTM_PROJECTION);
			zone.setText(((UTMProjection) inputProj).getUTM().getZone() + "");
		}
		else if (inputProj instanceof UTM) {
			type.setSelectedIndex(UTM_PROJECTION);
			zone.setText(((UTM)inputProj).getZone()+"");
		}

		setProjection();
		if(!switchToGridAlignment.isVisible()) {
			if(dx == 0.0) setDx((wesn[1]-wesn[0])/width);
			if(dy == 0.0) setDy((wesn[3]-wesn[2])/height);
		}
		dxLabel.setText("dx: " + dx);
		dyLabel.setText("dy: " + dy);
		// GMA 1.6.6: Add grid name to window title
		int ok = JOptionPane.showConfirmDialog( comp, panel, "Confirm Projection & Bounds: " +
							name.getText(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );

		// No action on cancel or close
		if( ok==JOptionPane.CANCEL_OPTION || ok==JOptionPane.CLOSED_OPTION){
			return null;
		}
//
		if( ok==JOptionPane.OK_OPTION){
			if((ceilingZ != 0) && (floorZ != 0)){
				double aMax = Double.parseDouble(maxEdit.getText().toString());
				double bMin = Double.parseDouble(minEdit.getText().toString());
				double cMax = Double.parseDouble(maxCeilingLabel.getText().toString());
				double dMin = Double.parseDouble(minFloorLabel.getText().toString());
				// Check max bounds, reset to original max if out of bound
				if(cMax < aMax){
					System.out.println("Out of Bounds: " + aMax + "Reset.");
					maxEdit.setText(String.valueOf(cMax));
				}
				// Check min bounds, reset to original min if out of bound
				if(bMin < dMin){
					System.out.println("Out of Bounds: " + bMin + "Reset.");
					minEdit.setText(String.valueOf(dMin));
				}
			}
		}
		
		if(shouldSwitchRegistration()) {
			wesn[0] += dx/2;
			wesn[1] += dx/2;
			wesn[2] += dy/2;
			wesn[3] += dy/2;
			this.wesn = wesn;
		}

		MapProjection proj = getProjection();
		if( proj==null ) {
			JOptionPane.showMessageDialog(comp, "Zone must be 1-60. Try Again");
			return getProjection( comp, wesn, defaultZScale, width, height, inputProj);
		}
		double zmax = Double.parseDouble(maxEdit.getText());
		double zmin = Double.parseDouble(minEdit.getText());
		double zscale = Double.parseDouble(zScale.getText());
		try {
			noData = Double.parseDouble(noDataField.getText());
		}
		catch(Exception e) {
			noData = Double.NaN;
		}
		if (((zmax - zmin)  * zscale) > Short.MAX_VALUE || 
				((zmax - zmin)  * zscale) < Short.MIN_VALUE) {
			String msg = "GeoMapApp could not import this grid: The Z range is too large."
					+ "\nPlease check the Min Z and Max Z values of the grid."
					+ "\nOr, edit the Z values in the Confirm Projection & Bounds window.";

			JOptionPane.showMessageDialog(comp, msg);
			return getProjection( comp, wesn, defaultZScale, width, height, inputProj);
		}
		return proj;
	}

	public static void main(String[] args) {
		ProjectionDialog d = new ProjectionDialog();
		d.getProjection( (Component)null, new double[] {400000., 550000., -6000000., -2000000.}, 1, 1500, 4000, null);
		d.getPolarProjection(null, 100, 1, "ABC", false, 18000, 18000);
		System.exit(0);
	}

	public void itemStateChanged(ItemEvent arg0) {
	/* GMA 1.6.6: Display hemisphere radio buttons when UTM projection is selected
	 * Put "northRB" and "southRB" into same ButtonGroup so only one can be selected at a time
	 */
		if (type.getSelectedIndex() == UTM_PROJECTION) {
			panelUTM.setVisible(true);
			if (getProjection() == null) zone.setBackground(Color.YELLOW); 

		} else if (type.getSelectedIndex() == GEOGRAPHIC_PROJECTION) {
			panelUTM.setVisible(false);
			zone.setBackground(Color.WHITE); 
		}
	}

	public void stateChanged(ChangeEvent ce) {
		// ***** GMA 1.6.6: Recalculate WESN values when hemisphere radio button is selected
		if ( ce.getSource().equals(northRB) || ce.getSource().equals(southRB) ) {
			setProjection();
		}
	}

	public MapProjection getPolarProjection(Component comp, double dx, double defaultZScale, String name, boolean southPole) {
		return getPolarProjection(comp, dx, defaultZScale, name, southPole, 0, 0);
	}
	
	public MapProjection getPolarProjection(Component comp, double dx, double defaultZScale, String name, boolean southPole, int width, int height) {
		if (name == null)
			this.name.setText("");
		else
			this.name.setText(name);

		JPanel p = new JPanel(new GridLayout(0, 3));
		ButtonGroup bg = new ButtonGroup();
		p.add(new JLabel("Pole: "));
		JRadioButton b = new JRadioButton("N", !southPole); p.add(b); bg.add(b);
		b = new JRadioButton("S", southPole); p.add(b); bg.add(b);

		JComboBox<String> cb = new JComboBox<String>(GCTP_Constants.SPHEROID_NAMES);
		cb.setSelectedIndex(GCTP_Constants.WGS84);
		p.add(new JLabel("Ellipsoid"));
		p.add(cb);
		p.add(new JLabel(""));

		FocusListener focusSelect = new FocusListener() {
			public void focusLost(FocusEvent e) {
				((JTextField)e.getSource()).select(0, 0);
			}
			public void focusGained(FocusEvent e) {
				((JTextField)e.getSource()).selectAll();
			}
		};

		p.add( new JLabel("Scale z: "));
		JTextField zScale = new JTextField(defaultZScale+"");
		if ( initialZScale != null ) {
			zScale.setText(initialZScale);
		}
		initialZScale = null;
		p.add(zScale);
		zScale.addFocusListener(focusSelect);
		p.add(new JLabel(""));

		p.add( new JLabel("Add z offset: "));
		JTextField offsetTF = new JTextField("0");
		if ( offsetStr != null ) {
			offsetTF.setText(offsetStr);
		}
		offsetStr = null;
		p.add(offsetTF);
		offsetTF.addFocusListener(focusSelect);
		p.add( new JLabel("") );

		p.add( new JLabel("Central Meridian: "));
		JTextField lon = new JTextField(southPole ? "180" : "0"); 
		p.add(lon); 
		lon.addFocusListener(focusSelect);
		p.add(new JLabel(""));

		p.add( new JLabel("Latitude of Origin: "));
		JTextField lat = new JTextField(southPole ? "-90" : "90"); 
		p.add(lat); 
		lat.addFocusListener(focusSelect);
		p.add(new JLabel(""));

		p.add( new JLabel("Grid Tangent Latitude: "));
		JTextField scaleLatText = new JTextField(southPole ? "-90" : "90"); 
		p.add(scaleLatText); 
		scaleLatText.addFocusListener(focusSelect);
		p.add(new JLabel(""));

		if ( minZ != maxZ ) {
			p.add(new JLabel("Max z: " + maxZ + "   Min z: " + minZ));
			p.add(new JLabel(""));
			minZ = 0.0;
			maxZ = 0.0;
		}
		
		if (width != 0 && height != 0) {
			p.add(new JLabel("Number of Nodes: "));
			p.add(new JLabel("nx: " + width));
			p.add(new JLabel("ny: " + height));
		}
		
		int ok = JOptionPane.showConfirmDialog( comp, p, "Confirm Projection & Bounds: " + this.name.getText(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
		// No action on cancel or close
		if( ok==JOptionPane.CANCEL_OPTION || ok==JOptionPane.CLOSED_OPTION){
			return null;
		}

		this.zScale.setText( zScale.getText() );
		this.offsetTF.setText( offsetTF.getText() );

		boolean northPole = !b.isSelected();
		double refLon = Double.parseDouble(lon.getText());
		double refLat = Double.parseDouble(lat.getText());
		double scaleLat = Double.parseDouble(scaleLatText.getText());
		int ellipsoid = cb.getSelectedIndex();

		if (northPole) {
			PolarStereo projA = new PolarStereo(new Point(0,0),
					0,
					dx,
					scaleLat,
					PolarStereo.NORTH,
					ellipsoid);

			double poleY = projA.getMapXY(0, refLat).getY(); 

			return new PolarStereo(new Point2D.Double(0,-poleY),
					refLon,
					dx,
					scaleLat,
					PolarStereo.NORTH,
					ellipsoid);
		} else {
			PolarStereo projA = new PolarStereo(new Point(0,0),
					180,
					dx,
					scaleLat,
					PolarStereo.SOUTH,
					ellipsoid);
			double poleY = projA.getMapXY(0, refLat).getY();

			return new PolarStereo(new Point2D.Double(0,poleY),
					refLon,
					dx,
					scaleLat,
					PolarStereo.SOUTH,
					ellipsoid);
		}
	}

	public boolean getApplyForAll() {
		return applyForAll.isSelected();
	}
	
	public void setProjectionType(int proj) {
		type.setSelectedIndex(proj);
	}

	public void setNoData(double fillValue) {
		noData = fillValue;
		
	}

	public Double getNoData() {
		return noData;
	}
	
	public void setDx(double newDx) {
		dx = newDx;
	}
	
	public double getDx() {
		return dx;
	}
	
	public void setDy(double newDy) {
		dy = newDy;
	}
	
	public double getDy() {
		return dy;
	}
}