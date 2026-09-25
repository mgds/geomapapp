package haxby.db.scs;

import java.io.*;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.ImageIO;

//import com.sun.image.codec.jpeg.*;

public class SCSPanel2 {
	byte[] code;
	BufferedImage image;
	public SCSPanel2(  byte[] code ) {
		this.code = code;
		image = null;
	}
	void decode() throws IOException {
		if( image!=null ) return;
		ByteArrayInputStream in = new ByteArrayInputStream( code );
		//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder( in );
		//image = decoder.decodeAsBufferedImage();
		image = ImageIO.read(in);
	}
}