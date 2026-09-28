package haxby.db.custom;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.PlainDocument;

import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geomapapp.io.GMARoot;
import org.geomapapp.util.Icons;
import org.geomapapp.util.ParseLink;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import jxl.CellType;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;

public class DBInputDialog extends JDialog implements ActionListener,
												WindowListener{

	public static String IMPORT_UNKNOWN_TEXT_FILE = "Import unknown text file"; // used by Survey Planner
	public static String IMPORT_PIPE_TEXT_FILE = "Import from Pipe URL...";
	public static String IMPORT_EXCEL_URL = "Import from Excel URL...";
	public static String IMPORT_ASCII_URL = "Import from ASCII URL...";
	public static String IMPORT_EXCEL_FILE = "Import from Excel-formatted file (.xls)...";
	public static String IMPORT_COMMA_TEXT_FILE = "Import from comma-delimited ASCII (text) file...";
	public static String IMPORT_CLIPBOARD = "Import from Clipboard (paste)...";
	public static String IMPORT_TAB_TEXT_FILE = "Import from tab-delimited ASCII (text) file...";
	public static int TAB_DELIMITER_INDEX = 0;
	public static int COMMA_DELIMITER_INDEX = 1;
	public static int PIPE_DELIMITER_INDEX = 2;
	

	//GMA 1.4.8: Add window listener so appropriate command from the main
	//File menu can be called when the input dialog window is opened
	static int num = 0;
	static int windowCount;
	public JTextArea input;
	public JTextField name;
	public JComboBox delim;
	protected JLabel detectMessage;
	BoxLayout boxL;
	public JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
	ExcelFileFilter eff = new ExcelFileFilter();
	Vector<DBDescription> history = new Vector<DBDescription>();
	Vector<DBDescription> bookmarks = new Vector<DBDescription>();
	Vector<DBDescription> dataBases = new Vector<DBDescription>();
	public DBDescription desc;

//	GMA 1.4.8: Allow title of dataset to be set, default is "Untitled"
	public String titleOfDataset = "Untitled";
	int type=-1;
	String path;
	JMenu bmM;
	protected JButton oDialog = new JButton("OK");
	protected JButton cDialog = new JButton("Cancel");
	protected JButton rDialog = new JButton("Refresh");
	protected int omitCount = 0;	// count the number of omitted commented out lines starts with #

//	GMA 1.4.8: String loadOption indicates which load command to execute based on option 
//	selected in the main File menu
	String loadOption = null;

	public DBInputDialog(Frame owner){
//		GMA 1.4.8: "Custom Databases" window renamed "Imported Data Tables"
//		super(owner,"Custom Databases", true);
		super(owner,"Imported Data Tables", true);
		num++;
		initGUI(owner);
	}

//	***** GMA 1.4.8: New constructor so that functionality can be activated according to what 
//	higher-level options have been selected; the checks on the string should correspond to the 
//	options in MapApp.java in the "Import Data Tables" menu
	public DBInputDialog(Frame owner, String currentLoadOption, String title){
		super(owner,"Imported Data Tables", true);
		loadOption = currentLoadOption;

		if ( title != null ) {
			titleOfDataset = title;
		} else
			titleOfDataset = "Untitled";
		num++;
		initGUI(owner);
	}
//	***** GMA 1.4.8

	public void initGUI(Frame owner) {
		JMenuBar mb = new JMenuBar();
		JMenu menu2 = new JMenu("File");
		menu2.setMnemonic(KeyEvent.VK_F);
		JMenuItem mi = new JMenuItem("Data Format Requirements");
		mi.setIcon(Icons.getIcon(Icons.INFO, false));
		menu2.add(mi);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFormatInfo();
			}
		});
		JMenu menu = new JMenu("Import your data");
		//menu2.add(menu2);
		mi = new JMenuItem("Import your data");
		//mi.addActionListener(this);
		menu2.add(mi);
		mi = new JMenuItem("	- Import from clipboard (paste)");
		mi.addActionListener(this);
		mi.setActionCommand("paste");
		menu2.add(mi);
		mi = new JMenuItem("	- Import from ASCII (text) file");
		mi.addActionListener(this);
		mi.setActionCommand("file");
		menu2.add(mi);
		mi = new JMenuItem("	- Import from Excel spreadsheet");
		mi.addActionListener(this);
		mi.setActionCommand("excelFile");
		menu2.add(mi);
		mi = new JMenuItem("	- Import from ASCII url");
		mi.addActionListener(this);
		mi.setActionCommand("url");
		menu2.add(mi);
		mi = new JMenuItem("	- Import from Excel url");
		mi.addActionListener(this);
		mi.setActionCommand("excelURL");
		menu2.add(mi);
		//menu2.add(menu);
		menu2.add(new JSeparator());
		mi = new JMenuItem("Browse General Data Viewer -> Tables");
		mi.setActionCommand("browse");
		mi.addActionListener(this);
		menu2.add(mi);
		File rt = GMARoot.getRoot();
		if (rt != null) {
			String fs = System.getProperty("file.separator");
			File hst = new File(rt.getPath() + fs + "history" + fs + "db.hst");
			File bm = new File(rt.getPath() + fs + "history" + fs + "db.bm");
			if (hst != null)
				history = readVectors(hst);
			if (bm != null)
				bookmarks = readVectors(bm);
		}

