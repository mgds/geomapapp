package org.geomapapp.gis.table;

import haxby.db.custom.HyperlinkTableRenderer;
import haxby.util.URLFactory;
import haxby.util.XBTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class TableDB extends javax.swing.table.AbstractTableModel {
	protected JFileChooser chooser;
	protected StringBuffer comments;
	protected Vector<Vector> rows;
	protected Vector headings;
	protected Vector<Integer> currentRowsIndices = new Vector<Integer>();
	protected Vector<Vector> allRows;
	protected Class[] classes;
	protected XBTable table;
	protected String delim;
	protected MouseAdapter sorter;
	protected Compare compare;
	public int latCol;
	public int lonCol;
	protected JList tagList;
	protected JList tagChoice;
	protected Vector allTags;
	protected Vector chosenTags;
	Vector columnOrder;
	JDialog tagDialog;
	File file;
	JPopupMenu popup;
	TableDB parent;
	public int selectedColumn = -1;
	int selectedRow = -1;
	boolean editable;
	JLabel info;
	String search;
	public TableDB( Vector headings, Vector rows, TableDB parent) {
		comments = parent.comments;
		this.headings = headings;
		this.rows = rows;
		this.parent = parent;
		classes = parent.classes;
		latCol = parent.latCol;
		lonCol = parent.lonCol;
		columnOrder = new Vector();
		for( int k=0 ; k<headings.size() ; k++) columnOrder.add(Integer.valueOf(k));
	}
	public TableDB( Vector headings, Vector rows, StringBuffer comments) {
		this.comments = comments;
		this.headings = headings;
		this.rows = rows;
		resolveLonLat();
		resolveClasses();
		columnOrder = new Vector();
		for( int k=0 ; k<headings.size() ; k++) columnOrder.add(Integer.valueOf(k));
	}
	public TableDB(String url) throws IOException {
		this(url, "\t");
	}
	public TableDB(String url, String delim) throws IOException {
		this.delim = delim;
		read(url);
	}
	public TableDB(File file) throws IOException {
		this(file, "\t");
	}
	public TableDB(File file, String delim) throws IOException {
		this.delim = delim;
		this.file = file;
		read(file);
	}
	public TableDB( InputStream in ) throws IOException {
		delim = "\t";
		read(in);
	}
	public TableDB() throws IOException {
		delim = "\t";
		paste();
	}
	void read( InputStream in ) throws IOException {
		read( new BufferedReader( new InputStreamReader(in) ));
	}
	void read( BufferedReader in ) throws IOException {
		
//		Read in DSDP column headers
		String s = in.readLine();
		
		comments = new StringBuffer();
		while( s.startsWith("#")) {
			comments.append( s +"\n");
			s = in.readLine();
		}
		if(comments.length()>0) {
			JOptionPane.showMessageDialog(haxby.map.MapApp.anchor,
				new JScrollPane(new JTextArea(comments.toString())),
				"Comments",
				JOptionPane.INFORMATION_MESSAGE);
		}
		while( s!=null && s.trim().length()==0 )	{
			s = in.readLine();
		}
		if(s==null) throw new IOException("no data in file");
		headings = parseRow(s);
		columnOrder = new Vector();
		for( int k=0 ; k<headings.size() ; k++) columnOrder.add(Integer.valueOf(k));

		rows = new Vector();
		
//		This loop reads in DSDP data
		while( (s=in.readLine())!=null ) {
			if( s.trim().length()==0 )continue;
			if(s.startsWith("#"))continue;
			rows.add( parseRow(s) );
		}
		
//	System.out.println( rows.size());
		resolveLonLat();
		resolveClasses();
	}
	protected void resolveLonLat() {
		String s;
		lonCol=-1;
		latCol=-1;
		for( int k=0 ; k<headings.size() ; k++) {
			String tag="";
			try {
				tag = (String)headings.get(k);
			} catch(Exception e) {
				continue;
			}
			if( latCol==-1 && tag.toLowerCase().startsWith("lat") )latCol=k;
			if( lonCol==-1 && tag.toLowerCase().startsWith("lon") )lonCol=k;
		}
	//	System.out.println( "lat,lon\t"+ latCol +"\t"+ lonCol);
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			for( int i=0 ; i<row.size() ; i++) {
				if( i==latCol || i==lonCol ) {
					s = (String)row.get(i);
					if( s!=null ) {
						double lat = ParseLatLon.parse( s );
						row.setElementAt( new Double(lat), i);
					}
				}
			}
		}
	}
	protected void resolveClasses() {
		String s;
		classes = ColClass.getColumnClasses(rows, headings.size());
	//	for( int k=0 ; k<classes.length ; k++)
	//		System.out.println( k +"\t"+ headings.get(k) +"\t"+ classes[k]);
		allRows = rows;
		currentRowsIndices.clear();
		for (int i = 0 ; i < allRows.size(); i++)
			currentRowsIndices.add(i);
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			for( int i=0 ; i<classes.length ; i++) {
				if( i>=row.size() )continue;
				if( classes[i]==String.class )continue;
				if( i==latCol || i==lonCol ) continue;
				s = (String)row.get(i);
				if( s==null )continue;
				try {
					if( classes[i]==Color.class ) {
						StringTokenizer st = new StringTokenizer(s,",");
						row.setElementAt( new Color(Integer.parseInt(st.nextToken()),
									Integer.parseInt(st.nextToken()),
									Integer.parseInt(st.nextToken())), i);
					}
					else if( classes[i]==Byte.class ) row.setElementAt( new Byte((byte)Double.parseDouble(s)), i);
					else if( classes[i]==Short.class ) row.setElementAt( new Short((short)Double.parseDouble(s)), i);
					else if( classes[i]==Integer.class ) row.setElementAt( Integer.valueOf(Integer.parseInt(s)), i);
					else if( classes[i]==Double.class ) row.setElementAt( new Double(Double.parseDouble(s)), i);
					else if( classes[i]==Boolean.class )
						row.setElementAt( new Boolean(ColClass.isTrue(s)), i );
				} catch(Exception e) {
					System.out.println( k +"\t"+ i +"\t"+ classes[i] +"\t"+ s );
					classes[i]=String.class;
					for( int kk=0 ; kk<k ; k++) {
						Vector r = (Vector)rows.get(kk);
						if( r.get(i)!=null )
							r.setElementAt( r.get(i).toString(), i);
					}
				}
			}
		}
	}
	public void checkComment( String s ) {
		if( s.indexOf("delim")<0 ) return;
		StringTokenizer st = new StringTokenizer(s.substring(1));
		if( st.countTokens()!=2 )return;
	}
	public Vector parseRow( String s ) {
		Vector row = new Vector();
		StringTokenizer st = new StringTokenizer(s,delim,true);
		while( st.hasMoreTokens() ) {
			s = st.nextToken();
			if( delim.indexOf(s)>=0 ) {
				row.add(null);
			} else {
				s = s.trim();
				if( s.length()==0 )row.add(null);
				else row.add( s );
				if(st.hasMoreTokens()) st.nextToken();
			}
		}
		return row;
	}
	void read( String urlString ) throws IOException {
		URL url = URLFactory.url(urlString);
		InputStream input = urlString.endsWith(".gz")
			? new GZIPInputStream( url.openStream() )
			: url.openStream();
			
		BufferedReader in = new BufferedReader(
			new InputStreamReader( input));
		read( in );
	}
	void read( File file ) throws IOException {
		BufferedReader in = new BufferedReader(
			!file.getName().endsWith(".gz") 
			? new FileReader( file )
			: new InputStreamReader(
				new GZIPInputStream(
				new FileInputStream( file )))
			);
		read( in );
	}
	void paste() throws IOException {
		Clipboard clip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clip.getContents(this);
		DataFlavor[] flavors = contents.getTransferDataFlavors();
		for( int k=0 ; k<flavors.length ; k++) {
			try {
				if( flavors[k].getHumanPresentableName().indexOf("html")>=0 )continue;
				BufferedReader in = new BufferedReader(
					flavors[k].getReaderForText(contents));
		System.out.println( flavors[k].getHumanPresentableName());
				read( in );
				break;
			} catch( UnsupportedFlavorException e) {
			}
		}
	}
