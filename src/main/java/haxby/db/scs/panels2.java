package haxby.db.scs;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.util.*;
import java.io.*;
import java.awt.image.*;
//import com.sun.image.codec.jpeg.*;

public class panels2 {
	BufferedImage image;
	public panels2( ) {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		int ok = chooser.showOpenDialog(null);
		if(ok==chooser.CANCEL_OPTION) System.exit(0);
		String dirName = chooser.getSelectedFile().getParent();
		try {
		if( dirName.equals(".") ) dirName = System.getProperty( "user.dir" );
		File dir = new File(dirName);
		File[] files = dir.listFiles();
		for( int k=0 ; k<files.length ; k++ ) {
			if( !files[k].getName().toLowerCase().endsWith(".jpg") )continue;
			StringTokenizer st = new StringTokenizer( files[k].getName(), "_.");
			st.nextToken();
			int page = Integer.parseInt( st.nextToken() );
			if( page%2 == 0 ) {
				page = page*2-2;
			} else {
				page = page*2-1;
			}
			System.out.println("decoding "+files[k].getName() );
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( files[k] ));
			//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
			//image = decoder.decodeAsBufferedImage();
			image = ImageIO.read(in);
			int h = image.getHeight()/4;
			int w = image.getWidth()/2;
			if( w%2==1 ) w--;
			if( image.getHeight()<2000 ) h=image.getHeight()/2;
			if( h%2==1 ) h--;
			String name = ( page<100 ? "0" : "" ) 
					+ ( page<10 ? "0" : "" ) + page;
			System.out.println("\t creating "+name+".ras" );
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(new File(dir,name+".ras"))));
			out.writeInt(1504078485);
			out.writeInt(w);
			out.writeInt(h);
			out.writeInt(8);
			out.writeInt(w*h);
			out.writeInt(1);
			out.writeInt(1);
			out.writeInt(768);
			byte[] gray = new byte[256];
			for(int i=0 ; i<gray.length ; i++) gray[i]=(byte)i;
			out.write(gray);
			out.write(gray);
			out.write(gray);
			for( int y=0 ; y<h ; y++) {
				for( int x=0 ; x<w ; x++) {
					int rgb = 3 + (0xff & image.getRGB(x*2, y*2)) 
						+ (0xff & image.getRGB(x*2+1, y*2))
						+ (0xff & image.getRGB(x*2, y*2+1))
						+ (0xff & image.getRGB(x*2+1, y*2+1));
					rgb /= 4;
					out.writeByte( rgb&0xff );
				}
			}
			out.close();
			if( image.getHeight()<2000 ) System.exit(0);
			int h1 = image.getHeight() - 2*h-2;
			page += 2;
			name = ( page<100 ? "0" : "" ) 
					+ ( page<10 ? "0" : "" ) + page;
			System.out.println("\t creating "+name+".ras" );
			out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(new File(dir,name+".ras"))));
			out.writeInt(1504078485);
			out.writeInt(w);
			out.writeInt(h);
			out.writeInt(8);
			out.writeInt(w*h);
			out.writeInt(1);
			out.writeInt(1);
			out.writeInt(768);
			out.write(gray);
			out.write(gray);
			out.write(gray);
			for( int y=0 ; y<h ; y++) {
				for( int x=0 ; x<w ; x++) {
					int rgb = 3 + (0xff & image.getRGB(x*2, h1+y*2)) 
						+ (0xff & image.getRGB(x*2+1, h1+y*2))
						+ (0xff & image.getRGB(x*2, h1+y*2+1))
						+ (0xff & image.getRGB(x*2+1, h1+y*2+1));
					rgb /= 4;
					out.writeByte( rgb&0xff );
				}
			}
			out.close();
		}
	} catch (IOException ex) {
		ex.printStackTrace();
		System.exit(0);
	}
	System.exit(0);
	}
	public static void main(String[] args) {
	//	if( args.length != 1) {
	//		System.out.println( "usage: java panels2 file_name");
	//		System.exit(0);
	//	}
		new panels2( );
	}
}