//		***** GMA 1.4.8: "History" was previously in "File" menu, should now be moved somewhere 
//		else
/*
		menu = new JMenu("History");
		for (int i = history.size() - 1; i >= 0; i--) {
			DBDescription d = (DBDescription) history.get(i);
			mi = new JMenuItem(d.toString());
			mi.addActionListener(this);
			mi.setActionCommand(i + "");
			menu.add(mi);
		}
		menu2.add(menu);
*/
//		***** GMA 1.4.8

		mb.add(menu2);
		menu = new JMenu("Bookmarks");
		mi = new JMenuItem("Remove Bookmark");
		mi.addActionListener(this);
		mi.setActionCommand("remove");
		menu.add(mi);
		for (int i = bookmarks.size() - 1; i >= 0; i--) {
			DBDescription d = bookmarks.get(i);
			mi = new JMenuItem(d.toString());
			mi.addActionListener(this);
			mi.setActionCommand(i + 10 + "");
			menu.add(mi);
		}
		mb.add(menu);
		bmM = menu;

		JPanel p = new JPanel(new BorderLayout());
		JPanel datasetNamePanel = new JPanel();

		boxL = new BoxLayout(p, BoxLayout.X_AXIS);
		JPanel p2 = new JPanel(new BorderLayout());
		p.add(mb, BorderLayout.WEST);

		JPanel separatePanel = new JPanel(new BorderLayout());
//		***** GMA 1.5.2: TESTING
//		p.add(new JLabel("DataSet Name:"));
		datasetNamePanel.add(new JLabel("DataSet Name:"));
//		***** GMA 1.5.2

		if ( titleOfDataset.equals("Untitled") ) {
			name = new JTextField("untitled" + num, 15); // number count of untitled filename
		}
		else {
			name = new JTextField(titleOfDataset, 15);
		}
