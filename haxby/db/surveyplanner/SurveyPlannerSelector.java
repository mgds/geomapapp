package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import org.geomapapp.geom.XYZ;
import org.geomapapp.util.Cursors;

import haxby.db.custom.DBInputDialog;
import haxby.db.custom.UnknownDataSet;
import haxby.db.dig.Digitizer;
import haxby.db.dig.LineSegmentsObject;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;

public class SurveyPlannerSelector implements ActionListener, MouseListener, MouseMotionListener, KeyListener, ChangeListener {
	private JPanel dialog;
	private JButton submitB, clearB, helpB, elevationB;
	private JTextField startLatTF, startLonTF, endLatTF, endLonTF, numLinesTF, gapTF, speedTF,
						swathTF, overlapTF;
	private JCheckBox autoGapCB;
	private JRadioButton waypointsRB, importWaypointsRB, surveyLinesRB, mouseRB, manualRB,
						straightLineRB, greatCircleRB, importLinesRB;
	private JLabel firstLineL, startL, endL, generateL, swathL, overlapL, gapL, dirL;
	private ButtonGroup surveyBG, firstLineBG;
	private DirectionCirclePanel dirCircle1, dirCircle2;
	private SurveyPlanner sp;
	private XMap map;
	public static final float EARTH_RADIUS = 6371;
	public static final double EARTH_CIRCUMFERENCE = 2 * Math.PI * EARTH_RADIUS;
	private Point p0;
	private Line2D currentLine, line;
	private GeneralPath currentPath;
	private NumberFormat df = new DecimalFormat("#0.00000");
	private NumberFormat dp3f = new DecimalFormat("#0.000");
	private NumberFormat dp2f = new DecimalFormat("#0.00");
	private NumberFormat dp1f = new DecimalFormat("#0.0");
	private boolean autogap = false;
	private static Dimension compDim = new Dimension(250, 30); 
	private boolean flippedCircles = false;
	private Digitizer dig;
	JToggleButton gridToggle;
	private String surveyType;
	
	public SurveyPlannerSelector(SurveyPlanner sp, Digitizer dig) {
		this.sp = sp;
		this.dig = dig;
		this.dig.setSurveyPlanner(true);
		p0 = null;
		currentLine = line = null;
		currentPath = null;
		autogap = false;
		flippedCircles = false;
		surveyType = "";
		map = sp.getMap();
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		gridToggle = ((MapApp)map.getApp()).getMapTools().getGridDialog().getToggle();
		gridToggle.addChangeListener(this);
		initDialog();
	}

