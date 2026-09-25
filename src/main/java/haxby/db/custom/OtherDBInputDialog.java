package haxby.db.custom;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Frame;
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
import java.net.URL;
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
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;

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

public class OtherDBInputDialog extends 	JDialog 
								implements 	ActionListener,
											WindowListener	{
	//public static final String DATABASE_XML_LISTING =
	//	haxby.map.MapApp.TEMP_BASE_URL + "database/globalDB.xml";

	static int num=0;
	static int windowCount;
	public JTextArea input;
	public JTextField name;
	public JComboBox delim;
	BoxLayout boxL;
	JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
	ExcelFileFilter eff = new ExcelFileFilter();
	Vector history=new Vector();
	Vector bookmarks=new Vector();
	Vector dataBases=new Vector();
	public DBDescription desc;
	int type=-1;
	String path;
	JMenu bmM;
	String selectedDataset = null;
	String selectedDatasetURL = null;
	int selectedDatasetType = -1;

	public OtherDBInputDialog(Frame owner){
		super(owner,"Custom Databases", true);
		num++;
		initGUI(owner);
	}

	public OtherDBInputDialog(Frame owner, String datasetToOpen, String datasetToOpenURL, int datasetToOpenType){
		super(owner,"Custom Databases", true);
		num++;
		selectedDataset = datasetToOpen;
		selectedDatasetURL = datasetToOpenURL;
		selectedDatasetType = datasetToOpenType;
		initGUI(owner);
	}

	public void initGUI(Frame owner) {
		JMenuBar mb = new JMenuBar();
		JMenu menu2 = new JMenu("File");
		menu2.setMnemonic(KeyEvent.VK_F);
		menu2.setPopupMenuVisible(true);
		JMenuItem mi = new JMenuItem(" Data Format Requirements");
		mi.setIcon(Icons.getIcon(Icons.INFO, false));
		menu2.add(mi);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFormatInfo();
			}
		});
		//menu2.add(menu);

		mi = new JMenuItem("Browse General Data Viewer -> Tables");
		mi.setActionCommand("browse");
		mi.addActionListener(this);
		menu2.add(mi);
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

		JMenu menu = new JMenu("History");
		for (int i = history.size() - 1; i >= 0; i--) {
			DBDescription d = (DBDescription) history.get(i);
			mi = new JMenuItem(d.toString());
			mi.addActionListener(this);
			mi.setActionCommand(i + "");
			menu.add(mi);
		}
		menu2.add(menu);
		mb.add(menu2);

		menu = new JMenu("Bookmarks");
		mi = new JMenuItem("Remove Bookmark");
		mi.addActionListener(this);
		mi.setActionCommand("remove");
		menu.add(mi);
		menu.add(new JSeparator());
		for (int i = bookmarks.size() - 1; i >= 0; i--) {
			DBDescription d = (DBDescription) bookmarks.get(i);
			mi = new JMenuItem(d.toString());
			mi.addActionListener(this);
			mi.setActionCommand(i + 10 + "");
			menu.add(mi);
		}
		mb.add(menu);
		bmM = menu;

		JPanel p = new JPanel();
		boxL = new BoxLayout(p, BoxLayout.X_AXIS);

		JPanel p2 = new JPanel(new BorderLayout());
		p.add(mb);
		p.add(new JLabel("Dataset:"));
		name = new JTextField(25);
		p.add(name);

		delim = new JComboBox();
		delim.addItem("Tab");
		delim.addItem("Comma");
		p.add(delim);

		p.add(new JLabel("separated"));
		p2.add(p, BorderLayout.NORTH);
		input = new JTextArea(10, 40);
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
		p = new JPanel();
		JButton ok = new JButton("OK");
		getRootPane().setDefaultButton(ok);
		ok.addActionListener(this);
		ok.setActionCommand("ok");
		p.add(ok);
		JButton b = new JButton("Cancel");
		b.addActionListener(this);
		b.setActionCommand("cancel");
		p.add(b);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.SOUTH);
		p2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		getContentPane().add(p2);

		// setSize(400, 400);
		setLocation(owner.getX() + owner.getWidth() / 2 - 200, owner.getY()
				+ owner.getHeight() / 2 - 200);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		pack();

//		1.3.5: Add window listener so window can be closed by clicking on the 
//		close window icon and so that browse window will open initially (which 
//		is why the listener must be added before the dialog is set visible
		addWindowListener(this);