/*
	void copy(String s) {
		Clipboard clip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(s);
		clip.setContents( ss, ss);
	}
*/
	public String getComments() {
		return comments.toString();
	}
	public void addComment(String comment) {
		comments.append( comment );
	}
	public Vector getData() {
		return rows;
	}
	public Vector getColumnOrder()	{
		return columnOrder;
	}
	void moveColumn( int from, int to) {
		if( from==to )return;
		Object o = columnOrder.remove(from);
		columnOrder.insertElementAt(o, to);
	//	System.out.println( table.getColumnName(to) +"\t"+ from +"\t"+ to +"\t"+ o.toString() );
	}
	
	protected Vector getCurrentRow(int i)
	{
		return allRows.get(currentRowsIndices.get(i));
	}
	
	public XBTable createTable() {
		if( table!=null ) return table;
		table = new XBTableExtension(this);
		table.getColumnModel().addColumnModelListener(
			new TableColumnModelListener() {
				public void columnAdded(TableColumnModelEvent e) {
				}
				public void columnRemoved(TableColumnModelEvent e) {
				}
				public void columnMoved(TableColumnModelEvent e) {
					moveColumn( e.getFromIndex(), e.getToIndex() );
				}
				public void columnMarginChanged(ChangeEvent e) {
				}
				public void columnSelectionChanged(ListSelectionEvent e) {
				}
			});
		sorter = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				selectedRow = table.rowAtPoint( e.getPoint());
				selectedColumn = table.columnAtPoint( e.getPoint());
				if( e.isPopupTrigger() ) popup.show(e.getComponent(), e.getX()+1, e.getY()+1);
			}
			public void mouseReleased(MouseEvent e) {
				if( e.isPopupTrigger() ) {
					mousePressed(e);
					return;
				}
			}
			public void mouseClicked(MouseEvent e) {
				if( e.isPopupTrigger() ) {
					mousePressed(e);
					return;
				}
				if( e.getButton()!= e.BUTTON1 )return;
			//	selectedColumn = table.columnAtPoint( e.getPoint());
				selectedRow = table.rowAtPoint( e.getPoint());
				selectedColumn = table.getColumnModel().getColumnIndexAtX( e.getPoint().x);
				
//				***** GMA 1.6.0: Select correct number of rows for sorted column
				
//				sort( selectedColumn, 
//						e.isShiftDown() );
				
				int [] temp = sort( selectedColumn, e.isShiftDown() );
				for ( int i = 0; i < temp.length; i++ ) {
					Object o = getCurrentRow(i).get(((Integer)columnOrder.get(selectedColumn)).intValue());
					if( o instanceof Boolean ) {
						if( !((Boolean)o).booleanValue() ) {
							if ( i > 0 ) {
								table.setRowSelectionInterval( 0, i - 1 );
							}
							else {
								table.clearSelection();
							}
							break;
						}
					}
				}

//				***** GMA 1.6.0				
				
			}
		};
		table.getColumnHeader().addMouseListener(sorter);
	//	table.setColumnSelectionAllowed(true);
		popup = new JPopupMenu();
		JMenuItem item = new JMenuItem("Save Selected Columns");
		popup.add( item );
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				saveColumns();
				popup.setVisible(false);
			}
		});
		item = popup.add( new JMenuItem("Selected Rows") );
		item.setEnabled(false);
		popup.addSeparator();
		item = popup.add("Select Tags, this column");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				selectTags();
				popup.setVisible(false);
			}
		});
		item = popup.add("reset");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				reset();
				popup.setVisible(false);
			}
		});
		table.addKeyListener( new KeyAdapter() {
			public void keyTyped(KeyEvent evt) {
				if( evt.getKeyChar()==KeyEvent.CHAR_UNDEFINED)return;
				addChar( evt.getKeyChar() );
			}
		});
		table.addMouseListener( new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				mouseClicked(e);
			}
			public void mousePressed(MouseEvent e) {
				mouseClicked(e);
			}
			public void mouseClicked(MouseEvent e) {
				selectedColumn = table.columnAtPoint( e.getPoint());
				selectedRow = table.rowAtPoint( e.getPoint());
				if( e.isPopupTrigger() ) popup.show(e.getComponent(), e.getX()+1, e.getY()+1);
			//	else if( popup.isVisible())popup.setVisible(false);
			}
		});
		return table;
	}
	void addChar(char c) {
		if( selectedRow<0 || selectedColumn<0 ) {
			search=" ";
			info.setText(selectedRow +", "+selectedColumn);
			return;
		}
		if(info==null)info=new JLabel(" ");
		int k=selectedRow-1;
		if( c!='\n' ) {
			if( !Character.isLetterOrDigit(c) )search=" ";
			else if( search==null|| search.equals(" ") ) search = new String(new char[] {c});
			else search += new String(new char[] {c});
		} else {
			if( search.equals(" "))return;
			k++;
		}
		info.setText(search);
		for( int i=0 ; i<currentRowsIndices.size() ; i++) {
			k = (k+1)%currentRowsIndices.size();
			Vector row = getCurrentRow(k);
			if( row.size()<selectedColumn+1 )continue;
			if( row.get(selectedColumn)==null )continue;
			if( row.get(selectedColumn).toString().toUpperCase().startsWith( search.toUpperCase() ) ) {
				selectedRow = k;
				table.setRowSelectionInterval( selectedRow, selectedRow );
				table.ensureIndexIsVisible( selectedRow );
				return;
			}
		}
		return;
	}
	public Vector getCurrentColumn(int col) {
		Vector column = new Vector(currentRowsIndices.size());
		for( int i=0 ; i<currentRowsIndices.size() ; i++) {
			Vector row = getCurrentRow(i);
			column.add( row.get(col).toString() );
		}
		return column;
	}
	protected void reset() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				rows = allRows;
				currentRowsIndices.clear();
				for (int i = 0 ; i < allRows.size(); i++)
					currentRowsIndices.add(i);
				fireTableDataChanged();
			}
		});
	}
	public void selectTags() {
		if( selectedColumn<0 )return;
		int col = ((Integer)columnOrder.get(selectedColumn)).intValue();
		selectedColumn = col;
		if( classes[col] != String.class ) return;
		initTagDialog();
		
		Vector currentRows = new Vector(currentRowsIndices.size());
		for (Integer i : currentRowsIndices)
			currentRows.add( allRows.get(i) );
		
		allTags = Tags.uniqueTags( currentRows, col, 500);
		chosenTags = new Vector();
		tagList.setListData(allTags);
		tagChoice.setListData(chosenTags);
		tagList.setListData( Tags.uniqueTags( currentRows, col, 500));
		tagDialog.setVisible(true);
	}
	protected void apply() {
		if( chosenTags!=null && chosenTags.size()==0 )return;
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				Vector<Integer> tagRows = new Vector<Integer>();
				int c = selectedColumn;
				for( int k=0 ; k<currentRowsIndices.size() ; k++) {
					Vector row = getCurrentRow(k);
					if( row.size()<c+1 ) continue;
					if( row.get(c)==null )continue;
					if( chosenTags.contains(row.get(c).toString()) ) tagRows.add(currentRowsIndices.get(k));
				}
				currentRowsIndices = tagRows;
				fireTableDataChanged();
			}
		});
	}
	void initTagDialog() {
		if( tagDialog!=null )return;
		
		JPanel panel = new JPanel(new BorderLayout());
		tagList = new JList();
		tagList.setVisibleRowCount(10);
		tagChoice = new JList();
		tagChoice.setVisibleRowCount(10);
		
		JPanel addRemove = new JPanel(new GridLayout(0,1,5,5));
		JButton add = new JButton(">> Add >>");
		addRemove.add(add);

		JButton remove = new JButton("<< Remove <<");
		addRemove.add(remove);

		add.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int[] names = tagList.getSelectedIndices();
				for( int i=names.length-1 ; i>=0 ; i--) {
					chosenTags.add((String)allTags.remove(names[i]));
				}
				tagList.setListData( allTags);
				tagChoice.setListData( chosenTags);
			}
		});
		remove.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int[] names = tagChoice.getSelectedIndices();
				for( int i=names.length-1 ; i>=0 ; i--) {
					allTags.add((String)chosenTags.remove(names[i]));
				}
				tagList.setListData( allTags);
				tagChoice.setListData( chosenTags);
			}
		});
		addRemove.setMinimumSize( addRemove.getPreferredSize());

		panel.add(new JScrollPane(tagList), "West");
		panel.add(addRemove, "Center");
		panel.add(new JScrollPane(tagChoice), "East");

		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				apply();
			}
		});
		panel.add( ok, "South");

		if( table.getTopLevelAncestor() instanceof JFrame ) {
			tagDialog = new JDialog( (JFrame)table.getTopLevelAncestor());
		} else if( table.getTopLevelAncestor() instanceof JDialog ) {
			tagDialog = new JDialog( (JDialog)table.getTopLevelAncestor());
		} else {
			tagDialog = new JDialog( (JDialog)null );
		}
		tagDialog.getContentPane().add(panel);
		tagDialog.pack();
	}
	public void saveColumns() {
		int[] cols = table.getSelectedColumns();
		if(cols.length==0)return;
		if( chooser==null ) {
			String dir = file==null
				? System.getProperty("user.dir")
				: file.getParent();
			chooser=new JFileChooser(dir);
		}
		int ok = chooser.showSaveDialog( table.getTopLevelAncestor() );
		if( ok==chooser.CANCEL_OPTION ) return;
		Arrays.sort(cols);
		int[] col1 = new int[cols.length];
		for( int k=0 ; k<cols.length ; k++) col1[k]=table.convertColumnIndexToView(cols[k]);
		cols = col1;
		try {
			file = chooser.getSelectedFile();
			PrintStream out = new PrintStream(
				new BufferedOutputStream(
				new FileOutputStream(file)));
			if(comments.length()!=0)out.println( comments );
			StringBuffer sb = new StringBuffer();
			Object o = headings.get(cols[0]);
			if( o!=null )sb.append( o );
			for( int k=1 ; k<cols.length ; k++) {
				o = headings.get(cols[k]);
				sb.append("\t");
				if(o!=null)sb.append( o );
			}
			out.println( sb.toString() );
			for( int i=0 ; i<currentRowsIndices.size() ; i++) {
				Vector row = getCurrentRow(i);
				sb = new StringBuffer();
				o = row.get(cols[0]);
				if( o!=null )sb.append( o );
				for( int k=1 ; k<cols.length ; k++) {
					o = row.get(cols[k]);
					sb.append("\t");
					if(o!=null)sb.append( o );
				}
				out.println( sb.toString() );
			}
			out.close();
		} catch(Exception e) {
		}
	}
	public int[] sort(int column,final boolean inverse) {
		column = ((Integer)columnOrder.get(column)).intValue();
//	System.out.println( classes[column].getName());
		if( compare==null ) compare = new Compare();
		compare.setInverse( inverse );
		final int[] order = new int[currentRowsIndices.size()];
		final Vector tmp = new Vector(currentRowsIndices.size());
		for( int k=0 ; k<order.length ; k++) {
			Vector v = new Vector(2);
			v.add(Integer.valueOf(k));
			if ( getCurrentRow(k).size()<column+1 ) {
				v.add(null);
			}
			else {
				v.add( getCurrentRow(k).get(column) );
			}
			tmp.add(v);
		}
		Collections.sort( tmp, compare );
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				Vector<Integer> newV = new Vector<Integer>(order.length);
				for( int k=0 ; k<order.length ; k++) {
					Vector v = inverse ?
							(Vector)tmp.get(order.length-1-k)
							: (Vector)tmp.get(k);
							order[k] = ((Integer)v.get(0)).intValue();
							newV.add( currentRowsIndices.get(order[k]) );
				}
				currentRowsIndices = newV;
			}
		});
		return order;
	}