	void initDialog() {
		if (dialog == null) {
			dialog = new JPanel( new BorderLayout() );
			dialog.setPreferredSize(new Dimension(250,650));
			dialog.setMaximumSize(new Dimension(250,650));
		} else dialog.removeAll();
	
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));

		JPanel panel2 = new JPanel(new GridLayout(0,1));
		panel2.setMaximumSize(new Dimension(2000, 150));
		helpB = new JButton("Help");
		helpB.addActionListener(this);
		panel2.add(helpB);

		waypointsRB = new JRadioButton("Generate waypoints");
		waypointsRB.setActionCommand("waypoints");
		waypointsRB.addActionListener(this);
		importWaypointsRB = new JRadioButton("Import waypoints");
		importWaypointsRB.setActionCommand("importWaypoints");
		importWaypointsRB.addActionListener(this);
		surveyLinesRB = new JRadioButton("Generate survey lines");
		surveyLinesRB.setActionCommand("lines");
		surveyLinesRB.addActionListener(this);
		surveyLinesRB.setSelected(true);
		importLinesRB = new JRadioButton("Import survey lines");
		importLinesRB.setActionCommand("import");
		importLinesRB.addActionListener(this);
		surveyBG = new ButtonGroup();
		surveyBG.add(waypointsRB);
		surveyBG.add(importWaypointsRB);
		surveyBG.add(surveyLinesRB);
		surveyBG.add(importLinesRB);
		panel2.add(waypointsRB);
		panel2.add(importWaypointsRB);
		panel2.add(surveyLinesRB);
		panel2.add(importLinesRB);
		if (!sp.waypoints && surveyType != null && surveyType.contains("import")) {
			panel2.add(Box.createRigidArea(new Dimension(0,50)));
		}
		GeneralUtils.setButtonGroup(surveyType, surveyBG.getElements());
		panel1.add(panel2);
		
		if (!sp.waypoints) {
			if (surveyType == null || !surveyType.contains("import")) {
				JPanel panel3 = new JPanel(new GridLayout(0,1));
				firstLineL = new JLabel("<html><strong>Define first survey line:</strong></html>");
				panel3.add(firstLineL);
				JPanel firstLineP = new JPanel();
				firstLineP.setLayout(new BoxLayout(firstLineP, BoxLayout.LINE_AXIS));
				manualRB = new JRadioButton("Manually");
				manualRB.setActionCommand("manual");
				manualRB.addActionListener(this);
				manualRB.setSelected(true);
				mouseRB = new JRadioButton("Use mouse");
				mouseRB.setActionCommand("mouse");
				mouseRB.addActionListener(this);
				firstLineBG = new ButtonGroup();
				firstLineBG.add(manualRB);
				firstLineBG.add(mouseRB);
				firstLineP.add(manualRB);
				firstLineP.add(mouseRB);
				panel3.add(firstLineP);
				
				startL = new JLabel("Start point:");
				panel3.add(startL);
				JPanel startP = new JPanel();
				startP.setLayout(new BoxLayout(startP, BoxLayout.LINE_AXIS));
				JLabel startLonL = new JLabel("Lon");
				startLonTF = new JTextField(5);
				startLonTF.addKeyListener(this);
				JLabel startLatL = new JLabel("Lat");
				startLatTF = new JTextField(5);
				startLatTF.addKeyListener(this);
				startP.add(startLonL);
				startP.add(startLonTF);
				startP.add(startLatL);
				startP.add(startLatTF);
				panel3.add(startP);
				
				endL = new JLabel("End point:");
				panel3.add(endL);
				JPanel endP = new JPanel();
				endP.setLayout(new BoxLayout(endP, BoxLayout.LINE_AXIS));
				JLabel endLonL = new JLabel("Lon");
				endLonTF = new JTextField(5);
				endLonTF.addKeyListener(this);
				JLabel endLatL = new JLabel("Lat");
				endLatTF = new JTextField(5);
				endLatTF.addKeyListener(this);
				endP.add(endLonL);
				endP.add(endLonTF);
				endP.add(endLatL);
				endP.add(endLatTF);
				panel3.add(endP);
				panel1.add(panel3);
			}
			JPanel panel4 = new JPanel(new GridLayout(0,1));
			panel4.setMaximumSize(new Dimension(2000, 60));
			JPanel lineTypeP = new JPanel();
			lineTypeP.setLayout(new BoxLayout(lineTypeP, BoxLayout.LINE_AXIS));
			straightLineRB = new JRadioButton("Straight Line");
			straightLineRB.setActionCommand("straightLine");
			straightLineRB.addActionListener(this);
			straightLineRB.setSelected(SurveyLine.getIsStraightLine());
			greatCircleRB = new JRadioButton("Great Circle");
			greatCircleRB.setActionCommand("greatCircle");
			greatCircleRB.setSelected(!SurveyLine.getIsStraightLine());
			greatCircleRB.addActionListener(this);
			ButtonGroup lineTypeBG = new ButtonGroup();
			lineTypeBG.add(straightLineRB);
			lineTypeBG.add(greatCircleRB);
			lineTypeP.add(greatCircleRB);
			lineTypeP.add(straightLineRB);
			panel4.add(lineTypeP);
			
			JPanel speedP = new JPanel();
			speedP.setLayout(new BoxLayout(speedP, BoxLayout.LINE_AXIS));		
			JLabel speedL = new JLabel("Ship speed (knots)");
			speedTF = new JTextField();
			speedTF.setPreferredSize(new Dimension(50,23));
			speedTF.setMaximumSize(new Dimension(50,23));
			speedTF.addKeyListener(this);
			speedP.add(speedL);
			speedP.add(speedTF);
			panel4.add(speedP);
			panel1.add(panel4);
			
			if (surveyType == null || !surveyType.contains("import")) {
				JPanel panel5 = new JPanel(new GridLayout(0,1));
				generateL = new JLabel("<html><strong>Generate additional parallel lines</strong></html>");
				panel5.add(generateL);
				JPanel linesP = new JPanel();
				linesP.setLayout(new BoxLayout(linesP, BoxLayout.LINE_AXIS));
				JLabel numLinesL = new JLabel("Total number of lines");
				numLinesTF = new JTextField();
				numLinesTF.setPreferredSize(new Dimension(50,30));
				numLinesTF.setMaximumSize(new Dimension(50,30));
				numLinesTF.setText("1");
				numLinesTF.addKeyListener(this);
				linesP.add(numLinesL);
				linesP.add(numLinesTF);
				panel5.add(linesP);
				
				JPanel gapP = new JPanel();
				gapP.setLayout(new BoxLayout(gapP, BoxLayout.LINE_AXIS));
				gapL = new JLabel("Line spacing (km)      ");
				gapL.setEnabled(enableSpacing());
				gapTF = new JTextField(3);
				gapTF.setPreferredSize(new Dimension(50,23));
				gapTF.setMaximumSize(new Dimension(50,23));
				gapTF.setEnabled(enableSpacing());
				gapTF.addKeyListener(this);
				gapP.add(gapL);
				gapP.add(gapTF);
				panel5.add(gapP);
//				
//				autoGapCB = new JCheckBox("<html>Calculate spacing from elevation<br>and multibeam sonar angle</html>");
//				alignComponent(autoGapCB);
//				autoGapCB.setPreferredSize(new Dimension(250,60));
//				autoGapCB.setMaximumSize(new Dimension(250,60));
//				autoGapCB.setSelected(autogap);
//				autoGapCB.setActionCommand("autogap");
//				autoGapCB.addActionListener(this);
//				autoGapCB.setEnabled(false);
//				panel1.add(autoGapCB);
//		
//				JPanel swathP = new JPanel();
//				swathP.setLayout(new BoxLayout(swathP, BoxLayout.LINE_AXIS));
//				alignComponent(swathP);
//				swathL = new JLabel("Swath Angle");
//				swathL.setEnabled(autogap);
//				swathTF = new JTextField("120");
//				swathTF.setEnabled(autogap);
//				swathTF.addKeyListener(this);
//				overlapL = new JLabel("Overlap %");
//				overlapL.setEnabled(autogap);
//				overlapTF = new JTextField("10");
//				overlapTF.setEnabled(autogap);
//				overlapTF.addKeyListener(this);
//				swathP.add(swathL);
//				swathP.add(swathTF);
//				swathP.add(overlapL);
//				swathP.add(overlapTF);
//				panel1.add(swathP);
		

				dirL = new JLabel("<html><strong>Step additional lines to:</strong></html>");
				dirL.setEnabled(false);
				panel5.add(dirL);
				panel1.add(panel5);
				JPanel dirGP = new JPanel(new GridLayout(1,4));
				dirCircle1 = new DirectionCirclePanel();
				dirCircle1.setArc(0, 0, 40, 90);
				dirCircle1.addMouseListener(this);
				dirCircle1.setSelected(true);
				dirCircle2 = new DirectionCirclePanel();
				dirCircle2.setArc(0, 0, 40, -90);
				dirCircle2.addMouseListener(this);
				dirGP.setMinimumSize(new Dimension(0,50));
				dirGP.add(Box.createRigidArea(new Dimension(0,50)));
				dirGP.add(dirCircle1);
				dirGP.add(dirCircle2);
				dirGP.add(Box.createRigidArea(new Dimension(0,50)));
				dirCircle1.setEnabled(false);
				dirCircle2.setEnabled(false);
				panel1.add(dirGP);
			}

			JPanel panel6 = new JPanel(new GridLayout(0,1));
			panel6.setMaximumSize(new Dimension(2000, 30));
			JPanel btnsP = new JPanel();
			btnsP.setLayout(new BoxLayout(btnsP, BoxLayout.LINE_AXIS));
			submitB = new JButton("Submit");
			submitB.addActionListener(this);
			submitB.setEnabled(false);
			btnsP.add(submitB);
			clearB = new JButton("Reset");
			clearB.addActionListener(this);
			clearB.setActionCommand("reset");
			btnsP.add(clearB);
			panel6.add(btnsP);
			panel1.add(panel6);
			dialog.add(panel1, "Center");
			
		} else {
			if (dig == null) return;
			JPanel panel7 = new JPanel(new GridLayout(0,1));
			dig.startStopBtn.addActionListener(this);
			panel7.add(dig.startStopBtn);

			dig.deleteBtn.addActionListener(this);
			panel7.add(dig.deleteBtn);
			
			dig.insertBtn.addActionListener(this);
			panel7.add(dig.insertBtn);

			dig.addBtn.addActionListener(this);
			panel7.add(dig.addBtn);

			JPanel lineTypeP = new JPanel();
			lineTypeP.setLayout(new BoxLayout(lineTypeP, BoxLayout.LINE_AXIS));
			dig.greatCircleRB.addActionListener(this);
			dig.straightLineRB.addActionListener(this);
			lineTypeP.add(dig.greatCircleRB);
			lineTypeP.add(dig.straightLineRB);
			panel7.add(lineTypeP);

			panel7.add(dig.speedP);
			
			elevationB = new JButton("Extract elevation");
			elevationB.addActionListener(this);
			elevationB.setEnabled(!gridToggle.isSelected());
			panel7.add(elevationB);
			
			dig.objectClasses = new Class[2];
			dig.objectClasses[0] = null;

			try {
				dig.objectClasses[1] = Class.forName( "haxby.db.dig.LineSegmentsObject" );
			} catch( Exception ex) {
				ex.printStackTrace();
				dig.objectClasses[1] = null;
			}
			panel1.add(panel7);
			dialog.add( panel1, "North" );
			dialog.add( new JScrollPane( dig.list ), "Center");
		}

		dialog.validate();
		dialog.repaint();
	}
	
	public JComponent getDialog() {
		return dialog;
	}
	
	/*
	 *check whether enough fields have been filled
	 *in the right format so that we can start calculating survey lines 
	 */
	private boolean readyToSubmit() {
		if (sp.importLines) return false;
		if (sp.waypoints) return false;
		if (importLinesRB.isSelected() && sp.getSurveyLines().size() > 0) return true;
		return (GeneralUtils.isDouble(startLatTF.getText()) &&
				GeneralUtils.isDouble(startLonTF.getText()) &&
				GeneralUtils.isDouble(endLatTF.getText()) &&
				GeneralUtils.isDouble(endLonTF.getText()) &&
				(GeneralUtils.isInteger(numLinesTF.getText()) && Integer.parseInt(numLinesTF.getText()) > 0) &&
				( !enableSpacing() ||	
				 (gapTF.isEnabled() && GeneralUtils.isDouble(gapTF.getText())) 
//				|| (autoGapCB.isSelected() && GeneralUtils.isDouble(swathTF.getText()) &&
//						 GeneralUtils.isDouble(overlapTF.getText()))
				));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		sp.moveLayerToTop();
		String cmd = e.getActionCommand();
		if (cmd.equals("Submit")) {
			//remove all lines and clear table
			sp.clearLines();
			if (importLinesRB.isSelected()) {
				//if importing survey lines, can only change ship speed and line shape
				try {
					double speed = Double.parseDouble(speedTF.getText());
					SurveyLine.setSpeed(speed);
					((SurveyPlannerTableModel)sp.getTableModel()).recalculateRows();
					((AbstractTableModel) sp.getTableModel()).fireTableDataChanged();
					map.repaint();
				} catch(Exception ex) {}
				return;
			}
			
			//remove all lines and clear table
			sp.clearLines();
			map.repaint();
			
			double startLat = Double.parseDouble(startLatTF.getText());
			double startLon = Double.parseDouble(startLonTF.getText());
			double endLat = Double.parseDouble(endLatTF.getText());
			double endLon = Double.parseDouble(endLonTF.getText());
			
			//if a ship speed has been entered, use this to calculate cruise duration
			try {
				double speed = Double.parseDouble(speedTF.getText());
				SurveyLine.setSpeed(speed);
			} catch(Exception ex) {}
			
			SurveyLine.setIsStraightLine(straightLineRB.isSelected());
			
			SurveyLine newLine = new SurveyLine(map, startLat, startLon, endLat, endLon);	
			sp.addLine(newLine);

			Graphics2D g = map.getGraphics2D();
			newLine.draw(g);
			
			int numLines = Integer.parseInt(numLinesTF.getText());
			
			double gap;
			if (autogap) {
				//calculate gap between lines based on water depth
				float swath = Float.parseFloat(swathTF.getText());
				float overlap = Float.parseFloat(overlapTF.getText());
				gap = newLine.getGapFromElevations(swath, overlap);
				gapTF.setText(Double.toString(gap));
			} else {
				if (gapTF.isEnabled() && gapTF.getText().length() > 0) {
					//read in from text field
					gap = Double.parseDouble(gapTF.getText());
				} else gap = 0;
			}
			
			double sLat, sLon, eLat, eLon;
			Point2D startPt = new Point2D.Double(startLon, startLat); 
			Point2D endPt = new Point2D.Double(endLon, endLat); 
			Point2D[] firstLine = {startPt, endPt};
			Point2D[] nextLine;
			
			//determine in which direction extra lines should be added
			byte dir = 1;		
			if ((!flippedCircles && dirCircle1.isSelected()) || (flippedCircles && dirCircle2.isSelected())) {
				dir = -1;
			}
			
			for (int i=1; i<numLines; i++) {
				//find the next parallel line
				nextLine = GeneralUtils.parallelLine(firstLine, gap, dir);
				//round to 5 dp
				sLat = (double)Math.round((nextLine[0].getY()) * 100000d) / 100000d;
				sLon = (double)Math.round((nextLine[0].getX()) * 100000d) / 100000d;
				eLat = (double)Math.round((nextLine[1].getY()) * 100000d) / 100000d;
				eLon = (double)Math.round((nextLine[1].getX()) * 100000d) / 100000d;
				
				firstLine = nextLine.clone();
				
				//cruise will go back and forth, so need to switch directions
				if (i % 2 == 0) {					
					newLine = new SurveyLine(map, sLat, sLon , eLat, eLon);
				} else {
					newLine = new SurveyLine(map, eLat, eLon, sLat, sLon);
				}
				
				sp.addLine(newLine);
				newLine.draw( map.getGraphics2D() );
			}
		}
		else if (cmd.equals("mouse")) {
			if (mouseRB.isSelected()) {
				//set the pointer icon on the toolbar
				map.getMapTools().tb[0].setSelected(true);
				//change cursor to crosshair
				map.setCursor(Cursors.getCursor(Cursors.CROSS_HAIR));
			}
		}
		else if (cmd.equals("reset")) {
			//remove all lines and clear table
			sp.clearLines();
			//repaint the screen
			map.repaint();
			//reset the start and end points
			manualRB.setSelected(true);
			startLatTF.setText("");
			startLonTF.setText("");
			endLatTF.setText("");
			endLonTF.setText("");
			speedTF.setText("");
			numLinesTF.setText("1");
			gapTF.setText("");
			//autoGapCB.setSelected(false);
			if (map.getMapTools().getGridDialog().isLoaded()) {
				map.getMapTools().getGridDialog().getToggle().doClick();
			};
//			swathTF.setText("120");
//			overlapTF.setText("10");
//			swathL.setEnabled(false);
//			swathTF.setEnabled(false);
//			overlapL.setEnabled(false);
//			overlapTF.setEnabled(false);
			dirL.setEnabled(false);
			dirCircle1.setEnabled(false);
			dirCircle2.setEnabled(false);
		}
		else if (cmd.equals("autogap")) {
			//load/unload the grid
			map.getMapTools().getGridDialog().getToggle().doClick();
			//move survey planner layer to top
			map.moveOverlayToFront(sp);	
			
			//set which JSwing components are enabled
			autogap = autoGapCB.isSelected();
			gapL.setEnabled(enableSpacing());
			gapTF.setEnabled(enableSpacing());
			swathL.setEnabled(autogap);
			swathTF.setEnabled(autogap);
			overlapL.setEnabled(autogap);
			overlapTF.setEnabled(autogap);
		}
		else if (cmd.equals("Help")) {
			sp.displayHelp();
		}
		else if (cmd.equals("waypoints")) {
			sp.waypoints = true;
			sp.importLines = false;
			surveyType = cmd;
			initDialog();
			sp.initDataDisplay();		
		}
		else if (cmd.equals("importWaypoints")) {
			sp.waypoints = true;
			sp.importLines = false;
			surveyType = cmd;
			loadWaypointsFromFile();
			SurveyLine.setIsStraightLine(straightLineRB.isSelected());
			initDialog();
			sp.initDataDisplay();
			dig.list.setSelectedIndex(dig.list.getModel().getSize() -1);
		}
		else if (cmd.equals("lines")) {
			sp.waypoints = false;
			sp.importLines = false;
			surveyType = cmd;
			initDialog();
			sp.initDataDisplay();
		}
		else if (cmd.equals("import")) {
			sp.waypoints = false;
			sp.importLines = true;
			surveyType = cmd;
			initDialog();
			sp.initDataDisplay();
			loadFromFile();
		}
		else if (cmd.equals("Extract elevation")) {
			//load up the GMRT grid
			sp.setLoadedGMRT(true);
			gridToggle.doClick();
		}
		else if (e.getSource() == straightLineRB || e.getSource() == greatCircleRB) {
			SurveyLine.setIsStraightLine(straightLineRB.isSelected());
			if (submitB.isEnabled() || importLinesRB.isSelected()) {
				//update cumulative distances and durations
				((SurveyPlannerTableModel)sp.getTableModel()).recalculateRows();			
				sp.repaint();
			}	
		}
		//check whether we are ready to enable the Submit button
		if (submitB != null) submitB.setEnabled(readyToSubmit());
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == dirCircle1) {
			dirCircle1.setSelected(true);
			dirCircle2.setSelected(false);
		} else if (e.getSource() == dirCircle2) {
			dirCircle2.setSelected(true);
			dirCircle1.setSelected(false);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (mouseRB != null && mouseRB.isSelected() && map.getMapTools().tb[0].isSelected()) {
			map.setCursor(Cursors.getCursor(Cursors.CROSS_HAIR));
		}
	}
	
	void begin(MouseEvent e) {
		//clear the map
		map.repaint();
		
		//redraw lines
		for (SurveyLine line : sp.getSurveyLines()) {
			line.draw(map.getGraphics2D());;
		}
		//get start point
		p0 = e.getPoint();
	}

	public void mouseDragged(MouseEvent e) {

		if (mouseRB != null && mouseRB.isSelected() && map.getMapTools().tb[0].isSelected() && !sp.editLine) {
			 // find the current mouse location and draw a line as the mouse is dragged
			if (p0==null) { begin(e); return; }
			drawLine();
			Point2D p = map.getScaledPoint(e.getPoint());
			currentLine = new Line2D.Double(map.getScaledPoint(p0), p);
			Point2D[] pts = getPath(currentLine, map);
			currentPath =  getGeneralPath(pts, map);
			drawLine();
			
			//display coords and line distance at the top of the screen
			java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
			fmt.setMaximumFractionDigits(2);
			map.setXY(e.getPoint());
			map.getMapTools().setInfoText( map.getMapTools().getInfoText() + ", distance = " +
					fmt.format(GeneralUtils.distance(pts)) +" km");
		}
	}
	
	/*
	 * create a GeneralPath that can be plotted
	 */
	static GeneralPath getGeneralPath(Point2D[] pts, XMap map) {
		Projection proj = map.getProjection();
		GeneralPath path = new GeneralPath();
		float[] lastP = null;
		float wrap = (float)map.getWrap()/2f;

		for( int k=0 ; k<pts.length ; k++) {
			Point2D p = proj.getMapXY( pts[k] );
			float x = (float)p.getX();
			float y = (float)p.getY();
			if( lastP!=null && wrap>0f ) {
				while( x-lastP[0] <-wrap ){x+=wrap*2f;}
				while( x-lastP[0] > wrap ){x-=wrap*2f;}
			}
			lastP = new float[] {x, y};
			if( k==0 ) path.moveTo( x, y );
			else path.lineTo( x, y );
		}
		return path;
	}

	/*
	 * calculate points along the path
	 */
	static Point2D[] getPath(Line2D l, XMap map) {
		Point2D.Double p1 = (Point2D.Double) l.getP1();
		Point2D.Double p2 = (Point2D.Double) l.getP2();
		
		double dist = map.getZoom()*Math.sqrt(
			Math.pow( p1.getX()-p2.getX(),2 ) +
			Math.pow( p1.getY()-p2.getY(),2 ));
		int npt = (int)Math.ceil(dist);
		if (npt < 30) npt = 30;
		
		Projection proj = map.getProjection();
		Point2D q1 = proj.getRefXY(p1);
		Point2D q2 = proj.getRefXY(p2);
		XYZ r1 = XYZ.LonLat_to_XYZ(q1);
		XYZ r2 = XYZ.LonLat_to_XYZ(q2);
		double angle;
		//check if we are going the long way round and adjust the increment angle accordingly
		if (Math.abs((p2.getX() - p1.getX())) > map.getWrap()/2.) {
			//long way
			angle = -(Math.PI * 2 - Math.acos( r1.dot(r2) ))/(npt-1.);
		} else {
			angle = Math.acos( r1.dot(r2))/(npt-1.);
		}
		r2 = r1.cross(r2).cross(r1).normalize();
		Point2D[] path = new Point2D[npt];
		for( int k=0 ; k<npt ; k++) {
			double s = Math.sin(k*angle);
			double c = Math.cos(k*angle);
			XYZ r = r1.times(c).plus( r2.times(s) );
			path[k] = r.getLonLat();
		}
		return path;
	}
	
	/*
	 * draw the line on the screen
	 */
	void drawLine() {
		if( currentPath==null || sp.waypoints )return;
		double wrap = map.getWrap();
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke(2f/(float)map.getZoom()) );
			//g.setColor( Color.red );
			g.setXORMode(Color.white);
			g.draw( currentPath );
			if( wrap>0.) {
				g.translate(wrap,0.);
				g.draw( currentPath );
			}
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseRB == null) return;
		if (mouseRB.isSelected()) {
			finish(e);
		}
		//check whether we are ready to enable the Submit button
		submitB.setEnabled(readyToSubmit());
	}

	/*
	 * At the end of a mouse drag, redraw the line in white,
	 * convert the start and end points to lat/lon and display 
	 * in the start end end lat/lon text fields
	 */
	void finish(MouseEvent e) {
		double wrap = map.getWrap();
		if (p0 == null || currentPath == null || sp.waypoints) {
			return;
		}
		drawLine();
		Graphics2D g = map.getGraphics2D();
		g.setStroke( new BasicStroke(4f/(float)map.getZoom()) );
		g.setColor( Color.white );
		//g.setXORMode(Color.white);
		g.draw( currentPath );
		if( wrap>0.) {
			g.translate(wrap,0.);
			g.draw( currentPath );
		}
		Point p1 = e.getPoint();
		if( p0.x==p1.x && p0.y==p1.y ) {
			if( line!=null ) {
				line=null;
				if(dialog!=null)dialog.setVisible(false);
				map.repaint();
			}
			p0=null;
			return;
		}

		Point2D pA = map.getScaledPoint( p0 );
		Point2D pB = map.getScaledPoint( e.getPoint());
		Point2D latLonA = map.getProjection().getRefXY(pA);
		Point2D latLonB = map.getProjection().getRefXY(pB);
		
		startLonTF.setText(df.format(latLonA.getX()));
		startLatTF.setText(df.format(latLonA.getY()));
		endLonTF.setText(df.format(latLonB.getX()));
		endLatTF.setText(df.format(latLonB.getY()));
	
		// update the direction circles
		updateDirectionCircles();
		
		currentLine = null;
		currentPath = null;
		p0=null;
	}
	
	
	private void loadFromFile() {
		double startLat, startLon, endLat, endLon, duration;
		int lineNum;
		int startLonCol = 0;
		int startLatCol = 1;
		int endLonCol = 2; 
		int endLatCol = 3;
		int startElevationCol = Integer.MAX_VALUE;
		int endElevationCol = Integer.MAX_VALUE;
		int lineNumCol = Integer.MAX_VALUE;
		int distanceCol = Integer.MAX_VALUE;
		int durationCol = Integer.MAX_VALUE;
		SurveyLine newLine;
		boolean dataFound = false;
		SurveyPlannerTableModel tm = null;
		
		//open import table dialog
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();
		DBInputDialog dialog = new DBInputDialog((Frame) c,DBInputDialog.IMPORT_UNKNOWN_TEXT_FILE,null);
		dialog.setVisible(true);
		if (dialog.input == null) return;
		
		//create a dataset object using the UnknownDataSet format
		UnknownDataSet d = new UnknownDataSet(dialog.desc, dialog.input.getText(), dialog.getDelimeter(), map);
		dialog.dispose();
	
		try {
			for (int i = 0; i < d.header.size(); i++) {
				//try and assign columns based on header
				String col = d.header.get(i).toLowerCase();
				if (col.contains("start") && col.contains("lat")) startLatCol = i;
				if (col.contains("start") && col.contains("lon")) startLonCol = i;
				if (col.contains("end") && col.contains("lat")) endLatCol = i;
				if (col.contains("end") && col.contains("lon")) endLonCol = i;
				if (col.contains(SurveyPlannerTableModel.LINE_NUM_COL.toLowerCase())) lineNumCol = i;
				if (col.contains(SurveyPlannerTableModel.START_ELEVATION_COL.toLowerCase())) startElevationCol = i;
				if (col.contains(SurveyPlannerTableModel.END_ELEVATION_COL.toLowerCase())) endElevationCol = i;
				if (col.contains(SurveyPlannerTableModel.DURATION_COL.toLowerCase())) durationCol = i;
				if (col.contains(SurveyPlannerTableModel.DISTANCE_COL.toLowerCase())) distanceCol = i;
			}
			
			//add any extra columns not included in the imported file
			if (lineNumCol == Integer.MAX_VALUE) d.header.add(SurveyPlannerTableModel.LINE_NUM_COL);
			if (startElevationCol == Integer.MAX_VALUE) d.header.add(SurveyPlannerTableModel.START_ELEVATION_COL);
			if (endElevationCol == Integer.MAX_VALUE) d.header.add(SurveyPlannerTableModel.END_ELEVATION_COL);
			if (distanceCol == Integer.MAX_VALUE) d.header.add(SurveyPlannerTableModel.DISTANCE_COL);
			if (durationCol == Integer.MAX_VALUE) d.header.add(SurveyPlannerTableModel.DURATION_COL);
			
			//make sure lon and lat columns have the same names we've defined in the table model
			d.header.set(startLonCol, SurveyPlannerTableModel.START_LON_COL);
			d.header.set(startLatCol, SurveyPlannerTableModel.START_LAT_COL);
			d.header.set(endLonCol, SurveyPlannerTableModel.END_LON_COL);
			d.header.set(endLatCol, SurveyPlannerTableModel.END_LAT_COL);
					
			//add the SurveyLinescolumn to the dataset
			d.header.add(SurveyPlannerTableModel.SURVEY_LINE_COL);
			sp.clearLines();
			//create a new table model based on the dataset
			tm = new SurveyPlannerTableModel(d, sp);
			//sp.setTableModel(tm);
			
			//read in coords and add new lines to map
			for (int i = 0; i < d.data.size(); i++ ) {
				Vector<Object> row = d.data.get(i).data;
				startLat = Double.parseDouble((String) row.get(startLatCol));
				startLon = Double.parseDouble((String) row.get(startLonCol));
				endLat = Double.parseDouble((String) row.get(endLatCol));
				endLon = Double.parseDouble((String) row.get(endLonCol));
				newLine = new SurveyLine(map, startLat, startLon, endLat, endLon);
				if (lineNumCol != Integer.MAX_VALUE) {
					lineNum = Integer.parseInt(((String) row.get(lineNumCol)).replaceAll(",", ""));
					newLine.setLineNum(lineNum);
				} else {
					row.add(tm.getColumnIndex(SurveyPlannerTableModel.LINE_NUM_COL), Integer.toString(newLine.getLineNum()));
					tm.setValueAt(Integer.toString(newLine.getLineNum()), i, tm.getColumnIndex(SurveyPlannerTableModel.LINE_NUM_COL));
				}
					
				if (durationCol != Integer.MAX_VALUE) {
					duration = Double.parseDouble(((String) row.get(durationCol)).replaceAll(",", ""));
					newLine.setDuration(duration);
				} 

				sp.addLine(newLine);
				//populate extra columns
				if (startElevationCol == Integer.MAX_VALUE) {
					row.add(tm.getColumnIndex(SurveyPlannerTableModel.START_ELEVATION_COL), newLine.getStartElevation());
					tm.setValueAt(newLine.getStartElevation(), i, tm.getColumnIndex(SurveyPlannerTableModel.START_ELEVATION_COL));
				}
				if (endElevationCol == Integer.MAX_VALUE) {
					row.add(tm.getColumnIndex(SurveyPlannerTableModel.END_ELEVATION_COL), newLine.getEndElevation());
					tm.setValueAt(newLine.getEndElevation(), i, tm.getColumnIndex(SurveyPlannerTableModel.END_ELEVATION_COL));
				}
				if (distanceCol == Integer.MAX_VALUE) {
					row.add(tm.getColumnIndex(SurveyPlannerTableModel.DISTANCE_COL), Integer.toString(newLine.getCumulativeDistance()));
					tm.setValueAt(Integer.toString(newLine.getCumulativeDistance()), i, tm.getColumnIndex(SurveyPlannerTableModel.DISTANCE_COL));
				}
				if (durationCol == Integer.MAX_VALUE) {
				row.add(tm.getColumnIndex(SurveyPlannerTableModel.DURATION_COL), Double.toString(newLine.getDuration()));
				tm.setValueAt(Double.toString(newLine.getDuration()), i, tm.getColumnIndex(SurveyPlannerTableModel.DURATION_COL));
				}
				
				row.add(newLine);
				tm.setValueAt(newLine, i, tm.getSurveyLineColumn());

				newLine.draw(map.getGraphics2D());
				dataFound = true;
			}
			//reorder columns
			tm.reorderColumn(0, SurveyPlannerTableModel.LINE_NUM_COL);
			tm.reorderColumn(1, SurveyPlannerTableModel.START_LON_COL);
			tm.reorderColumn(2, SurveyPlannerTableModel.START_LAT_COL);
			tm.reorderColumn(3, SurveyPlannerTableModel.START_ELEVATION_COL);
			tm.reorderColumn(4, SurveyPlannerTableModel.END_LON_COL);
			tm.reorderColumn(5, SurveyPlannerTableModel.END_LAT_COL);
			tm.reorderColumn(6, SurveyPlannerTableModel.END_ELEVATION_COL);
			tm.reorderColumn(7, SurveyPlannerTableModel.DISTANCE_COL);
			tm.reorderColumn(8, SurveyPlannerTableModel.DURATION_COL);
			
			//update table model
		//	sp.setTableModel(tm);
			
			//if durations included, calculate ship speed in knots
			SurveyLine firstLine = sp.getSurveyLines().get(0);
			if (firstLine.getDuration() != 0) {
				double speed = (firstLine.getCumulativeDistance() / firstLine.getDuration()) / GeneralUtils.KNOTS_2_KPH;
				SurveyLine.setSpeed(speed);
			}
						
			//update text fields
			startLatTF.setText(Double.toString(firstLine.getStartLat()));
			startLonTF.setText(Double.toString(firstLine.getStartLon()));
			endLatTF.setText(Double.toString(firstLine.getEndLat()));
			endLonTF.setText(Double.toString(firstLine.getEndLon()));
			numLinesTF.setText(Integer.toString(sp.getSurveyLines().size()));
			speedTF.setText(dp1f.format(SurveyLine.getSpeed()));
			
			//calculate line spacing
			if (sp.getSurveyLines().size() > 1) {
				Point2D[] pts = {firstLine.getStartPoint(), sp.getSurveyLines().get(1).getEndPoint()};
				double gap = GeneralUtils.distance(pts);
				gapTF.setText(dp3f.format(gap));
			}
			
			//check whether the Line Spacing components can be enabled 
//			gapL.setEnabled(enableSpacing());
//			gapTF.setEnabled(enableSpacing());
//			submitB.setEnabled(readyToSubmit());

		} catch (Exception e) {
			System.out.println(e);
		}
		
		if (!dataFound) {
			JOptionPane.showMessageDialog(dialog, "Unable to read survey lines from file " + dialog.getPath() + 
					".\nPlease make sure file contains at least the following columns:" +
					"\nStart Longitude"+
					"\nStart Latitude"+
					"\nEnd Longitude"+
					"\nEdn Latitude", 
					"Load Error", JOptionPane.ERROR_MESSAGE);
			return;
		} 
		//update table model
		map.repaint();
		sp.setTableModel(tm);
	}

	private void loadWaypointsFromFile() {
		int lonCol = 0;
		int latCol = 1;
		int zCol = Integer.MAX_VALUE;
		int cumulativeCol = Integer.MAX_VALUE;
		int distanceCol = Integer.MAX_VALUE;
		int durationCol = Integer.MAX_VALUE;
		boolean dataFound = false;
		
		//open import table dialog
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();
		DBInputDialog dialog = new DBInputDialog((Frame) c,DBInputDialog.IMPORT_UNKNOWN_TEXT_FILE,null);
		dialog.setVisible(true);
		if (dialog.input == null) return;
		
		//create a dataset object using the UnknownDataSet format
		UnknownDataSet d = new UnknownDataSet(dialog.desc, dialog.input.getText(), dialog.getDelimeter(), map);
		dialog.dispose();
		
		try {
			for (int i = 0; i < d.header.size(); i++) {
				//try and assign columns based on header
				String col = d.header.get(i).toLowerCase();
				if (col.contains("lat") && !col.contains("cumulative")) latCol = i;
				if (col.contains("lon")) lonCol = i;
				if (col.contains(LineSegmentsObject.Z_COL.toLowerCase())) zCol = i;
				if (col.contains(LineSegmentsObject.CUMULATIVE_COL.toLowerCase())) cumulativeCol = i;
				if (col.contains(LineSegmentsObject.DURATION_COL.toLowerCase())) durationCol = i;
				if (col.contains(LineSegmentsObject.DISTANCE_COL.toLowerCase())) distanceCol = i;
			}
			
			//add any extra columns not included in the imported file
			if (zCol == Integer.MAX_VALUE) d.header.add(LineSegmentsObject.Z_COL);
			if (distanceCol == Integer.MAX_VALUE) d.header.add(LineSegmentsObject.DISTANCE_COL);
			if (cumulativeCol == Integer.MAX_VALUE) d.header.add(LineSegmentsObject.CUMULATIVE_COL);
			if (durationCol == Integer.MAX_VALUE) d.header.add(LineSegmentsObject.DURATION_COL);
			
			//make sure lon and lat columns have the same names we've defined in the table model
			d.header.set(lonCol, LineSegmentsObject.LON_COL);
			d.header.set(latCol, LineSegmentsObject.LAT_COL);
					
			//create a new table model based on the dataset
			LineSegmentsObject obj = new LineSegmentsObject(d, map, dig);
			
			//read in coords and add new line to table and map
			double lon, lat, z;
			int rowNum = 0;
			double prevX = Double.NaN;
			float wrap = (float)map.getWrap();	
			for (int i = 0; i < d.data.size(); i++ ) {
				Vector<Object> row = d.data.get(rowNum).data;
				if (row.get(0).equals("Digitized points")) {
					d.data.remove(i);
					continue;
				}
				if (row.get(0).equals("Interpolated points")) break;
				lon = Double.parseDouble((String) row.get(lonCol));
				lat = Double.parseDouble((String) row.get(latCol));
				if (zCol != Integer.MAX_VALUE) {
					String zString = ((String) row.get(zCol)).replaceAll(",","");
					if (zString.equals("-") || zString.equals("")) zString = "NaN";
					z = Double.parseDouble(zString);
				}
				else {
					z = Double.NaN;
					row.add(obj.getColumnIndex(LineSegmentsObject.Z_COL), "-");
				}
				dataFound = true;
				Point2D.Double p = (Point2D.Double)map.getProjection().getMapXY(lon,  lat);

				// if crossing the meridian, probably want to add 640 to p.x
				if (wrap > 0 && i >= 1 && Math.abs(p.x - prevX) > Math.abs(p.x + wrap - prevX)) {
					p.x += wrap;
				}
				prevX = p.x;
	
				double[] xyz = {p.x, p.y, z};
				obj.points.add(xyz);
				
				if (distanceCol == Integer.MAX_VALUE) {
					row.add(obj.getColumnIndex(LineSegmentsObject.DISTANCE_COL), (obj.getValueAt(rowNum, obj.getColumnIndex(LineSegmentsObject.DISTANCE_COL), 0, false)));
				}
				if (cumulativeCol == Integer.MAX_VALUE) {
					row.add(obj.getColumnIndex(LineSegmentsObject.CUMULATIVE_COL), (obj.getValueAt(rowNum, obj.getColumnIndex(LineSegmentsObject.CUMULATIVE_COL), 0, false)));
				}
				if (durationCol == Integer.MAX_VALUE) {
					row.add(obj.getColumnIndex(LineSegmentsObject.DURATION_COL), (obj.getValueAt(rowNum, obj.getColumnIndex(LineSegmentsObject.DURATION_COL), 0, false)));
				}
				rowNum ++;

			}
			//remove any interpolated points rows from displayToDataIndex
			obj.displayToDataIndex.subList(rowNum, obj.displayToDataIndex.size()).clear();
			//reorder columns
			obj.reorderColumn(0, LineSegmentsObject.LON_COL);
			obj.reorderColumn(1, LineSegmentsObject.LAT_COL);
			obj.reorderColumn(2, LineSegmentsObject.Z_COL);
			obj.reorderColumn(3, LineSegmentsObject.DISTANCE_COL);
			obj.reorderColumn(4, LineSegmentsObject.CUMULATIVE_COL);
			obj.reorderColumn(5, LineSegmentsObject.DURATION_COL);
			
			dig.objects.add( obj );
			dig.table.setModel(obj);
			dig.makeProfile();

			//add to list
			dig.model.objectAdded();
			obj.setName(dialog.getFilename().split("\\.")[0]);
			
			//if durations included, calculate ship speed in knots
			Vector<Object> row1 = d.data.get(1).data;
			double duration = 0;
			if (durationCol != Integer.MAX_VALUE) duration = Double.parseDouble(((String)row1.get(durationCol)).replaceAll(",", ""));
			int cumulativeDistance = Integer.parseInt(((String) obj.getValueAt(1, obj.getColumnIndex(LineSegmentsObject.CUMULATIVE_COL))).replaceAll(",", ""));
			if (duration != 0) {
				double speed = (cumulativeDistance / duration) / GeneralUtils.KNOTS_2_KPH;
				dig.speedTF.setText(dp1f.format(speed));
			}
			
			//draw
			dig.list.setSelectedValue(obj, true);
			obj.setSelected(true);
			obj.redraw();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		
		if (!dataFound) {
			JOptionPane.showMessageDialog(dialog, "Unable to read waypoints from file " + dialog.getPath() + 
					".\nPlease make sure file contains at least a longitude column and a latitude column.", 
					"Load Error", JOptionPane.ERROR_MESSAGE);
			return;
		} 				
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getSource() == speedTF) {
			try {
				double speed = Double.parseDouble(speedTF.getText());
				SurveyLine.setSpeed(speed);
				((SurveyPlannerTableModel)sp.getTableModel()).recalculateRows();
			} catch (Exception ex) {}
		}
		
		//check whether the Line Spacing components can be enabled 
		gapL.setEnabled(enableSpacing());
		gapTF.setEnabled(enableSpacing());
		//check whether we are ready to enable the Submit button
		submitB.setEnabled(readyToSubmit());
		//update the direction circles
		try{
			if (Integer.parseInt(numLinesTF.getText()) > 1) {
				dirL.setEnabled(true);
				dirCircle1.setEnabled(true);
				dirCircle2.setEnabled(true);
			
				Object src = e.getSource();
				if (src == endLatTF || src == endLonTF || src == startLatTF || src == startLonTF)
					updateDirectionCircles();
			}
			else {
				dirL.setEnabled(false);
				dirCircle1.setEnabled(false);
				dirCircle2.setEnabled(false);
			}
		} catch(Exception ex) {}
	}
	
	/*
	 * enable line spacing is autogap is off and number of lines > 1
	 */
	private boolean enableSpacing() {
		return (GeneralUtils.isInteger(numLinesTF.getText()) 
				&& (Integer.parseInt(numLinesTF.getText()) > 1) 
				&& !autogap);
	}

	
	/*
	 * Update the direction circles based on the current start and end points
	 */
	private void updateDirectionCircles() {
		if (GeneralUtils.isDouble(startLatTF.getText()) &&
				GeneralUtils.isDouble(startLonTF.getText()) &&
				GeneralUtils.isDouble(endLatTF.getText()) &&
				GeneralUtils.isDouble(endLonTF.getText()) ) {
			
			double startLat = Double.parseDouble(startLatTF.getText());
			double startLon = Double.parseDouble(startLonTF.getText());
			double endLat = Double.parseDouble(endLatTF.getText());
			double endLon = Double.parseDouble(endLonTF.getText());

			float wrap = (float)map.getWrap();			
			//get the limits of the displayed map
			Rectangle2D rect = map.getClipRect2D();
			double xmin = rect.getMinX();
			double xmax = rect.getMaxX();
			
			Projection proj = map.getProjection();
			Point2D.Double pt = new Point2D.Double();
			pt.x = startLon;
			pt.y = startLat;
			Point2D.Double p_start = (Point2D.Double) proj.getMapXY(pt);

			if( wrap>0f ) {
				while( p_start.x <= xmin ){p_start.x+=wrap;}
				while( p_start.x >= xmax ){p_start.x-=wrap;}
			}
			
			pt.x = endLon;
			pt.y = endLat;
			Point2D.Double p_end = (Point2D.Double) proj.getMapXY(pt);

			if( wrap>0f ) {
				while( p_end.x <= xmin ){p_end.x+=wrap;}
				while( p_end.x > wrap + xmin ){p_end.x-=wrap;}
			}
			
			//draw the shortest line - either p_start.x to p_end.x or the x+wrap values. 
			if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
				((p_start.x - (p_end.x + wrap)) * (p_start.x - (p_end.x + wrap))) )  {p_end.x += wrap;}
			if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
			(((p_start.x + wrap) - p_end.x) * ((p_start.x + wrap) - p_end.x)) )  {p_start.x += wrap;}
			
			Point2D[] firstLine = {p_start, p_end};
			double bearing = GeneralUtils.flatEarthBearing(firstLine);	
			flippedCircles = false;
			if (bearing > 90 || bearing <= -90) {
				bearing -= 180;
				flippedCircles = true;
			}
			dirCircle1.setAngle((int)bearing);
			dirCircle2.setAngle(180 + (int)bearing);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// if GMRT grid is closed, enable the Extract elevation button
		if (e.getSource() == gridToggle) {
			if (!sp.isEnabled()) return;
			if (sp != null && sp.waypoints && elevationB != null) elevationB.setEnabled(!gridToggle.isSelected());
			sp.moveLayerToTop();
			updateSurveyPlanner();
		}
		
	}
	
	public void updateSurveyPlanner() {
		if (sp.waypoints)
			dig.makeProfile();
		else
			((SurveyPlannerTableModel)sp.getTableModel()).recalculateElevations();
	}
}