//		***** GMA 1.6.4: Do not show this window, automatically click "Ok" button
//		setVisible(true);
		if ( selectedDataset != null && !selectedDataset.equals("") && selectedDatasetURL != null && !selectedDatasetURL.equals("") && selectedDatasetType > 0 ) {
			browseAvailableFiles(selectedDataset, selectedDatasetURL, selectedDatasetType);
		}
		ok();
	}

	public String getDelimeter() {
		return delim.getSelectedIndex()==1 ? "," : "\t";
	}

	public boolean valid(){
		if (name.getText().length()>0 && input.getText().length() > 0)
			return true;
		return false;
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("cancel")){
			input = null;
			dispose();
		} else if (evt.getActionCommand().equals("ok") && valid()) ok();
		else if (evt.getActionCommand().equals("file")) loadFile();
		else if (evt.getActionCommand().equals("url")) loadURL();
		else if (evt.getActionCommand().equals("excelFile")) loadExcelFile();
		else if (evt.getActionCommand().equals("excelURL")) loadExcelURL();
		else if (evt.getActionCommand().equals("paste")) pasteFromClip();
		else if (evt.getActionCommand().equals("remove")) removeBookmark();
		else if (evt.getActionCommand().equals("browse")) browseAvailableFiles(selectedDataset, selectedDatasetURL, selectedDatasetType);
		else load(Integer.parseInt(evt.getActionCommand()));
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
				DBDescription d = (DBDescription) bookmarks.get(i);
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
				for (int i = Math.max(history.size()-10,0); i < history.size(); i++)
					out.writeObject(history.get(i));
				out.close();
			} catch (IOException ex) {}
		} else
			desc = new DBDescription(name.getText(),-1,null);
		dispose();
	} 

	public void loadFile(){
		type = UnknownDataSet.ASCII_FILE;
		jfc.setFileFilter(null);
		int c = jfc.showOpenDialog(this);
		if (c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION) return;
		path = jfc.getSelectedFile().getPath();
		loadFile(jfc.getSelectedFile());
	}

	public void loadExcelFile() {
		type = UnknownDataSet.EXCEL_FILE;
		jfc.setFileFilter(eff);
		int c = jfc.showOpenDialog(this);
		if (c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION) return;
		path = jfc.getSelectedFile().getPath();
		loadExcelFile(jfc.getSelectedFile());
	}

	public void loadURL(){
		type = UnknownDataSet.ASCII_URL;
		String i = JOptionPane.showInputDialog(this, "URL to load:");
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

	public void loadFile(File f){
		try {
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
			BufferedReader in = new BufferedReader(new FileReader(f));
			StringBuffer strBuff = new StringBuffer();
			String i = in.readLine();
			
//			GMA 1.4.8: Test for '\t' in first line of input to set proper delimiting
			if ( i != null && i.indexOf("\t") != -1 ) {
				delim.setSelectedItem("Tab");
			}
			else {
				delim.setSelectedItem("Comma");
			}
			
			while (i!=null) {
				pb.setValue(pb.getValue() + (2*i.length() + 36));
				pb.repaint();
				strBuff.append(i+"\n");
				i=in.readLine();
			}
			name.setText(f.getName().substring(0, f.getName().lastIndexOf('.')));
			in.close();
			d.dispose();
			input.setText(strBuff.toString());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error loading file:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}

	}

	public void loadExcelFile(File f){
		try {
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

			Workbook wb = Workbook.getWorkbook(new ProgressMonitorInputStream(this,"Loading",new FileInputStream(f)));
			if (wb.getNumberOfSheets()==0)return;
			Sheet s = wb.getSheet(0);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < s.getRows(); i++) {
				for (int j = 0; j < s.getColumns(); j++)
				{
						if (s.getCell(j, i).getType() == CellType.NUMBER && !s.getCell(j, i).getContents().matches("\\d*"))
						{
							pb.setValue(pb.getValue()+ 16);
							pb.repaint();
							sb.append(((NumberCell)s.getCell(j, i)).getValue()+"\t");
						}
						else
						{
							pb.setValue(pb.getValue() + 2*s.getCell(j, i).getContents().length() + 36);
							pb.repaint();
							sb.append(s.getCell(j, i).getContents()+"\t");
						}
				}
				sb.append("\n");
			}
			name.setText(f.getName().substring(0, f.getName().lastIndexOf('.')));
			input.setText(sb.toString());
			wb.close();
			d.dispose();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error loading file:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	public void loadURL(String i){

		try {

			URL url = URLFactory.url(i);
			int length = url.openConnection().getContentLength();
			JDialog d = new JDialog((Frame)null, "Loading File");
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			d.setLocationRelativeTo(MapApp.anchor);
			JProgressBar pb = null;
			if (length > 0) {
				pb = new JProgressBar(0,length);
				p.add(new JLabel("Loading " + (length / 1000) + " kb file"), BorderLayout.NORTH);
				p.add(pb);
			}
			d.getContentPane().add(p);
			d.pack();
			d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			d.setVisible(true); 
			d.setAlwaysOnTop(true);

			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			StringBuffer strBuff = new StringBuffer();
			i = in.readLine();

//			GMA 1.4.8: Test for '\t' in first line of input to set proper delimiting
			if ( i != null && i.indexOf("\t") != -1 ) {
				delim.setSelectedItem("Tab");
			}
			else {
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
				if (length > 0 && pb != null) {
					pb.setValue(pb.getValue() + (i.length()*2) + 36);
					pb.repaint();
				}
				strBuff.append(i+"\n");
				i=in.readLine();
			}

			name.setText(url.getFile().substring(url.getFile().lastIndexOf('/')+1, url.getFile().lastIndexOf('.')));
			in.close();
			d.dispose();
			this.input.setText(strBuff.toString());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	public void loadExcelURL(String c){
		try {
			URL url = URLFactory.url(c);
			int length = url.openConnection().getContentLength();
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
			Workbook wb = Workbook.getWorkbook(url.openStream());
			if (wb.getNumberOfSheets()==0) return;
			Sheet s = wb.getSheet(0);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < s.getRows(); i++) {
				for (int j = 0; j < s.getColumns(); j++)
				{
						if (s.getCell(j, i).getType() == CellType.NUMBER && !s.getCell(j, i).getContents().matches("\\d*")) {
							sb.append(((NumberCell)s.getCell(j, i)).getValue()+"\t");	
							pb.setValue(pb.getValue() + 16);
							pb.repaint();
						}
						else {
							pb.setValue(pb.getValue() + 2*s.getCell(j, i).getContents().length()+36);
							pb.repaint();
							sb.append(s.getCell(j, i).getContents()+"\t");
						}
						//if (j<s.getRows()-1) sb.append(s.getCell(j, i).getContents()+"\t");
					//else sb.append(s.getCell(j, i).getContents()+"\t");
				}
				sb.append("\n");
			}
			name.setText(url.getFile().substring(url.getFile().lastIndexOf('/')+1, url.getFile().lastIndexOf('.')));
			input.setText(sb.toString());
			wb.close();
			d.dispose();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	public void pasteFromClip(){
		type=-1;
		input.paste();
	}

	public Vector readVectors(File f){
		Vector v = new Vector();
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
			Object o;
			while ((o=in.readObject())!=null) v.add(o);
		} catch (Exception ex) { }
		return v;
	}

//	***** GMA 1.5.2: Modify to accept dataset selected
//	private void browseAvailableFiles() {
	private void browseAvailableFiles(String datasetToOpen, String datasetToOpenURL, int datasetToOpenType) {
//	***** GMA 1.5.2
  
//		***** GMA 1.5.2: Add to take already selected dataset
//		Show Tree Dialog and load selected file
//		Vector v = (new XMLJTreeDialog(DATABASE_XML_LISTING, this))
//	  	.getSelection().getProperties();

		System.out.println(datasetToOpen);
		System.out.println(datasetToOpenURL);
		System.out.println(datasetToOpenType);

		Vector v = new Vector();
		if ( datasetToOpen != null && !datasetToOpen.equals("") && datasetToOpenURL != null && !datasetToOpenURL.equals("") && datasetToOpenType > 0 ) {
			v = new Vector();
			v.add(new Object[] {"name",datasetToOpen});
			v.add(new Object[] {"url",datasetToOpenURL});
			v.add(new Object[] {"type",Integer.toString(datasetToOpenType)});
		}
		else {
			v = (new XMLJTreeDialog(this)).getSelection().getProperties();
		}
//		***** GMA 1.5.2

//		Vector v = XMLJTreeDialog.showXMLJTree(DATABASE_XML_LISTING, this);
		if (v.size()==0) return;

		int type = -1;
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//System.out.println("Hello from browse at beginning");
		try {
			Object obj = ParseLink.getProperty(v, "type");

			if (obj==null) {
				JOptionPane.showMessageDialog(this,
						"No type tag found in database entry; Failed to load",
						"Failed to load", JOptionPane.ERROR_MESSAGE);
				this.setCursor(Cursor.getDefaultCursor());
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
			this.setCursor(Cursor.getDefaultCursor());
			return;
		}

		String url = null;
		try {
			url = (String) ParseLink.getProperty(v, "url");
			if (url==null) {
				JOptionPane.showMessageDialog(this,
						"No url tag found in database entry; Failed to load",
						"Failed to load", JOptionPane.ERROR_MESSAGE);
				this.setCursor(Cursor.getDefaultCursor());
				return;
			} else {
				URLFactory.url(url);
			}
		} catch (IOException ex){
			JOptionPane.showMessageDialog(this,
					"Invalid URL protocol in database entry; Failed to load",
					"Failed to load", JOptionPane.ERROR_MESSAGE);
			this.setCursor(Cursor.getDefaultCursor());
			return;
		}

		if (type==UnknownDataSet.ASCII_URL) {
			loadURL(url);
		}
		else if (type==UnknownDataSet.EXCEL_URL) {
			loadExcelURL(url); 
		}
		String str = (String) ParseLink.getProperty(v, "name");
		str = str==null?"Untitled":str;
		name.setText(str);
		this.setCursor(Cursor.getDefaultCursor());
	}

	private void load(int x){
		DBDescription d; 
		if (x>=0&&x<10) d = (DBDescription) history.get(x);
		else if (x<0) d = (DBDescription) dataBases.get(x*-1-1);
		else d = (DBDescription) bookmarks.get(x-10);
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

//	***** Change by A.K.M. 06/30/06 *****
//	Custom Database Creation window now closes when the "X" icon on the window 
//	is clicked
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		dispose();
		input = null;
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {
		if ( selectedDataset != null && !selectedDataset.equals("") && selectedDatasetURL != null && !selectedDatasetURL.equals("") && selectedDatasetType > 0 ) {
			browseAvailableFiles(selectedDataset, selectedDatasetURL, selectedDatasetType);
		}
	}
//	***** Change by A.K.M. 06/30/06 *****
}
