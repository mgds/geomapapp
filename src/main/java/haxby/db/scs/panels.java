package haxby.db.scs;

import java.util.*;
import java.io.*;
import java.awt.image.*;
//import com.sun.image.codec.jpeg.*;

import javax.imageio.ImageIO;

public class panels {
	BufferedImage image;
	public panels( String dirName ) {
		try {
		if( dirName.equals(".") ) dirName = System.getProperty( "user.dir" );
		File dir = new File(dirName);
		File[] files = dir.listFiles();
		for( int k=0 ; k<files.length ; k++ ) {
			if( !files[k].getName().trim().toLowerCase().endsWith(".jpg") )continue;
			StringTokenizer st = new StringTokenizer( files[k].getName(), "_.");
			st.nextToken();
			int page = Integer.parseInt( st.nextToken() );
			if( page%2 == 0 ) {
				page = page*2-2;
			} else {
				page = page*2-1;
			}
			String name = ( page<100 ? "0" : "" )
					+ ( page<10 ? "0" : "" ) + page;
			System.out.println("decoding "+files[k].getName() );
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( files[k] ));
			//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
			//image = decoder.decodeAsBufferedImage();
			image = ImageIO.read(in);
			int h = image.getHeight()/2;
			int w = image.getWidth();
			if( w%2==1 ) w--;
			if( h<1000 ) h=image.getHeight();
			System.out.println("\t creating "+ name +"/"+ name+".ras" );
			File outFile = new File(dir, name);
			outFile.mkdirs();
			outFile = new File(outFile, name+".ras");
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(outFile)));
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
					int rgb = image.getRGB(x, y);
					out.writeByte( rgb&0xff );
				}
			}
			out.close();
			if( h==image.getHeight() ) continue;
			int h1 = image.getHeight() - h;
			page += 2;
			name = ( page<100 ? "0" : "" )
					+ ( page<10 ? "0" : "" ) + page;
			System.out.println("\t creating "+ name +"/"+ name+".ras" );

			outFile = new File(dir, name);
			outFile.mkdirs();
			outFile = new File(outFile, name+".ras");
			out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(outFile)));

			out.writeInt(1504078485);
			out.writeInt(w);
			out.writeInt(h1);
			out.writeInt(8);
			out.writeInt(w*h1);
			out.writeInt(1);
			out.writeInt(1);
			out.writeInt(768);
			out.write(gray);
			out.write(gray);
			out.write(gray);
			for( int y=0 ; y<h1 ; y++) {
				for( int x=0 ; x<w ; x++) {
					int rgb = image.getRGB(x, y+h);
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
		if( args.length != 1) {
			System.out.println( "usage: java panels file_name");
			System.exit(0);
		}
		new panels( args[0] );
	}
}