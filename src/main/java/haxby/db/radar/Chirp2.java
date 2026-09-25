package haxby.db.radar;

import java.io.*;

import haxby.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class Chirp2 extends JComponent implements ActionListener, MouseMotionListener {
	int lineNumber;
	BufferedImage image=null;
	byte[] buf=null;
//	float[][][] traces;
//	short[][][] traces2;
	static int SAMPLES_PER_TRACE=-1;
	int[] bottom;
	int nTrace;
	int firstCDP, cdpInterval;
	int microsPerSample;
	int samplesPerTrace;
	int firstTimeSample;
	int tracesPerRecord;
	int traceNo;
	int nRecord;
	int format;
	int n2;
	boolean bandPass;
	double low1=0.;
	double low2=0.;
	double high1=0.;
	double high2=0.;
	int agcWin;
	int samplesBefore=0;
	int samplesAhead=100;
	double factor=1.;
	File file;
	JLabel xLabel;
	JLabel yLabel;
	JLabel zLabel;
	public Chirp2( File file ) throws IOException {
		xLabel = new JLabel("x");
		yLabel = new JLabel("y");
		zLabel = new JLabel("z");
		this.file = file;
		bandPass = false;
		agcWin = 100;
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream( file )));
		long size = file.length();
		in.skipBytes(3200);
		byte[] header = new byte[400];
		int n = in.read(header);
		while( n<400 ) n += in.read( header, n, 400-n);
		DataInputStream in1 = new DataInputStream(
				new ByteArrayInputStream( header ));
		in1.readInt();
		lineNumber = in1.readInt();
		in1.readInt();
		tracesPerRecord = (short)in1.readShort();
		if( tracesPerRecord<1 )tracesPerRecord=1;
		traceNo = 0;
		in1.readShort();
		microsPerSample = in1.readShort();
		in1.readShort();
		samplesPerTrace = in1.readShort();
		if( SAMPLES_PER_TRACE != -1 ) samplesPerTrace = SAMPLES_PER_TRACE;
		in1.readShort();
		format = in1.readShort();
		if( format==3) {
			nTrace = (int) ((size-3600L) / (long)(2*samplesPerTrace+240));
		} else {
			nTrace = (int) ((size-3600L) / (long)(4*samplesPerTrace+240));
		}
		nRecord = nTrace/tracesPerRecord;
		System.out.println( file.getName() +"\tline "+lineNumber+ ":\n\t"+ 
				tracesPerRecord +" traces per record\n\t"+
				nRecord +" records\n\t"+
				nTrace +"  traces\n\t"+
				"format = "+ format +"\n\t"+
				samplesPerTrace +" samples per trace\n\t"+
				microsPerSample +" microSecs per sample");
	//	if(nRecord>4000)nRecord=4000;
		nRecord &= 0xfffffff8;
		in.close();
		n2 = 2;
		while( n2<samplesPerTrace ) n2*=2;
	}
	public void mouseDragged( MouseEvent evt) {}
	public void mouseMoved( MouseEvent evt) {
		xLabel.setText("x= " + evt.getX() );
		yLabel.setText("y= " + evt.getY() );
		int k = evt.getX() + nRecord*evt.getY();
		zLabel.setText("z= " + buf[k]);
	}
	public void process() throws IOException {
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream( file )));
		in.skipBytes( 3600 );
		byte[] header = new byte[240];
		double[] filter = new double[n2];
		bottom = new int[nRecord];
		double time = 1.e-06*microsPerSample*n2;
		int k=1;
		if( bandPass ) {
			filter[0] = 0;
			while( k<low1 ) {
				filter[k]=0;
				filter[n2-k]=0;
				k++;
			}
			while( k<low2 ) {
				filter[k] =  Math.pow( Math.cos(.5*Math.PI*(low2-k)/(low2-low1)), 2);
				filter[n2-k]=filter[k];
				k++;
			}
			while( k<high1 ) {
				filter[k]=1;
				filter[n2-k]=1;
				k++;
			}
			while( k<high2 ) {
				filter[k] =  Math.pow( Math.cos(.5*Math.PI*(k-high1)/(high2-high1)), 2);
				filter[n2-k]=filter[k];
				k++;
			}
			while( k<=n2/2 ) {
				filter[k]=0;
				filter[n2-k]=0;
				k++;
			}
		}
		buf = new byte[nRecord*samplesPerTrace/4];
		int nRead;
		int recl = (format==3) ? 240+2*samplesPerTrace : 240+4*samplesPerTrace;
		int offset = -1;
		DataOutputStream out = new DataOutputStream(
				new FileOutputStream("headers"));
		double[] tmp = new double[samplesPerTrace];
		for(int i=0 ; i<nRecord ; i++) {
			if( traceNo != 0 ) {
				in.skipBytes( traceNo*recl );
			}
			nRead = in.read(header);
			while( nRead<240 ) nRead += in.read( header, nRead, 240-nRead);
			if( i==0 || i==nRecord-1 ) out.write( header );
			DataInputStream in1 = new DataInputStream(
					new ByteArrayInputStream( header ));
		//	in1.skipBytes( 116 );
		//	int off = in1.readShort();
		//	if(off!=offset) {
		//		System.out.println("sample rate = "+ off +"\trecord " + i);
		//	}
		//	offset=off;
			in1.skipBytes(30);
			byte[] check = new byte[6];
			in1.read( check );
			String unknown = new String(check);
			double[][] trace = new double[n2][2];
			bottom[i] = 0;
			for( k=0 ; k<samplesPerTrace ; k++) {
				if(format==1) {
					trace[k][0] = (double)IBM.IBMToFloat(in.readInt());
				} else if(format==2) {
					trace[k][0] = (double)in.readInt();
				} else if(format==3) {
					trace[k][0] = (double)in.readShort();
				} else {
					trace[k][0] = (double)in.readFloat();
				}
				trace[k][1] = 0.;
			}
			if( traceNo<tracesPerRecord-1 ) {
				in.skipBytes( (tracesPerRecord-1-traceNo)*recl );
			}
			trace[0][0] = 0.;
			if( bandPass ) {
				for( k=samplesPerTrace ; k<n2 ; k++ ) {
					trace[k][0] = 0.;
					trace[k][1] = 0.;
				}
				trace = FFT.fft_1d( trace );
				for( k=0 ; k<n2 ; k++) {
					trace[k][0] *= filter[k];
					trace[k][1] *= filter[k];
				}
				trace = FFT.ifft_1d( trace );
			}
	if( i%100 == 0) System.out.println("trace "+i +"\tunknown = "+ unknown);
			double sum = 0.;
			double nSum = 0;
			int win = agcWin/2;
			double max = 0.;
		//	for( k=0 ; k<samplesPerTrace-1 ; k++) {
		//		trace[k][0] = trace[k+1][0]-trace[k][0];
		//	}
			for( k=0 ; k<win ; k++) {
				sum += Math.abs(trace[k][0]);
				nSum++;
				if( trace[k][0]>max ) max=trace[k][0];
			}
			double diff = 0.;
			boolean found=true;
		//	boolean found=false;
			boolean searching = false;
			int nTest = 0;
			double lastDif=0.;
			for( k=0 ; k<samplesPerTrace-win ; k++) {
				if( k+win<samplesPerTrace-2  && trace[k+win][0]>max) {
					max = trace[k+win][0];
				} else if( k-win>0 && trace[k-win-1][0]>=max ) {
					max=0;
					int j2 = k+win;
					if(j2>samplesPerTrace-3) j2=samplesPerTrace-3;
					for(int j=k-win ; j< k+win ; j++) {
						if(trace[j][0]>max) max=trace[j][0];
					}
				}
				if( max==0.) {
					continue;
				}
				if( k-win>0 ) {
					sum -= Math.abs(trace[k-win-1][0]);
					nSum--;
				}
				if( k+win<samplesPerTrace ) {
					sum += Math.abs(trace[k+win][0]);
					nSum++;
				}
				int val = (int) ( 255. * .2 * trace[k][0]*nSum/sum);
				
				if(val<0)val=0;
				if(val>255)val=255;
				int index = (k/2)*(nRecord/2) + i/2;
				int v = 0x000000ff & (int)buf[index];
				v += val/4;
				if( v>255) v=255;
				buf[index] = (byte)v;

				if( found )continue;
				if( searching ) {
					if( trace[k][0]-trace[k-1][0] < lastDif ) {
						bottom[i] = k-1;
						searching = false;
						found = true;
						continue;
					} else {
						lastDif = trace[k][0]-trace[k-1][0];
					}
				} else {
					if( Math.abs(trace[k][0])/max < .1 ) {
						nTest++;
					} else if(nTest>win*3/4) {
						lastDif = trace[k][0]-trace[k-1][0];
						searching=true;
					} else {
						nTest=0;
					}
				}
			}
		//	System.out.println(i + "\t"+ bottom[i]);
		//	System.out.println(i +"\t"+ (diff/samplesPerTrace/win));
		}
