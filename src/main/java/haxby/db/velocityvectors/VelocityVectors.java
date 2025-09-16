package haxby.db.velocityvectors;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import haxby.db.Database;
import haxby.db.custom.DBDescription;
import haxby.db.custom.OtherDBInputDialog;
import haxby.db.custom.UnknownData;
import haxby.db.custom.UnknownDataSet;
import haxby.db.custom.UnknownDataSet.UnknownDataSceneEntry;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.GeneralUtils.WideComboBox;
import haxby.util.PathUtil;
import haxby.util.SceneGraph.SceneGraphEntry;
import haxby.util.URLFactory;


public class VelocityVectors extends JFrame implements Database, ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;
	private XMap map;
	private boolean loaded;
	private boolean isEnabled;
	private JPanel dataDisplay = new JPanel(new BorderLayout());
	private JPanel panelA = null, infoPanel;
	private WideComboBox filesCB, decimateCB;
	private Vector<String> velocityFiles;
	private JTextField scaleTF, confLevelTF;
	private JButton helpBtn, submitBtn, importBtn;
	private JCheckBox showEllipsesChBx;
	private static Dimension compDim = new Dimension(250, 33); 
	private static Dimension ellipseDim = new Dimension(250, 60);
	private DrawArrow arrow;
	private double scale = 1;
	private double confLevel = 0.95;
	private String snapFile;
	private UnknownDataSet ds;
	private int velNIndex, velEIndex, sigNIndex, sigEIndex, rhoIndex, decimateIndex;
	private double dpcm;
	private JLabel pointsLabel, infoLabel, confLevelLabel;
	private int convertToMm = 1;
	private Map<String, String> snapFileDescriptions = new HashMap<String, String>();
	private Map<String, String> snapFileEndDates = new HashMap<String, String>();
	private String importedPath = null;
	private boolean showEllipses = false;
	private static String VELOCITY_VECTOR_HELP_URL = MapApp.BASE_URL+"/gma_html/help/velocity_vector_help.html";
    public static final double INCHES_PER_CM = 0.393701;
    private static final int[] DECIMATE_VALS = {1, 2, 3, 5, 10, 20};
    JDialog helpDialog; 
    
    public class DrawArrow extends JComponent {

        private static final long serialVersionUID = 1L;
        double arrowLength;
        private boolean enabled = true;
        DrawArrow(double scale) {
            arrowLength = dpcm * scale;
        }

        public void setScale(double scale) {
        	arrowLength = dpcm * scale;
        }
        
        @Override
        public void setEnabled(boolean tf) {
        	super.setEnabled(tf);
        	enabled = tf;
        	repaint();
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
	        //draw the arrow
	        double arrSize = 4;
	        double angle = 0;
	        double rad = 2;
	        Graphics2D g2d = (Graphics2D) g;
	        AffineTransform at = g2d.getTransform();
	        
	        if (enabled)
	        	drawArrow(g2d, at, 50, 50, angle, Color.RED, Color.BLACK, arrowLength, arrSize, 0, 0, 0, rad, 0);
	        else
	        	drawArrow(g2d, at, 50, 50, angle, Color.GRAY, Color.GRAY, arrowLength, arrSize, 0, 0, 0, rad, 0);

	        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
	        g2d.drawString("10mm/year", 35, 30);
        }
    }
	
	public VelocityVectors(XMap map) {
		this.map = map;
    	map.addMouseListener(this);
		//read in a list of velocity vector files
		velocityFiles = new Vector<String>();
		String velocityVectorsList = PathUtil.getPath("PORTALS/VELOCITY_VECTORS_PATH") + "file_list_info.txt";
		URL url;
		try {
			url = URLFactory.url(velocityVectorsList);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));
			String line = null;
			String filename, desc, endDate;
			while((line = in.readLine()) != null) {
				String[] parts = line.split("\t");
				if (parts.length == 3) {
					filename = parts[0];
					desc = parts[1];
					endDate = parts[2];
					velocityFiles.add(desc);
					snapFileDescriptions.put(desc, filename);
					snapFileEndDates.put(desc, endDate);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		velocityFiles.add(0, "--- Select velocity data ---");
		//set the size of the dataDisplay at the bottom
		dataDisplay.setPreferredSize(new Dimension(10,160));
		ds = null;
		//get the user's screen resolution and convert to dots per cm
        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        dpcm = dpi * INCHES_PER_CM;
	}

	private void createDataDisplay() {
		if (ds == null) return;
		dataDisplay.removeAll();
		dataDisplay.add(ds.tableSP, "Center");
		ds.dataT.addMouseListener(this);
		dataDisplay.validate();
		dataDisplay.repaint();
	}

	private void createInfoDisplay() {
		panelA = new JPanel();
		panelA.setPreferredSize(new Dimension(260,100));
		panelA.setMaximumSize(new Dimension(260,100));
		panelA.setLayout(new BoxLayout(panelA, BoxLayout.PAGE_AXIS));

		JPanel helpPanel = new JPanel( new GridLayout(0,1) );
		helpBtn = new JButton("Help");
		helpBtn.addActionListener(this);
		helpPanel.setMinimumSize(new Dimension(240, 25));
		helpPanel.setPreferredSize(new Dimension(240, 25));
		helpPanel.setMaximumSize(new Dimension(2000, 25));
		helpPanel.add(helpBtn);
		panelA.add(helpPanel);

		
		infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));
		String infoText = "                                                                   ";
		infoLabel = new JLabel(infoText);
		infoLabel.setFont( new Font("SansSerif", Font.PLAIN, 11));
		infoLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Data Set Information"),
							BorderFactory.createEmptyBorder(2,4,2,4)));
		infoPanel.add(infoLabel);
		panelA.add(infoPanel);

		if ( !velocityFiles.isEmpty() ) {
			filesCB = new WideComboBox(velocityFiles);
			filesCB.addActionListener(this);
			filesCB.setPreferredSize(compDim);
			filesCB.setMaximumSize(compDim);
			panelA.add( filesCB);
		}
		
		JPanel importPanel = new JPanel();
		importPanel.setPreferredSize(compDim);
		importPanel.setMaximumSize(compDim);
		importBtn = new JButton("Or, import your own file");
		importBtn.addActionListener(this);
		importPanel.add(importBtn);
		panelA.add(importPanel);
		
		JPanel scalePanel = new JPanel();
		JLabel scaleLabel = new JLabel("Vector length scale:");
		scalePanel.add(scaleLabel);
		JPanel scalePanel2 = new JPanel();
		scaleTF = new JTextField(2);
		scaleTF.setText(Double.toString(scale));
		scalePanel2.add(scaleTF);
		JLabel scalePostL = new JLabel("mm = 1 mm/yr");
		scalePanel2.add(scalePostL);
		scalePanel.add(scalePanel2);
		scalePanel.setPreferredSize(ellipseDim);
		scalePanel.setMaximumSize(ellipseDim);
		panelA.add(scalePanel);
		
		JPanel ellipsesPanel = new JPanel();
		ellipsesPanel.setPreferredSize(ellipseDim);
		ellipsesPanel.setMaximumSize(ellipseDim);
		showEllipsesChBx = new JCheckBox("Show error ellipses");
		showEllipsesChBx.setSelected(false);
		showEllipsesChBx.addActionListener(this);
		ellipsesPanel.add(showEllipsesChBx);
		confLevelLabel = new JLabel("Confidence level (0-1)");
		confLevelLabel.setEnabled(showEllipsesChBx.isSelected());
		confLevelTF = new JTextField(3);
		confLevelTF.setText(Double.toString(confLevel));
		confLevelTF.setEnabled(showEllipsesChBx.isSelected());
		ellipsesPanel.add(confLevelLabel);
		ellipsesPanel.add(confLevelTF);
		panelA.add(ellipsesPanel);
		
		JPanel decimatePanel = new JPanel();
		decimatePanel.setPreferredSize(new Dimension(250, 70));
		decimatePanel.setMaximumSize(new Dimension(2000, 70));
		JLabel decimateLabel = new JLabel();
		decimateLabel.setText("<html><body>Number of velocity<br>solutions displayed:</body></html>");
		String[] decimateList = {"Show all", "Show half", "Show one in three", "Show one in five", "Show one in ten", "Show one in twenty"};
		decimateCB = new WideComboBox(decimateList);
		decimateCB.setSelectedIndex(decimateIndex);
		decimateCB.addActionListener(this);
		decimatePanel.add(decimateLabel);
		decimatePanel.add(decimateCB);
		panelA.add(decimatePanel);
			
		JPanel btnPanel = new JPanel();
		submitBtn = new JButton("Submit");
		submitBtn.addActionListener(this);
		submitBtn.setEnabled(false);
		btnPanel.add(submitBtn);
		btnPanel.setPreferredSize(compDim);
		btnPanel.setMaximumSize(compDim);
		panelA.add(btnPanel);
		
		arrow = new DrawArrow(scale);
		arrow.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Legend"),
				BorderFactory.createEmptyBorder(2,4,2,4)));
		arrow.setEnabled(false);
		arrow.setPreferredSize(new Dimension(250, 70));
		arrow.setMaximumSize(new Dimension(250, 70));
		panelA.add(arrow);
		
		JPanel pointsPanel = new JPanel();
		pointsPanel.setLayout(new BoxLayout(pointsPanel, BoxLayout.LINE_AXIS));
		pointsLabel = new JLabel("<html>0 of 0</html>");
		pointsLabel.setSize(100, 60);
		pointsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		pointsLabel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "Points Displayed", TitledBorder.CENTER, TitledBorder.TOP));
		pointsPanel.add(pointsLabel);
		panelA.add(pointsPanel);
	}

	public void disposeDB() {
		setEnabled(false);
		panelA = null;
		ds = null;
		dataDisplay.removeAll();
		loaded = false;
    	//reset some initial values
		snapFile = null;
		scale = 1;
		confLevel = 0.95;
		velNIndex = velEIndex = sigNIndex = sigEIndex = rhoIndex = decimateIndex = 0;
		convertToMm = 1;
		importedPath = null;
		showEllipses = false;
	}

	public void draw(Graphics2D g) {
		if (map == null) return;
		if (ds == null) return; 

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		
		double zoom = map.getZoom();
		ds.tm.setArea(rect, zoom);
		Color arrowColor = Color.RED;
		Color pointColor = Color.BLACK;
		AffineTransform at = g.getTransform();
		g.setStroke(new BasicStroke( 1f/(float)zoom ));
		int kount = 0;
		
        List<SceneGraphEntry> entriesToDraw;
        ds.tm.resetDisplayIndices();
		entriesToDraw = ds.scene.getAllEntries(map.getClipRect2D());
		for (int i = 0; i < entriesToDraw.size(); i++) {
			UnknownDataSceneEntry entry = (UnknownDataSceneEntry) entriesToDraw.get(i);
			// apply any decimation
			if (i % DECIMATE_VALS[decimateIndex] != 0) {
				entry.getData().setVisible(false);
				continue;
			}
			int z = entry.getID();
			int k = ds.tm.rowToDisplayIndex.get(z);
			UnknownData d = ds.data.get(z);
			if (ds.dataT.isRowSelected(k)) {
				continue;
			}
			kount = drawDataArrow(g, at, d, zoom, xMin, xMax, yMin, yMax, wrap, kount, arrowColor, pointColor);
		}
		//update table-to-data relation
		ds.tm.updateDisplayToDataIndex();
		
		// plot selected arrows on top (want to make sure selected arrow are plotted on top)
		for (SceneGraphEntry entry : entriesToDraw) {
			if (!((UnknownDataSceneEntry)entry).getData().isVisible()) continue;
			int z = entry.getID();
			int k = ds.tm.rowToDisplayIndex.get(z);
			if (ds.dataT.isRowSelected(k)) {
				UnknownData d = ds.data.get(z);
				kount = drawDataArrow(g, at, d, zoom, xMin, xMax, yMin, yMax, wrap, kount, Color.YELLOW, Color.YELLOW);
			}
		}		
		updateTotalDataSize(kount, ds.data.size());
	}
	
	private int drawDataArrow(Graphics2D g, AffineTransform at, UnknownData d, double zoom,
			float xMin, float xMax, float yMin, float yMax, float wrap, int kount, Color arrowColor, Color pointColor){

		if (velNIndex == -1 || velEIndex == -1) return 0;
		float x = d.x;
		float y = d.y;
		if (y > yMax) return kount;
		if (y < yMin) return kount;

		double velN =  Double.parseDouble((String) d.data.get(velNIndex)) * convertToMm;
		double velE =  Double.parseDouble((String) d.data.get(velEIndex)) * convertToMm;
		double sigN =  (Double.parseDouble((String) d.data.get(sigNIndex)) * convertToMm * dpcm * scale / 10.) / zoom;
		double sigE =  (Double.parseDouble((String) d.data.get(sigEIndex)) * convertToMm * dpcm * scale / 10.) / zoom;
		double rho = (Double.parseDouble((String) d.data.get(rhoIndex)));
		double speed = Math.sqrt((velN * velN) + (velE * velE));	
		double angle = - Math.atan(velN/velE);
		if (velE < 0) angle += Math.PI;
        double arrowLength = (dpcm * scale * speed / 10.)/zoom;

        double arrSize = 4./zoom;
        double rad = 2./zoom;

		if( wrap>0f ) {
			while( x>xMin+wrap ) x -= wrap;
			while( x<xMin ) x += wrap;
			if( x<xMax ) {
				kount++;
			}
			while( x<xMax ) {
				drawArrow(g, at, x, y, angle, arrowColor, pointColor, arrowLength, arrSize, sigN, sigE, rho, rad, wrap);
				x += wrap;
			}
		} else {
			if( x>xMin && x<xMax ) {
				kount++;
				drawArrow(g, at, x, y, angle, arrowColor, pointColor, arrowLength, arrSize, sigN, sigE, rho, rad, wrap);
				x += wrap;
			}
		}
		return kount;
	}
	
	private void drawArrow(Graphics2D g, AffineTransform at, float x, float y, double angle, Color arrowColor, Color pointColor, 
			double arrowLength, double arrSize, double sigN, double sigE, double rho, double rad, double wrap) {
        g.translate(x, y);
        g.rotate(angle);
        g.setColor(arrowColor);
        Shape l = new Line2D.Double(0, 0, arrowLength, 0);
        g.draw(l);
        double[] xpts = {arrowLength, arrowLength-arrSize, arrowLength-arrSize, arrowLength};
        double[] ypts = {0, 0-arrSize, 0+arrSize, 0};	
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(xpts[0], ypts[0]);
        arrow.lineTo(xpts[1], ypts[1]);
        arrow.lineTo(xpts[2], ypts[2]);
        arrow.closePath();
        g.fill(arrow);
        g.setColor(pointColor);
        Ellipse2D.Double circle = new Ellipse2D.Double(-rad, -rad, 2*rad, 2*rad);
        g.fill(circle);
        if (showEllipses) {
        	drawEllipse(g, at, x, y, arrowLength, angle, sigE, sigN, rho);
        }
		g.setTransform(at);
	}
	
	/* 
	 * draw an ellipse at the end of the arrow - based on code from psvelo (see utilmeca.c ellipse_convert())
	 */
	private void drawEllipse(Graphics2D g, AffineTransform at, double x, double y, double arrowLength, double angle, 
			double sigE, double sigN, double rho){
		
		g.setTransform(at);
		
		double a, b, c, d, e, semiMinor, semiMajor, ang;
		
		/* confidence scaling */
		/*   confid      - Confidence interval wanted (0-1) */
		/* conrad = sqrt( -2.0 * log(1.0 - confid)); */
		double conRad = Math.sqrt(-2.0 * Math.log(1 - confLevel) );
		
		/* the formulas for this part may be found in Bomford, p. 719 */
		a = (sigE * sigE - sigN * sigN) * (sigE * sigE - sigN * sigN);
		b = 4. * (rho * sigE * sigN) * (rho * sigE * sigN);
		c = sigE * sigE + sigN * sigN;
		semiMinor = conRad * Math.sqrt((c - Math.sqrt(a + b))/2.0);
		semiMajor = conRad * Math.sqrt((c + Math.sqrt(a + b))/2.0);
		d = 2. * rho * sigE * sigN;
		e = sigE * sigE - sigN * sigN;
		ang = Math.atan2(d,  e)/2.0;
		
		double dE = arrowLength* Math.cos(angle);
		double dN = arrowLength * Math.sin(angle);
	    Ellipse2D.Double sigEllipse = new Ellipse2D.Double(-semiMajor, -semiMinor, semiMajor*2, semiMinor*2);
	    
	    g.translate(x+dE, y+dN);
	    g.rotate(-ang);
		g.draw(sigEllipse);
	}
	
	public void updateTotalDataSize(int numSize, int numTotal) {
		pointsLabel.setText("<html>" + Integer.toString(numSize) + " of " + Integer.toString(numTotal) + "</html>");
	}
	
	public JComponent getDataDisplay() {
		if (dataDisplay == null) createDataDisplay();
		return dataDisplay;
	}

	public String getDBName() {
		return "GPS Velocity Vectors";
	}

	public String getCommand() {
		return "velocity_vectors_cmd";
	}

	public String getDescription() {
		return "GPS Velocity Vectors";
	}

	public JComponent getSelectionDialog() {
		if (panelA == null) {
			createInfoDisplay();
		}
		return panelA;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public boolean isLoaded() {
		return loaded;
	}
	
	public void unloadDB() {
		loaded = false;
	}
	
	public boolean loadDB() {
		if( loaded ) return loaded;
		loaded = true;
		return loaded;
	}

	public void setEnabled(boolean tf) {
		if( tf && isEnabled ) return;
		map.removeMouseListener( this );
		map.addMouseListener(this);
		isEnabled = tf;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == submitBtn){
			try {
				scale = Double.parseDouble(scaleTF.getText());
				arrow.setScale(scale);
				arrow.setEnabled(true);
				if (filesCB.getSelectedIndex() != 0 && snapFile != filesCB.getSelectedItem()) {
					snapFile = (String) filesCB.getSelectedItem();
					loadFile(snapFile);
				}
				showEllipses = showEllipsesChBx.isSelected();
				confLevel = Double.parseDouble(confLevelTF.getText());
				decimateIndex = decimateCB.getSelectedIndex();
				map.repaint();
			} catch(Exception ex) {
				scaleTF.setText(Double.toString(scale));
				confLevelTF.setText(Double.toString(confLevel));
				System.out.println(ex.getMessage());
			}
		}
		else if (e.getSource().equals(helpBtn)) {
			displayHelp();
		}
		else if (e.getSource() == filesCB) {
			if (filesCB.getSelectedIndex() != 0) {
				importedPath = null;
				displayFileInfo((String)filesCB.getSelectedItem());
			}
			submitBtn.setEnabled(filesCB.getSelectedIndex() > 0);
		}
		
		else if (e.getSource() == importBtn) {
			importFile();
			if (importedPath != null) {
				snapFile = null;
				filesCB.setSelectedIndex(0);
				displayFileInfo(importedPath);
				submitBtn.setEnabled(true);
				submitBtn.doClick();
			}
		}
		
		else if (e.getSource() == showEllipsesChBx) {
			confLevelLabel.setEnabled(showEllipsesChBx.isSelected());
			confLevelTF.setEnabled(showEllipsesChBx.isSelected());
		}
		
		else if (e.getSource() == decimateCB) {
			// reset table selection
			if (ds != null) ds.dataT.clearSelection();
		}
	}
	
	private void loadFile(String desc) {
		//load up the snap file
		String filename = snapFileDescriptions.get(desc);
		String snapFilePath = PathUtil.getPath("PORTALS/VELOCITY_VECTORS_DATA") + filename;
		loadURL(getDBName(), snapFilePath);
	}
	
	
	private boolean loadURL(String inputName, String inputURL) {
		Object[] options = {"Help", "OK"};
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent(); 
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c, inputName, inputURL, 2);
		if (dialog.input==null) {
			dialog.dispose();
			return false;
		}
		try {
			ds = new UnknownDataSet(new DBDescription("Velocity Vectors",0,""), dialog.input.getText(), "\t", map);
		} catch (Exception e) {
			int selection = JOptionPane.showOptionDialog(MapApp.anchor, "Invalid velocity vector file: "+inputURL
					+ "\nFor valid file format, see the Help page linked below.", "Error", JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE, null, options, options[1]);
			if (selection == 0) displayHelp();
			return false;
		}
		dialog.dispose();
		ds.latIndex = -1;
		ds.lonIndex = -1;
		velNIndex = -1;
		velEIndex = -1;
		sigNIndex = -1;
		sigEIndex = -1;
		rhoIndex = -1;
		
		for (int i=0; i < ds.header.size(); i++) {
			//try and assign columns based on header
			String col = ds.header.get(i).toLowerCase();
			if (col.contains("lat")) ds.latIndex = i;
			if (col.contains("lon")) ds.lonIndex = i;
			if (col.contains("dn/dt") || col.contains("north velocity") || col.contains("vel_n")) velNIndex = i;
			if (col.contains("de/dt") || col.contains("east velocity") || col.contains("vel_e")) velEIndex = i;
			if (col.contains("snd") || col.contains("n. vel std dev") || col.contains("sig_n")) sigNIndex = i;
			if (col.contains("sed") || col.contains("e. vel std dev") || col.contains("sig_e")) sigEIndex = i;
			if (col.contains("rne") || col.contains("rho_en")) rhoIndex = i;
		}
		
		//check file has all required columns
		if (ds.latIndex == -1 || ds.lonIndex == -1 || velNIndex == -1 || velEIndex == -1 || sigNIndex == -1 || sigEIndex == -1 || rhoIndex == -1) {
			String missingCols = "\nThe following columns are missing: ";
			if (ds.latIndex == -1) missingCols += "\nLatitude";
			if (ds.lonIndex == -1) missingCols += "\nLongitude";
			if (velNIndex == -1) missingCols += "\nNorth velocity";
			if (velEIndex == -1) missingCols += "\nEast velocity";
			if (sigNIndex == -1) missingCols += "\nNorth velocity standard deviation";
			if (sigEIndex == -1) missingCols += "\nEast velocity standard deviation";
			if (rhoIndex == -1) missingCols += "\nNorth/East correlation coefficient";
			
			
			int selection = JOptionPane.showOptionDialog(MapApp.anchor, "Invalid velocity vector file: "+inputURL + missingCols
					+ "\nFor the required column headers, see the Help page linked below.", "Error", JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE, null, options, options[1]);
			if (selection == 0) displayHelp();
			return false;
		}
		
		if (ds.header.get(velNIndex).contains("(mm/yr)")) convertToMm = 1;
		else if (ds.header.get(velNIndex).contains("(m/yr)")) convertToMm = 1000;
		
		//If multiple rows for the same station, only take the most recent
		@SuppressWarnings("unchecked")
		Vector<UnknownData> copyOfData = (Vector<UnknownData>) ds.data.clone();
		for (int i=1; i < ds.rowData.size(); i++) {
			if (ds.data.get(i).data.get(0).equals(ds.data.get(i-1).data.get(0))) 
				copyOfData.removeElement(ds.data.get(i-1));
		}
		ds.data = copyOfData;

		//update the data used to display the table
		ds.updateDataSet();
		
		// create data display table
		createDataDisplay();
		return true;
	}
	
	private void displayFileInfo(String desc) {
		if(desc == null) {
			desc = "unknown file description";
		}
		String endDate = snapFileEndDates.get(desc);
		if(endDate == null) {
			endDate = "unknown end date";
		}
		String infoText = "<html><br>"+ desc + "</b><br>Last updated:&nbsp;&nbsp;<b>" + endDate + "<br></b><br></html>";
		infoLabel.setText(infoText);
	}
	
	private void importFile() {
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		jfc.setFileFilter(null);
		int c = jfc.showOpenDialog(null);
		if (c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION) return;
		importedPath = jfc.getSelectedFile().getPath();
		if (!loadURL(getDBName(), importedPath)) importedPath = null;
	}
	
	public void displayHelp() {
		String helpText = "";
		String s = "";
		URL helpURL = null;
		if(MapApp.BASE_URL.matches(MapApp.DEV_URL)){
			VELOCITY_VECTOR_HELP_URL = VELOCITY_VECTOR_HELP_URL.replace("http://app.", "http://app-dev.")
					.replace("https://app.", "https://app-dev.");
		}
		try {
			helpURL = URLFactory.url(VELOCITY_VECTOR_HELP_URL);
			BufferedReader in = new BufferedReader( new InputStreamReader( helpURL.openStream() ) );

			while((s=in.readLine())!=null){
				helpText = helpText.concat(s);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	    // for copying style
	    JLabel label = new JLabel();
	    Font font = label.getFont();

	    // create some css from the label's font
	    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
	    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
	    style.append("font-size:" + font.getSize() + "pt;");
		
	    //need to use editor pane and hyperlink listener so that we can include hyperlinks in help text
	    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
	            + helpText //
	            + "</body></html>");

	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e)
	        {
	            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
	            	BrowseURL.browseURL(e.getURL().toString());
	        }
	    });
		ep.setEditable(false);
		ep.setBackground(label.getBackground());

		//need to do this to make Help window non-modal so that it can be kept open
		//whilst the user works on the tool
		helpDialog = new JDialog(this, "GPS Velocity Vectors Help");
		JOptionPane helpPane = new JOptionPane(ep, JOptionPane.PLAIN_MESSAGE); 
		helpPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("value")) {
					switch ((Integer)e.getNewValue()) {
					case JOptionPane.OK_OPTION:
						break;
					}
					helpDialog.dispose();
				}
			}
		});
		
		helpDialog.setContentPane(helpPane);
		helpDialog.pack();
		helpDialog.setLocationRelativeTo(this);
		helpDialog.setVisible(true);
		helpDialog.toFront();

	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == map) {
			if (ds == null) return;
			ds.mouseClicked(e);
			map.repaint();
		}
		else if (e.getSource() == ds.dataT) {
			map.repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
}