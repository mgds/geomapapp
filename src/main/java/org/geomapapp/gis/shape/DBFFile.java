package org.geomapapp.gis.shape;

import haxby.util.URLFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.geomapapp.io.LittleIO;

public class DBFFile extends // org.geomappapp.db.util.GTable {
			javax.swing.table.AbstractTableModel {
	byte version;
	int nRecord;
	short recLength;
	int hLength;
	byte languageCode;
	Vector classes;
	Vector names;
	Vector lengths;
	Vector records;
	String path;
	String filename;
	DataInputStream input;
	public DBFFile( Vector names, Vector classes ) {
		this.names = names;
		this.classes = classes;
		records = new Vector();
	}
	public void addRecord( Vector record ) {
		records.add( record );
	}
	public DBFFile( String path, String fileprefix ) throws IOException {
		this.path = path;
		this.filename = fileprefix;
		init();
	}
	public DBFFile( InputStream in ) throws IOException {
		init(in);
	}
	public Vector getRecords() {
		return records;
	}
	public int[] sort(int column, boolean inverse) {
		int[] order = new int[records.size()];
		Vector tmp = new Vector(records.size());
		for( int k=0 ; k<order.length ; k++) {
			Vector v = new Vector(2);
			v.add(new Integer(k));
			v.add( ((Vector)records.get(k)).get(column) );
			tmp.add(v);
		}
		Collections.sort( tmp, new Comparator() {
			public int compare(Object o1, Object o2) {
				o1 = ((Vector)o1).get(1);
				o2 = ((Vector)o2).get(1);
				return ((Comparable)o1).compareTo((Comparable)o2);
			}
			public boolean equals(Object obj) {
				return obj==this;
			}
		});
		Vector newV = new Vector(order.length);
		for( int k=0 ; k<order.length ; k++) {
			Vector v = inverse ?
				(Vector)tmp.get(order.length-1-k)
				: (Vector)tmp.get(k);
			order[k] = ((Integer)v.get(0)).intValue();
			newV.add( records.get(order[k]) );
		}
		records = newV;
		return order;
	}
	void init() throws IOException {
		boolean url = path.startsWith( "http://" ) || path.startsWith("https://") || path.startsWith( "file://" );
		if( url&& !path.endsWith("/") ) path += "/";
		InputStream in = url ?
			(URLFactory.url( path+filename+".dbf")).openStream()
			: new FileInputStream( new File(path, filename+".dbf"));
		init(in);
	}
	void init( InputStream in ) throws IOException {
		input = new DataInputStream(
			new BufferedInputStream( in ));
		version = input.readByte();
		input.readByte();
		input.readByte();
		input.readByte();
		nRecord = LittleIO.readInt( input );
		hLength = LittleIO.readShort( input );
		recLength = LittleIO.readShort( input );
	//	System.out.println( nRecord +"\t"+ hLength +"\t"+ recLength);
		for( int k=12 ; k<32 ; k++) input.readByte();
		byte[] nm = new byte[11];
		int index = 32;
		names = new Vector();
		classes = new Vector();
		lengths = new Vector();
		int rLen = 1;
		while( rLen<recLength ) {
// System.out.println( rLen +"\t"+ recLength);
			input.readFully(nm);
			int offset=0;
			for( int j=0 ; j<nm.length ; j++) {
				if( nm[j]>32 )break;
				offset++;
			}
			int len=nm.length-offset;
			for( int j=nm.length-1 ; j>=offset ; j--) {
				if( nm[j]>32 )break;
				len--;
			}
			String name = new String(nm,offset,len);
		//	name.trim();
			names.add(name);
			byte[] tp = new byte[] { input.readByte() };
			String type = new String(tp);
			if( type.equalsIgnoreCase("C") ) classes.add( String.class );
			else if( type.equalsIgnoreCase("N") ) classes.add( Number.class );
			else if( type.equalsIgnoreCase("L") ) classes.add( Boolean.class );
			else classes.add( String.class );
			for( int k=0 ; k<4; k++) input.readByte();
			len = input.read();
			rLen += len;
			lengths.add( new Integer( len ));
			for( int k=17 ; k<32; k++) input.readByte();
			index += 32;
		//	System.out.println( name +"\n\t"+ type +"\t"+ len);
		}
		while(index<hLength) {
			input.read();
			index++;
		}
		records = new Vector();
		for( int k=0 ; k<nRecord ; k++) {
			input.read();
			StringBuffer sb = new StringBuffer();
			sb.append( (k+1)+"\t");
			Vector record = new Vector();
			for( int i=0 ; i<names.size() ; i++) {
//	System.out.println( ((Integer)lengths.get(i)).intValue() );
				nm = new byte[((Integer)lengths.get(i)).intValue()];
				input.readFully(nm);
				int offset=0;
				for( int j=0 ; j<nm.length ; j++) {
					if( nm[j]>32 )break;
					offset++;
				}
				int len=nm.length-offset;
				for( int j=nm.length-1 ; j>=offset ; j--) {
					if( nm[j]>32 )break;
					len--;
				}
				String val = new String(nm,offset,len);
				val.trim();
//	System.out.println( len +"\t"+ val.length() +"\t"+ nm[0] +"\t"+ nm[len-1]);
				sb.append( val+"\t");
				if( classes.get(i)==String.class ) record.add(val);
				else if( classes.get(i)==Number.class ) {
					try {
						record.add(new Double(val));
					} catch(NumberFormatException ex) {
						record.add(new Double(Double.NaN));
					}
				} else if( classes.get(i)==Boolean.class ) {
					val = val.toLowerCase();
					boolean tf = val.startsWith("t") || val.startsWith("y");
					record.add(new Boolean(tf));
				} else record.add(val);
			}
			records.add( record);
			// System.out.println( sb);
		}
		input.close();
	}

// TableModel methods
	public int getRowCount() {
		return records.size();
	}
	public int getColumnCount() {
		return names.size();
	}
	public Object getValueAt(int row, int column) {
		Vector r = (Vector)records.get(row);
		return r.get(column);
	}
	public Class getColumnClass(int columnIndex) {
		return (Class) classes.get(columnIndex);
	}
	public String getColumnName(int column) {
		return (String)names.get(column);
	}
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	public void setValueAt(Object aValue,
			int rowIndex,
			int columnIndex) {
		Vector row = (Vector)records.get(rowIndex);
		Class c = (Class)classes.get(columnIndex);
		if( aValue.getClass() != c ) {
			String val = aValue.toString();
			try {
				if( c==Number.class ) row.setElementAt(
						new Double(Double.parseDouble(val)), columnIndex);
				else if( c==String.class ) row.setElementAt(val, columnIndex);
				else return;
			} catch(Exception e) {
			}
		} else {
			row.setElementAt(aValue, columnIndex);
		}
	}
	public void write(String path, String name) throws IOException {
		File file = new File(path, name+".dbf");
	//	if( !file.canWrite() )throw new IOException("File is Write Protected :\n\t"+file.getPath());
		RandomAccessFile dbf = new RandomAccessFile( file, "rw");
		dbf.writeByte(3);
		Calendar cal = Calendar.getInstance();
		int year = cal.get(cal.YEAR)%100;
		int mon = cal.get(cal.MONTH)+1;
		int day = cal.get(cal.DAY_OF_MONTH);
		dbf.writeByte( year );
		dbf.writeByte( mon );
				dbf.writeByte( day );

		LittleIO.writeInt( records.size(), dbf );

		int nField = names.size();
		LittleIO.writeShort( (short)(33+32*nField) , dbf);
		int[] maxChars = new int[nField];
		for( int i=0 ; i<nField ; i++) maxChars[i]=0;
		for( int k=0 ; k<records.size() ; k++) {
			Vector r = (Vector)records.get(k);
			for( int i=0 ; i<nField ; i++) {
				if( r.get(i)==null)continue;
				int len = r.get(i).toString().length();
				if( len>maxChars[i] )maxChars[i] = len;
			}
		}
		int maxC = 1;
		for( int i=0 ; i<nField ; i++) maxC+=maxChars[i];
		LittleIO.writeShort( (short)(maxC), dbf );
StringBuffer sb = new StringBuffer( ""+maxC);
		for( int i=0 ; i<nField ; i++)sb.append("\t"+ maxChars[i]);
// System.out.println(sb);
		dbf.writeShort( 0 );	// Reserved
				dbf.write( 0 );		 // Incomplete transaction
				dbf.write( 0 );		 // Encryption flag
				dbf.writeInt( 0 );	  // Free record thread
				dbf.writeLong(0L);	  // Reserved
				dbf.write( 0 );		 // MDX flag
				dbf.write( 1 );		 // Language driver
				dbf.writeShort( 0 );	// Reserved

				for( int i=0 ; i<nField ; i++) {
					byte[] fieldName = new byte[11];
			byte[] b = names.get(i)==null ? new byte[0] : ((String)names.get(i)).getBytes();
			int n = b.length;
			if( n>10 )n=10;
			for( int j=0 ; j<n ; j++)fieldName[j]=b[j];
			dbf.write( fieldName );
			if(classes.get(i)==Number.class ) dbf.write( (byte)'N' );
			else if(classes.get(i)==Boolean.class ) dbf.write( (byte)'L' );
			else if(classes.get(i)==String.class ) dbf.write( (byte)'C' );
			dbf.writeInt( 0 );
			dbf.write( maxChars[i] );
			for( int k=17 ; k<32 ; k++) dbf.write( 0 );
		}
		dbf.write( 0x0D );

		for( int k=0 ; k<records.size() ; k++) {
// sb = new StringBuffer(""+k);
			Vector r = (Vector)records.get(k);
			dbf.write( 0x20 );
			for( int i=0 ; i<nField ; i++) {
// sb.append("\t|"+ (String)r.get(i)+"|");
				byte[] entry = new byte[maxChars[i]];
				byte[] chars = r.get(i)==null 
					? new byte[0]
				//	: ((String)r.get(i)).getBytes();
					: r.get(i).toString().getBytes();
				for( int j=0 ; j<chars.length ; j++)entry[j]=chars[j];
				dbf.write(entry);
			}
// System.out.println(sb);
		}
		dbf.write( 0x1a );
		dbf.close();
	}
	public static void main(String[] args) {
		javax.swing.JFileChooser c = new javax.swing.JFileChooser(
				System.getProperty( "user.dir" ));
		int ok = c.showOpenDialog(null);
		if( ok==c.CANCEL_OPTION) System.exit(0);
		File f = c.getSelectedFile();
		String path = f.getParent();
		String prefix = f.getName();
		prefix = prefix.substring(0, prefix.indexOf(".dbf"));
		try {
			DBFFile dbf = new DBFFile(path, prefix);
			JTable t = new JTable(dbf);
			JFrame frame = new JFrame(f.getName());
			frame.getContentPane().add(new JScrollPane(t));
			frame.pack();
			frame.show();
			frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		} catch(IOException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	//	System.exit(0);
	}
}