// TableModel methods
	public int getRowCount() {
		return currentRowsIndices.size();
	}
	public int getColumnCount() {
		return headings.size();
	}
	public Object getValueAt(int row, int column) {
		Vector r = getCurrentRow(row);
		if( column>=r.size() ) return null;
		return r.get(column);
	}
	public Class getColumnClass(int columnIndex) {
		if( classes==null || columnIndex>=classes.length)return Object.class;
		return classes[columnIndex];
	}
	public String getColumnName(int column) {
		return (String)headings.get(column);
	}
	public boolean isCellEditable(int row, int col) {
		return editable;
	}
	public void setValueAt( Object value, int row, int col) {
		Vector r = getCurrentRow(row);
		r.setElementAt( value, col);
	}
	public void setEditable(boolean tf) {
		editable = tf;
	}
	class Compare implements Comparator {
		int inverse = 1;
		public Compare() {
		}
		public void setInverse( boolean tf ) {
			inverse = tf ? -1 : 1;
		}
		public int compare(Object o1, Object o2) {
			o1 = ((Vector)o1).get(1);
			o2 = ((Vector)o2).get(1);
			if( o1==null || o2==null) {
				int i=0;
				if( o1==null ) i+=inverse;
				if( o2==null ) i-=inverse;
				return i;
			}
			if( o1 instanceof Color ) return 0;
			if( o1 instanceof Boolean ) {
				int i=0;
				if( ((Boolean)o2).booleanValue() )i++;
				if( ((Boolean)o1).booleanValue() )i--;
				return i;
			}
			return ((Comparable)o1).compareTo((Comparable)o2);
		}
		public boolean equals(Object obj) {
			return obj==this;
		}
	}
	public JLabel getInfoLabel() {
		if(info==null) info = new JLabel(" ");
		return info;
	}

	private static final class XBTableExtension extends XBTable {
		TableCellRenderer renderer = new HyperlinkTableRenderer();

		private XBTableExtension(TableModel model) {
			super(model);
		}

		public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
			return renderer;
		}
	}

	public static void main(String[] args) {
		try {
			JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
			int ok = c.showOpenDialog(null);
			if( ok==c.CANCEL_OPTION )System.exit(0);
			TableDB t = new TableDB(c.getSelectedFile());
			XBTable table = t.createTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setScrollableTracksViewportWidth(false);
			JFrame f = new JFrame( c.getSelectedFile().getName() );
			f.getContentPane().add(new JScrollPane(table));
			f.getContentPane().add(t.getInfoLabel(), "South");
			f.pack();
			f.setVisible(true);
			f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}