/* bottom detect
		int[] b = new int[nRecord];
		int[] b1 = new int[21];
		int itmp;
		for( int i=0 ; i<nRecord ; i++) {
			int i1 = i-10;
			int i2 = i+10;
			if(i1<0) i1=0;
			if(i2>=nRecord) i2=nRecord-1;
			int n = i2-i1+1;
			k=0;
			for( int ii=i1 ; ii<=i2 ; ii++ ) {
				if( bottom[ii]==0 ) {
					n--;
					continue;
				}
				b1[k++]=bottom[ii];
			}
			if( n==0 ) {
				b[i] = 0;
				continue;
			}
			for( k=0 ; k<=n/2 ; k++) {
				for(int j=k+1 ; j<n ; j++) {
					if( b1[j]<b1[k] ) {
						itmp = b1[k];
						b1[k]=b1[j];
						b1[j]=itmp;
					}
				}
			}
			if( n%2==0 ) b[i] = (b1[n/2]+b1[n/2-1])/2;
			else b[i] = b1[n/2];
			for( int kk=0 ; kk<b[i]-20 ; kk++) buf[kk*nRecord+i]=0;
			buf[b[i]*nRecord+i] = (byte)255;
		}
*/
		in.close();
		out.close();
	//	DataBufferByte buff = new DataBufferByte(buf,buf.length);
	//	WritableRaster raster = Raster.createPackedRaster(buff,
	//		nRecord, samplesPerTrace, 8, new Point(0,0));
	//	image = new BufferedImage(grayLUT(false), 
	//		raster, false, new Hashtable());
	//	JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
	//	int ok = chooser.showSaveDialog( null );
	//	if( ok!=chooser.APPROVE_OPTION ) return;
		String name = file.getName();
		int indx = name.indexOf(".segy");
		if(indx==-1) indx = name.indexOf(".sgy");
		if(indx==-1) indx = name.indexOf(".stolt");
		name = name.substring(0, indx) + ".ras";
		int imH = samplesPerTrace/2;
		int imW = nRecord/2;
		try {
			out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(name)));
			out.writeInt(1504078485);
			out.writeInt(imW);
			out.writeInt(imH);
			out.writeInt(8);
			out.writeInt(imH*imW);
			out.writeInt(1);
			out.writeInt(1);
			out.writeInt(768);
			byte[] gray = new byte[256];
			for(int i=0 ; i<gray.length ; i++) gray[i]=(byte)(255-i);
			out.write(gray);
			out.write(gray);
			out.write(gray);
			for(int y=0 ; y<imH ; y++ ) {
				int yy = y*imW;
				for( int x=0 ; x<imW ; x++ ) {
					out.writeByte( buf[yy+x] );
				}
			}
			out.close();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
	public static IndexColorModel grayLUT(boolean reversed) {
		byte[] gray = new byte[256];
		if(reversed) {
			for( int i=0 ; i<256 ; i++) gray[i] = (byte)(i);
			for( int i=0 ; i<256 ; i++) {
				double g = (double) (((int)gray[i])&255);
				gray[i] = (byte)(int)( 255.98*Math.pow( (.01+g)/255.01d , .5d));
			}
		} else {
			for( int i=0 ; i<256 ; i++) gray[i] = (byte)(255-i);
		}
		byte[] gray1 = new byte[256];
		for(int i=0 ; i<255 ; i++) gray1[i] = gray[i];
		if( reversed ) {
			gray1[255] = 0;
			return new IndexColorModel(8, 256, gray, gray1, gray1);
		} else {
			gray1[255] = (byte)255;
			return new IndexColorModel(8, 256, gray1, gray, gray);
		}
	}
	public void setBandPass( int low, int lowWidth, int high, int highWidth ) {
		if( high <= 0 ) {
			bandPass=false;
			return;
		}
		double time = 1.e-06*microsPerSample*n2;
		low1 = time*(low-lowWidth);
		low2 = time*low;
		high1 = time*high;
		high2 = time*(high-highWidth);
		bandPass=true;
	}
	public void setTVG( int samplesBefore, int samplesAhead, double factor) {
		this.samplesBefore = samplesBefore;
		this.samplesAhead = samplesAhead;
		this.factor = factor;
	}
	public void setAGC( int window ) {
		agcWin = window;
	}
	public void actionPerformed(ActionEvent evt) {
	}
	public Dimension getPreferredSize() {
		return new Dimension( nRecord, samplesPerTrace );
	}
	public void paint( Graphics g ) {
		g.drawImage( image, 0, 0, this);
	}
	public void setTraceNumber( int trace ) {
		traceNo = trace;
		if( traceNo > tracesPerRecord-1 ) traceNo=tracesPerRecord-1;
	}
	public static void main(String[] args) {
		if(args.length != 1 && args.length != 2) {
			System.out.println("usage: java Chirp2 filename [samples_per_trace]");
			System.exit(0);
		}
		if( args.length==2 ) SAMPLES_PER_TRACE = Integer.parseInt( args[1] );
		try {
			Chirp2 segy = new Chirp2( new File(args[0]) );
			System.out.println( "Chirp2 Object created");
			int nyquist = 500000 / segy.microsPerSample;
			nyquist /=2;
			int highWidth = 1+nyquist/6;
			int high = nyquist-highWidth/3;
			int low = 1+high/8;
			int lowWidth = 1+highWidth/4;
		System.out.println( low +"\t"+ lowWidth +"\t"+ high +"\t"+ highWidth);
			segy.setBandPass( low, lowWidth, high, highWidth);
			segy.setTraceNumber(0);
			segy.setAGC(101);
		System.out.println( "processing" );
			segy.process();
/*
			JFrame frame = new JFrame(args[0]);
			frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
			frame.getContentPane().add( new JScrollPane(segy), "Center" );
			JPanel panel = new JPanel(new GridLayout(1, 0));
			panel.add(segy.xLabel);
			panel.add(segy.yLabel);
			panel.add(segy.zLabel);
			segy.addMouseMotionListener( segy );
			frame.getContentPane().add( panel, "North" );
			frame.pack();
			frame.setSize(1000, 800);
			frame.show();
*/
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}