//		***** GMA 1.5.2: TESTING
		datasetNamePanel.add(name);
		separatePanel.add(datasetNamePanel, BorderLayout.WEST);
		separatePanel.add(new JLabel("Delimited:"), BorderLayout.CENTER);
		delim = new JComboBox();
		delim.addItem("Tab");
		delim.addItem("Comma");
		delim.addItem("Pipe");
		
		if ( loadOption == IMPORT_TAB_TEXT_FILE ) {
			delim.setSelectedItem("Tab");
		} else if (loadOption == IMPORT_PIPE_TEXT_FILE) {
			delim.setSelectedItem("Pipe");
		} else if (loadOption == IMPORT_CLIPBOARD) {
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			try {
				String text = (String) c.getData(DataFlavor.stringFlavor);
				// if pasted from clipboard, try and work out delimiter from first line
				if (text != null && text.indexOf("|") != -1) {
					delim.setSelectedItem("Pipe");
				} else if ( text != null && text.indexOf("\t") != -1 ) {
					delim.setSelectedItem("Tab"); 
				} else {
					delim.setSelectedItem("Comma");
				}
			
			} catch (UnsupportedFlavorException e1) {
				delim.setSelectedItem("Tab");
			} catch (IOException e1) {
				delim.setSelectedItem("Tab");
			}	
		} else {
			delim.setSelectedItem("Comma");
		}
		
		separatePanel.add(delim,BorderLayout.EAST);
		p.add(separatePanel, BorderLayout.CENTER);
		p2.add(p, BorderLayout.NORTH);
		input = new JTextArea("", 13, 50);
		input.setBackground(new Color(255, 255, 204));
		/*input = new JTextArea("Use the File menu to import Excel spreadsheets and text tables.\n" +
				"IMPORTANT: For text tables, please select tab-separated or comma-separated from\n" +
				"pull-down box at upper right.\n" +
				"Requires latitude and longitude as column identifiers.\n" +
				"The following are acceptable latitude and longitude column identifiers: \n" +
				"Latitude, latitude, Lat, lat, Longitude, longitude, Lon, lon \n" +
				"Latitude and longitude must be decimal degrees \n" +
				"with negative values for southern and western hemispheres \n" +
				"NOTE: A table copied from a spreadsheet can be " +
				"directly pasted into this text area and manipulated \nprovided it has " +
				"columns for latitude and longitude.  \nData can also be directly " +
				"typed into this text area.", 10, 50);
		*/
		/*input.addMouseListener(new MouseListener()	{
			public void mouseClicked(MouseEvent e) {
				input.selectAll();
				input.replaceSelection("");
				input.removeMouseListener(this);
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}	
		});*/
		input.selectAll();
		input.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent evt) {
				if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_V)
					type = -1;
			};

			public void keyReleased(KeyEvent evt) {
			};

			public void keyTyped(KeyEvent evt) {
				if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_V)
					type = -1;
			};
		});
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				input.requestFocus();
			}
		});
		JScrollPane sp = new JScrollPane(input);
		p2.add(sp);
		JPanel p3 = new JPanel();
		p3.add(rDialog);
		rDialog.addActionListener(this);
		rDialog.setActionCommand("refresh");

		p3.add(oDialog);
		oDialog.addActionListener(this);
		oDialog.setActionCommand("ok");

		p3.add(cDialog);
		cDialog.addActionListener(this);
		cDialog.setActionCommand("cancel");

		getContentPane().setLayout(new BorderLayout());
		p2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		getContentPane().add(p2, BorderLayout.NORTH);
		// Set up the alert message but don't show until needed.
		detectMessage = new JLabel("");
		Font labelFont = detectMessage.getFont();
		detectMessage.setFont(new Font(labelFont.getName(), Font.PLAIN, 14));
		detectMessage.setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 4));
		getContentPane().add(detectMessage,BorderLayout.WEST);
		getContentPane().add(p3, BorderLayout.EAST);
		// setSize(400, 400);
		setLocation(owner.getX() + owner.getWidth() / 2 - 200, owner.getY()
				+ owner.getHeight() / 2 - 200);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		pack();
		addWindowListener(this);
	}

	public String getDelimeter() {
		int i = delim.getSelectedIndex();
		if (i == TAB_DELIMITER_INDEX) return "\t";
		else if (i == COMMA_DELIMITER_INDEX) return ",";
		else return "|";
	}

	public boolean valid(){
		if (name.getText().length()>0 && input.getText().length() > 0)
			return true;
		return false;
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("cancel")){
			input = null;
			setVisible(false);
		} else if (evt.getActionCommand().equals("ok") && valid()) { ok();
		} else if (evt.getActionCommand().equals("refresh")) {
			String strTA;
			omitCount = 0;
			BufferedReader readTextArea = new BufferedReader(new StringReader(input.getText()));
			try {
				while ((strTA = readTextArea.readLine()) != null) { 
					if (strTA.length() > 0) {
						if(strTA.startsWith("#")) {
							omitCount ++;	// Get comment count
						}
					}   
				}
				System.out.println("Detected " + omitCount + " commented row(s). Commented data rows will not be plotted.");
				detectMessage.setText("<html>Detected <b>" + omitCount + " </b> commented row(s).<br>Commented data rows <b>will not</b> be plotted.<br>Click Refresh to view updated count.</html>");
				pack();
			} catch(IOException e) {
			  e.printStackTrace();
			}
		} else if (evt.getActionCommand().equals("file")) { loadFile();
		} else if (evt.getActionCommand().equals("url")) { loadURL();
		} else if (evt.getActionCommand().equals("excelFile")) { loadExcelFile();
		} else if (evt.getActionCommand().equals("excelURL")) { loadExcelURL();
		} else if (evt.getActionCommand().equals("paste")) { pasteFromClip();
		} else if (evt.getActionCommand().equals("remove")) { removeBookmark();
		} else if (evt.getActionCommand().equals("browse")) { browseAvalibleFiles();
		} else {
			load(Integer.parseInt(evt.getActionCommand()));
		}
	}

	private void removeBookmark(){
		if (bookmarks.size()==0) return;
		Object o = JOptionPane.showInputDialog(this, "Choose Bookmark to remove", "Remove Bookmark", JOptionPane.QUESTION_MESSAGE, null, bookmarks.toArray(), bookmarks.get(0));
		if (o==null) return;
		bookmarks.remove(o);
		File rt = GMARoot.getRoot();
		if (rt==null) return;
		String fs = System.getProperty("file.separator");
		File bm = new File(rt.getPath()+fs+"history"+fs+"db.bm");
		File hstD = new File(rt.getPath()+fs+"history");
		hstD.mkdir();
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(bm));
			for (int i = 0; i < bookmarks.size(); i++)
				out.writeObject(bookmarks.get(i));
			out.close();
			JOptionPane.showMessageDialog(MapApp.anchor, "Bookmark Removed.", "Succesful", JOptionPane.INFORMATION_MESSAGE);

			bmM.removeAll();
			JMenuItem mi = new JMenuItem("Remove Bookmark");
			mi.addActionListener(this);
			mi.setActionCommand("remove");
			bmM.add(mi);
			bmM.add(new JSeparator());
			for (int i = bookmarks.size()-1; i >=0; i--){
				DBDescription d = bookmarks.get(i);
				mi = new JMenuItem(d.toString());
				mi.addActionListener(this);
				mi.setActionCommand(i+10+"");
				bmM.add(mi);
			}
		} catch (IOException e) {JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);}
	}

	private void ok() {
		if (type >-1){
			desc = new DBDescription(name.getText(),type,path);
			for (int i = Math.max(history.size()-10,0); i < history.size(); i++)
				if (desc.equals((history.get(i)))) {history.remove(i); break;}

			history.add(desc);
			File rt = GMARoot.getRoot();
			String fs = System.getProperty("file.separator");
			File hstD = new File(rt.getPath()+fs+"history");
			hstD.mkdir();
			File hst = new File(rt.getPath()+fs+"history"+fs+"db.hst");
			try {
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(hst));
				for (int i = Math.max(history.size()-10,0); i < history.size(); i++) {
					out.writeObject(history.get(i));
				}
				out.close();
			} catch (IOException ex) {}
		} else {
			desc = new DBDescription(name.getText(),-1,null);
		}
		setVisible( false );
	} 

	public void loadFile(){
		type = UnknownDataSet.ASCII_FILE;
		jfc = haxby.map.MapApp.getFileChooser();
		jfc.setFileFilter(null);
		int c = jfc.showOpenDialog(this);
		if (c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION) {
			return;
		}
		path = jfc.getSelectedFile().getPath();
		loadFile(jfc.getSelectedFile());
	}

	public void loadFile(File f){
		final File f1 = f;
		final String lo = loadOption; 
		new Thread(){
			File f = f1;
			String loadOption = lo;
			public void run(){
			int length = (int) f.length();
			// Create a JProgressBar + JDialog
			JDialog d = new JDialog((Frame)null, "Loading File");
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			d.setLocationRelativeTo(MapApp.anchor);
			JProgressBar pb = new JProgressBar(0,length);
			p.add(new JLabel("Loading " + (length / 1000) + " kb file"), BorderLayout.NORTH);
			p.add(pb);
			d.getContentPane().add(p);
			d.pack();
			d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			d.setVisible(true); 
			d.setAlwaysOnTop(true);
		try {
			BufferedReader in = new BufferedReader(new FileReader(f));
			StringBuffer strBuff = new StringBuffer();
			String i = in.readLine();
			String fileName;
			boolean delimFound = false;
			if (loadOption == IMPORT_TAB_TEXT_FILE || 
					loadOption == IMPORT_PIPE_TEXT_FILE ||
					loadOption == IMPORT_COMMA_TEXT_FILE) delimFound = true;
			
			while (i!=null) {
				pb.setValue(pb.getValue() + (2*i.length()+38));
				pb.repaint();
				if(i.startsWith("#")) {
					omitCount ++;	// Get comment count
				} else { 
					// if delimeter is not specified on the menu selection
					// then try and work it out from first line
					if (!delimFound) {
						if (i != null && i.indexOf("|") != -1) {
							delim.setSelectedItem("Pipe");
							delimFound = true;
						} else if ( i != null && i.indexOf("\t") != -1 ) {
							delim.setSelectedItem("Tab"); 
							delimFound = true;
						} else if ( i != null && i.indexOf(",") != -1 ){
							delim.setSelectedItem("Comma");
							delimFound = true;
						}
					}
				}
				
				strBuff.append(i+"\n");
				i=in.readLine();
			}
			if (!delimFound) delim.setSelectedItem("Comma");
			
			detectMessage.setText("<html>Detected <b>" + omitCount + " </b> commented row(s).<br>Commented data rows (beginning with #) <b>will not</b> be plotted.<br>Click Refresh to view updated count.</html>");
			pack();
			// Catch if doesn't have file type ending.
			try {
				fileName = f.getName().substring(0, f.getName().lastIndexOf('.'));
			} catch (StringIndexOutOfBoundsException se) {
				fileName = f.getName();
			}
			name.setText(fileName);
			in.close();
			d.dispose();
			input.setText(strBuff.toString());
			MapApp.sendLogMessage("Imported_ASCII_Table&name="+name.getText());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(MapApp.anchor, "Error loading file:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		}
		}.start();
	}

	// Determines which excel format type file is selected and opens appropriately
	public void loadExcelFile() {
		type = UnknownDataSet.EXCEL_FILE;
		jfc = haxby.map.MapApp.getFileChooser();
		jfc.setFileFilter(eff);
		int c = jfc.showOpenDialog(this);
		if (c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION) {
			// If user cancels then close the dialog box also.
			input = null;
			setVisible(false);
			return;
		}
		path = jfc.getSelectedFile().getPath();
		if(jfc.getSelectedFile().getName().endsWith("xls")){
			loadExcelFile(jfc.getSelectedFile());
		}else if(jfc.getSelectedFile().getName().endsWith("xlsx")){
			loadExcelTypeXLSX(jfc.getSelectedFile());
		}else{
			return;
		}
	}

	// Reads the selected file type .xls and sets it as the input text.
	public void loadExcelFile(File f){
		final File f1 = f;
		final DBInputDialog diag = this;
		new Thread(){
			File f = f1;
			public void run(){
		int length = (int)f.length();
		JDialog d = new JDialog((Frame)null, "Loading File");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Loading " + (length / 1000) + " kb file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);
		d.pack();
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setVisible(true); 
		d.setAlwaysOnTop(true);
		String excelPath = f.getPath();
		//HSSFWorkbook workbook = new HSSFWorkbook();

		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			workbook = new HSSFWorkbook(new FileInputStream(excelPath));
			if (workbook.getNumberOfSheets()<0) return;
			//System.out.println(workbook.getNumberOfSheets());

			StringBuffer sb2 = new StringBuffer();
			if(workbook.getSheetAt(0) != null){
				String sheetname = workbook.getSheetName(0);
				//	XSSFExcelExtractor extract = null;
				HSSFSheet worksheet = workbook.getSheetAt(0);
				// Check the columns usually first row of titles for columns. Just incase check next row.
				int numCol = worksheet.getRow(0).getLastCellNum();
				int numCol1 = worksheet.getRow(1).getLastCellNum();
				if (numCol1 > numCol) {
					numCol = numCol1;
				}

				HSSFFormulaEvaluator formulaEvaluator = new HSSFFormulaEvaluator(workbook);
				omitCount = 0;
				for (int i = 0; i < worksheet.getLastRowNum()+1; i++) {
					for (int j = 0; j < numCol; j++) {
						formulaEvaluator.evaluateInCell(worksheet.getRow(i).getCell(j));
						sb2.append(worksheet.getRow(i).getCell(j) + "\t");
						pb.setValue(pb.getValue()+ (((""+worksheet.getRow(i).getCell(j)).length()*2) + 38));
						pb.repaint();
					}
					sb2.append("\n");
					try {
					 if(formulaEvaluator.evaluateInCell(worksheet.getRow(i).getCell(0)).getStringCellValue().startsWith("#")) {
						 omitCount ++; 
					 }
					} catch (IllegalStateException ix) {}
					//System.out.println(worksheet.getRow(1).getCell(1)+ " R " + worksheet.getLastRowNum() + " c " + worksheet.getRow(1).getLastCellNum());
					//String excelText = extract.getText().replaceFirst(sheetname+"\n", "");
				}
				name.setText(f.getName().substring(0, f.getName().lastIndexOf('.')));
				input.setText(sb2.toString());
				MapApp.sendLogMessage("Imported_Excel_Table&name="+name.getText());
				System.out.println("Detected " + omitCount + " commented row(s). Commented data rows will not be plotted.");
				detectMessage.setText("<html>Detected <b>" + omitCount + " </b> commented row(s).<br>Commented data rows <b>will not</b> be plotted.<br>Click Refresh to view updated count.</html>");
				pack();
			}
		} catch (OldExcelFormatException oe){
			d.setVisible(false);
			d.setEnabled(false);
			cDialog.doClick();
			JOptionPane.showMessageDialog(MapApp.anchor, "The supplied spreadsheet seems to be Excel 5.0/7.0 format\n" +
												"or older and is not supported. Please save the file in a newer\n" +
												"Excel format (such as 97-2003 .xls or 2007 .xlsx) and try\n" +
												"again to import it.", "Warning",JOptionPane.ERROR_MESSAGE ) ;// message to user
			oe.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		d.dispose();
		System.out.println("data ready");
		}
		}.start();
	}

	// Reads the selected file type .xlsx and sets it as the input text.
	public void loadExcelTypeXLSX(File f){
		final File f1 = f;

		new Thread(){
			File f = f1;
			public void run(){
		int length = (int)f.length();
		JDialog d = new JDialog((Frame)null, "Loading File");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Loading " + (length / 1000) + " kb file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);
		d.pack();
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setVisible(true); 
		d.setAlwaysOnTop(true);

		//String excelPath = f1.getPath();
		//XSSFWorkbook workbook = new XSSFWorkbook(excelPath);
		omitCount = 0;
		try {
			FileInputStream fis = new FileInputStream(f);
			XSSFWorkbook workbook = new XSSFWorkbook(fis);

			if (workbook.getNumberOfSheets()<0) {return;}

			//System.out.println(workbook.getNumberOfSheets());
			StringBuffer sb2 = new StringBuffer();
		if(workbook.getSheetAt(0) != null){
			String sheetname = workbook.getSheetName(0);
		//	XSSFExcelExtractor extract = null;
			XSSFSheet worksheet = workbook.getSheetAt(0);
			// Check the columns usually first row of titles for columns. Just incase check next row.
			int numCol = worksheet.getRow(0).getLastCellNum();
			int numCol1 = worksheet.getRow(1).getLastCellNum();
			if (numCol1 > numCol) {
				numCol = numCol1;
			}

			XSSFFormulaEvaluator formulaEvaluator = new XSSFFormulaEvaluator(workbook);

			for (int i = 0; i < worksheet.getLastRowNum()+1; i++) {
				for (int j = 0; j < numCol; j++){
					formulaEvaluator.evaluateInCell(worksheet.getRow(i).getCell(j));
					sb2.append(worksheet.getRow(i).getCell(j) + "\t");
					pb.setValue(pb.getValue()+ (((""+worksheet.getRow(i).getCell(j)).length()*2) + 38));
					pb.repaint();
				}
				sb2.append("\n");
				try {
					if(worksheet.getRow(i).getCell(0).getStringCellValue().startsWith("#")) {
						omitCount ++;
					}
				} catch (IllegalStateException ix) {}
		//System.out.println(worksheet.getRow(1).getCell(1)+ " R " + worksheet.getLastRowNum() + " c " + worksheet.getRow(1).getLastCellNum());
		//String excelText = extract.getText().replaceFirst(sheetname+"\n", "");
		//input.setText(excelText);
			}
			name.setText(f.getName().substring(0, f.getName().lastIndexOf('.')));
			//System.out.println(sb2.toString());
			input.setText(sb2.toString());
			MapApp.sendLogMessage("Imported_ExcelXLSX_Table&name="+name.getText());
			System.out.println("Detected " + omitCount + " commented row(s). Commented data rows will not be plotted.");
			detectMessage.setText("<html>Detected <b>" + omitCount + " </b> commented row(s).<br>Commented data rows <b>will not</b> be plotted.<br>Click Refresh to view updated count.</html>");
			pack();
		}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		d.dispose();
		System.out.println("data ready");
		}
		}.start();
	}

	public void loadURL(){
		type = UnknownDataSet.ASCII_URL;
		String i = JOptionPane.showInputDialog(this, "<html><b>Load a URL</b> (Tab, Comma or Pipe Delimited):</html>", "URL to Load",  JOptionPane.PLAIN_MESSAGE);
		if (i==null || i.length()<1) return;
		path = i;
		loadURL(i);
	}

	public void loadExcelURL(){
		type = UnknownDataSet.EXCEL_URL;
		String c = JOptionPane.showInputDialog(this, "URL to load:");
		if (c==null || c.length()<1) return;
		path = c;
		loadExcelURL(c);
	}

	public void loadURL(String i){
		final String i1 = i;
		final DBInputDialog diag = this;
		final JDialog d = new JDialog((Frame)null, "Importing File");
		new Thread(){
			String i = i1;
			public void run(){
			try {
				//check to see if the URL is being redirected
				//eg to https version of the page
				URL url = URLFactory.url(URLFactory.checkForRedirect(i));
				String urlFileName;
	
				d.setLocationRelativeTo(MapApp.anchor);
				d.setSize(180, 90);
				JPanel p = new JPanel(new BorderLayout());
				p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	
				int count = 0;
				boolean dialogMessage = true;
				boolean stopImport = false;
				JProgressBar pb = new JProgressBar(0,50000);
				pb.setIndeterminate(true);
				p.add(pb,BorderLayout.CENTER);
	
				JLabel lab = new JLabel("Gathering Data ...");
				lab.setVisible(true);
				p.add(lab, BorderLayout.NORTH);
	
				d.getContentPane().add(p,"Center");
				//d.pack();
				d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				d.setVisible(true); 
				d.setAlwaysOnTop(true);				
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				//StringBuffer strBuff = new StringBuffer();
				while ((i = in.readLine())!=null){
					if (i.startsWith("#")) {
						diag.input.append(i+"\n");
						omitCount++;
						continue;
					}
					break;
				}	

				if (i != null && i.indexOf("|") != -1) {
					delim.setSelectedItem("Pipe");
				} else if ( i != null && i.indexOf("\t") != -1 ) {
					delim.setSelectedItem("Tab"); 
				} else {
					delim.setSelectedItem("Comma");
				}
				
				/*  Some comma separated file from an outside URL might have String quotes and
				 * commas within them such as time and date.
				 * Prepare each comma separated line from URL file to append into GMA.
				 * Strip out the quotations and comma in between a quotation if it exists.
				 */
				while (i!=null) {
					while(i.contains(",\"") && i.contains("\",")){
						int open = i.indexOf(",\"") + 1;
						int close = i.indexOf("\",") + 1;
						String toModify = i.substring(open, close);
						String toReplace =  i.substring(open, close).replace(",", " ").replace("\"", "");
						i = i.replace(toModify, toReplace);
					}
					if(i.contains("\"")){
						i = i.replace("\"", "");
					}
					pb.setValue(pb.getValue() + i.length());
					pb.repaint();
					//strBuff.append(i+"\n");
					diag.input.append(i+"\n");
					i=in.readLine();
					if (i != null && i.startsWith("#")) omitCount++;
			
					if(stopImport) {
						i.equals(null);
						break;
					}
					if(count == 0) {
						lab.setText("Importing Data " + count);
					}
					count++;

					if ((count % 1000) == 0 ) {
						lab.setText("Importing Data " + count);
					}
					// Let the user choose.
					if(count >= 100000 && dialogMessage) {
						Object[] options = {"Continue to Import",		// okay
						"Plot Current Records"};	// cancel
						int selection = JOptionPane.showOptionDialog(delim.getParent(), "<html>The selected data set has over 100,000 records.</html>\n"
								+ "Continuing import might result in memory failure.", 
								"More Than 100,000 Records", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
						if (selection == 0) {
							dialogMessage = false;
							continue;
							
		                } else if (selection == 1) {
		                	dialogMessage = false;
		                	stopImport = true;
		                }
					}
				}
				// Catch if doesn't have file type ending.
				try {
					urlFileName = url.getFile().substring(url.getFile().lastIndexOf('/')+1, url.getFile().lastIndexOf('.'));
				} catch (StringIndexOutOfBoundsException se) {
					urlFileName = url.getFile();
				}
				name.setText(urlFileName);
				MapApp.sendLogMessage("Imported_ASCII_URL_Table&name="+name.getText());
				detectMessage.setText("<html>Detected <b>" + omitCount + " </b> commented row(s).<br>Commented data rows <b>will not</b> be plotted.<br>Click Refresh to view updated count.</html>");
				pack();
				in.close();
				d.dispose();

				//diag.input.setText(strBuff.toString());
				} catch (Exception e) {
					e.printStackTrace();
					d.dispose();
					JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}.start();
	}

	public void loadExcelURL(String c){
		final String c1 = c;
		new Thread(){
			public void run(){
		System.out.println("load excel url");
		try {
			URL url = URLFactory.url(c1);
			int length = url.openConnection().getContentLength();


			JDialog d = new JDialog((Frame)null, "Loading File");
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			d.setLocationRelativeTo(MapApp.anchor);
			JProgressBar pb = new JProgressBar(0,length);
			JLabel lab = new JLabel("Loading " + (length / 1000) + "kb file");
			lab.setVisible(true);
			p.add(lab, BorderLayout.NORTH);
			p.add(pb,BorderLayout.CENTER);
			d.getContentPane().add(p,"Center");

			d.pack();
			d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			d.setVisible(true);
			d.setAlwaysOnTop(true);
			Workbook wb = Workbook.getWorkbook(url.openStream());
			if (wb.getNumberOfSheets()==0) return;
			Sheet s = wb.getSheet(0);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < s.getRows(); i++) {
				for (int j = 0; j < s.getColumns(); j++){
					if (s.getCell(j, i).getType() == CellType.NUMBER && !s.getCell(j, i).getContents().matches("\\d*"))
					{
						sb.append(((NumberCell)s.getCell(j, i)).getValue()+"\t");
						pb.setValue(pb.getValue()+ 16);
						pb.repaint();
					}
					else {
						sb.append(s.getCell(j, i).getContents()+"\t");
						pb.setValue(pb.getValue()+ (2*s.getCell(j, i).getContents().length()) + 36);
						pb.repaint();
					}
					//if (j<s.getRows()-1) sb.append(s.getCell(j, i).getContents()+"\t");
					//else sb.append(s.getCell(j, i).getContents()+"\t");
				}
				sb.append("\n");
			}
			name.setText(url.getFile().substring(url.getFile().lastIndexOf('/')+1, url.getFile().lastIndexOf('.')));
			input.setText(sb.toString());
			MapApp.sendLogMessage("Imported_Excel_URL_Table&name="+name.getText());
			wb.close();
			d.dispose();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
			}
		}.start();
	}

	public void pasteFromClip(){
		type=-1;
		input.paste();
	}

	public Vector<DBDescription> readVectors(File f){
		Vector<DBDescription> v = new Vector<DBDescription>();
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
			Object o;
			while ((o=in.readObject())!=null) 
				v.add((DBDescription) o);
		} catch (Exception ex) { }
		return v;
	}

	private void browseAvalibleFiles() {
		//Show Tree Dialog and load selected file
		Vector v = (new XMLJTreeDialog(this))
				.getSelection().getProperties();

//		Vector v = XMLJTreeDialog.showXMLJTree(DATABASE_XML_LISTING, this);
		if (v.size()==0) return;

		int type = -1;
		try {
			Object obj = ParseLink.getProperty(v, "type");

			if (obj==null) {
				JOptionPane.showMessageDialog(this,
						"No type tag found in database entry; Failed to load",
						"Failed to load", JOptionPane.ERROR_MESSAGE);
				return;
			} else if (obj instanceof Integer) {
				type = ((Integer) obj).intValue();
			} else {
				type = Integer.parseInt((String) obj);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"Could not parse type tag in database entry; Failed to load",
					"Failed to load", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String url = null;
		try {
			url = (String) ParseLink.getProperty(v, "url");
			if (url==null) {
				JOptionPane.showMessageDialog(this,
						"No url tag found in database entry; Failed to load",
						"Failed to load", JOptionPane.ERROR_MESSAGE);
				return;
			} else {
				URLFactory.url(url);
			}
		} catch (IOException ex){
			JOptionPane.showMessageDialog(this,
					"Invalid URL protocol in database entry; Failed to load",
					"Failed to load", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (type==UnknownDataSet.ASCII_URL) loadURL(url);
		else if (type==UnknownDataSet.EXCEL_URL) loadExcelURL(url); 
		
		String str = (String) ParseLink.getProperty(v, "name");
		str = str==null?"Untitled":str;
		name.setText(str);
		loadOption = null;

	SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				DBInputDialog.this.ok();
			}
		});
	}

	private void load(int x){
		DBDescription d; 
		if (x>=0&&x<10) d = history.get(x);
		else if (x<0) d = dataBases.get(x*-1-1);
		else d = bookmarks.get(x-10);
		if (d.type==0) loadFile(new File(d.path));
		if (d.type==1) loadExcelFile(new File(d.path));
		if (d.type==2) loadURL(d.path);
		if (d.type==3) loadExcelURL(d.path);
		name.setText(d.name);
		path=d.path;
		type=d.type;
	}

	private void showFormatInfo() {
		String infoURL = PathUtil.getPath("HTML/IMPORTING_DATA_HELP",
				MapApp.BASE_URL+"/gma_html/Importing_Data.html");
		BrowseURL.browseURL(infoURL);
	}

//	***** GMA 1.4.8: Window listeners added to invoke appropriate load function according to 
//	option selected in main File menu under the "Import Data Tables" menu.
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		setVisible( false );
		input = null;
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {
		if (loadOption != null)
		{
			if ( loadOption == IMPORT_CLIPBOARD ) {
				pasteFromClip();
			}
			else if ( loadOption == IMPORT_PIPE_TEXT_FILE) {
				delim.setSelectedItem("Pipe");
				loadFile();
			}
			else if ( loadOption ==  IMPORT_TAB_TEXT_FILE) {
				delim.setSelectedItem("Tab");
				loadFile();
			}
			else if ( loadOption == IMPORT_COMMA_TEXT_FILE ) {
				delim.setSelectedItem("Comma");
				loadFile();
			}
			else if ( loadOption ==  IMPORT_UNKNOWN_TEXT_FILE) {
				loadFile();
			}
			else if ( loadOption == IMPORT_EXCEL_FILE ) {
				delim.setSelectedItem("Tab");
				loadExcelFile();
			}
			else if ( loadOption == IMPORT_ASCII_URL ) {
				loadURL();
			}
			else if ( loadOption == IMPORT_EXCEL_URL ) {
				loadExcelURL();
			}
			loadOption = null;
		}
	}

//	***** GMA 1.4.8
	@Override
	public void dispose() {
		super.dispose();

		if ( input != null ) {
			input.setDocument( new PlainDocument() );
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public String getFilename() {
		File f = new File(path);
		return f.getName();
	}
}
