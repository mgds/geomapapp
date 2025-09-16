package haxby.grid;

import haxby.util.XYZ;
import java.awt.image.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class GridImager {
	public static float[] defaultHT = {
			-7000.0f, -5500.0f, -4500.0f, -4000.0f, -3500.0f, -3250.0f, 
			-3000.0f, -2750.0f, -2500.0f, -2250.0f, -2000.0f, -1750.0f, 
			-1500.0f, -1000.0f, -500.0f, -100.0f, -1.0f, 0.0f, 
			 150.0f, 500.0f, 1000.0f, 2000.0f, 6000.0f};
	public static float[] defaultRED = {
			0.15686275f, 0.3137255f, 0.4117647f, 0.39215687f, 0.43137255f, 0.5882353f, 
			0.6666667f, 0.8235294f, 0.92156863f, 0.9411765f, 0.9411765f, 0.9411765f, 
			0.9411765f, 0.9411765f, 0.9019608f, 0.9411765f, 1.0f, 0.3529412f, 
			0.50980395f, 0.627451f, 0.8039216f, 0.88235295f, 0.9607843f};
	public static float[] defaultGREEN = {
			0.19607843f, 0.39215687f, 0.50980395f, 0.78431374f, 0.84313726f, 0.8627451f, 
			0.8627451f, 0.88235295f, 0.84313726f, 0.7647059f, 0.64705884f, 0.7254902f, 
			0.84313726f, 0.8627451f, 0.92156863f, 1.0f, 1.0f, 0.54901963f, 
			0.6862745f, 0.8039216f, 0.8039216f, 0.7137255f, 0.9607843f};
	public static float[] defaultBLUE = {
			0.7058824f, 0.78431374f, 0.88235295f, 0.9411765f, 0.9411765f, 0.84313726f, 
			0.7254902f, 0.6666667f, 0.5882353f, 0.5686275f, 0.49019608f, 0.64705884f, 
			0.78431374f, 0.78431374f, 0.9411765f, 1.0f, 1.0f, 0.39215687f, 
			0.39215687f, 0.39215687f, 0.54901963f, 0.39215687f, 0.9607843f};
	public static void listDefaultPallette() {
		for( int i=0 ; i<defaultHT.length ; i++) {
			int h = (int)Math.rint( (double)defaultHT[i] );
			int r = (int)Math.rint( 255.*(double)defaultRED[i] );
			int g = (int)Math.rint( 255.*(double)defaultGREEN[i] );
			int b = (int)Math.rint( 255.*(double)defaultBLUE[i] );
			System.out.println( h +"\t"+ r +"\t"+ g +"\t"+ b);
		}
	}
	public static void main(String[] args) {
		listDefaultPallette();
	}
	float[] ht, red, green, blue;
	int background = 0xff646464;
//	int background = 0;
	int nht;
	double ve, unitsPerNode;
	XYZ sun;
	double gamma;
	int[] rgbmap = new int[1001];
	int minRGB, maxRGB;
	float[] range;
	float hScale;
	public GridImager() {
		this(1d, 1000d, new XYZ(-1,1,1));
	}
	public GridImager( double ve, double unitsPerNode, 
			XYZ toSun) {
		ht = defaultHT;
		red = defaultRED;
		green = defaultGREEN;
		blue = defaultBLUE;
		nht = ht.length;
		sun = toSun;
		sun.normalize();
		this.ve = ve;
		this.unitsPerNode = unitsPerNode;
		minRGB = 50;
		maxRGB = 256;
		setGamma(1d);
		range = new float[2];
		range[0] = ht[0];
		range[1] = ht[ht.length-1];
		hScale = 1f;
	}
	public GridImager(String colorFile, double ve, double unitsPerNode, 
			XYZ toSun) throws IOException {
		FileReader fin = new FileReader(colorFile);
		BufferedReader in = new BufferedReader(fin);
		String s;
		StringTokenizer st;
		Vector tmp = new Vector();
		while( (s = in.readLine()) != null ) {
			st = new StringTokenizer(s);
			if(st.countTokens()<4)continue;
			float[] hrgb = new float[4];
			for( int i=0 ; i<4 ; i++) {
				hrgb[i] = (float)Integer.parseInt(st.nextToken());
			}
			tmp.add(hrgb);
		}
		fin.close();
		nht = tmp.size();
		ht = new float[nht];
		red = new float[nht];
		green = new float[nht];
		blue = new float[nht];
		for( int i=0 ; i<nht ; i++) {
			float[] hrgb = (float[])tmp.get(i);
			ht[i] = hrgb[0];
			red[i] = hrgb[1]/255f;
			green[i] = hrgb[2]/255f;
			blue[i] = hrgb[3]/255f;
		}
		sun = toSun;
		sun.normalize();
		this.ve = ve;
		this.unitsPerNode = unitsPerNode;
		minRGB = 50;
		maxRGB = 256;
		setGamma(1d);
		range = new float[2];
		range[0] = ht[0];
		range[1] = ht[ht.length-1];
		hScale = 1f;
	}
	public void saturate( float factor ) {
		if( factor<0f )return;
		if(factor>1f ) factor=1f;
		for( int i=0 ; i<red.length ; i++) {
			float max = red[i];
			if( green[i]>max) max=green[i];
			if( blue[i]>max) max=blue[i];
			float min = red[i];
			if( green[i]<min) min=green[i];
			if( blue[i]<min) min=blue[i];
			if( max==min) continue;
			float newMin =(1-factor)*min;
			red[i] = max - (max-red[i])*(max-newMin)/(max-min);
			green[i] = max - (max-green[i])*(max-newMin)/(max-min);
			blue[i] = max - (max-blue[i])*(max-newMin)/(max-min);
		}
	}
	public void setBackground( int rgb ) {
		background = rgb;
	}
	public XYZ getSun() {
		return sun;
	}
	public void setSun( XYZ sun ) {
		sun.normalize();
		this.sun = sun;
	}
	public float[] getRange() {
		float[] tmp = new float[] {range[0], range[1]};
		return tmp;
	}
	public void setRange( float min, float max ) {
		range[0] = min;
		range[1] = max;
		hScale = (ht[ht.length-1] - ht[0]) / (range[1]-range[0] );
	}
	public void setUnitsPerNode(double scale) {
		unitsPerNode = scale;
	}
	public void setVE(double ve) {
		this.ve = ve;
	}
	public double getVE() {
		return ve;
	}
	public double getGamma() {
		return gamma;
	}
	public void setGamma(double g) {
		gamma = g;
		rgbmap[0] = 0;
		double scale = (double)(maxRGB-minRGB);
		for(int i=1 ; i<=1000 ; i++) {
			rgbmap[i] = minRGB + (int)Math.floor(scale*Math.pow( .001*(double)i, 1/gamma));
			if(rgbmap[i]>255) rgbmap[i]=255;
			if(rgbmap[i]<minRGB) rgbmap[i]=minRGB;
		}
	}
	public BufferedImage gridImage(short[] h, int width, int height) {
		BufferedImage image;
		if( (background & 0xff000000) == 0xff000000) {
			image = new BufferedImage(width-1, height-1, BufferedImage.TYPE_INT_RGB);
		} else {
			image = new BufferedImage(width-1, height-1, BufferedImage.TYPE_INT_ARGB);
		}
	//	Graphics2D g2 = image.createGraphics();
	//	g2.setColor(new Color(150,150,150));
	//	g2.fillRect(0,0,width,height);
		for(int y=0 ; y<height-1 ; y++) {
			for(int x=0 ; x<width-1 ; x++) {
				image.setRGB( x, y, background );
			}
		}
		return gridImage( h, image);
	}
	public BufferedImage gridImage(short[] h, BufferedImage image) {
		int width = image.getWidth()+1;
		int height = image.getHeight()+1;
		int iy, k;
		float z, nz, dx, dy;
		XYZ grad = new XYZ();;
		int size = h.length;
		for(int y=0 ; y<height-1 ; y++) {
			iy = y*width;
			for(int x=0 ; x<width-1 ; x++) {
				k = x+iy;
				z=0;
				nz=0;
				if(k<size && h[k]!=-32668) {
					z += (float)h[k];
					nz++;
				}
				if(k+1<size && h[k+1]!=-32668) {
					z += (float)h[k+1];
					nz++;
				}
				if(k+width<size && h[k+width]!=-32668) {
					z += (float)h[k+width];
					nz++;
				}
				if(k+width+1<size && h[k+width+1]!=-32668) {
					z += (float)h[k+width+1];
					nz++;
				}
				if(nz<3) {
				//	image.setRGB(x, y, background);
					continue;
				}
				z /= nz;
				dx = 0;
				nz = 0;
				if(h[k]!=32768 && h[k+1]!=32768) {
					dx += (float)h[k+1] - (float)h[k];
					nz++;
				}
				if(h[k+width]!=32768 && h[k+width+1]!=32768) {
					dx += (float)h[k+width+1] - (float)h[k+width];
					nz++;
				}
				dx /= nz;
				dy = 0;
				nz = 0;
				if(h[k]!=32768 && h[k+width]!=32768) {
					dy += (float)h[k] - (float)h[k+width];
					nz++;
				}
				if(h[k+1]!=32768 && h[k+width+1]!=32768) {
					dy += (float)h[k+1] - (float)h[k+width+1];
					nz++;
				}
				dy /= nz;
				grad.x = -dx*ve/unitsPerNode;
				grad.y = -dy*ve/unitsPerNode;
				grad.z = 1;
				try {
					image.setRGB(x, y, getRGB(z, grad));
				} catch( Exception ex) {
					System.out.println( z +"\t"+ grad.x +"\t"+ grad.y
							+"\t"+ h[k] +"\t"+ h[k+1] +"\t"+ h[k+width] +"\t"+ h[k+width+1]);
				}
			}
		}
		return image;
	}
	public BufferedImage gridImage(float[] h, int width, int height) {
		BufferedImage image;
		if( (background & 0xff000000) == 0xff000000) {
			image = new BufferedImage(width-1, height-1, BufferedImage.TYPE_INT_RGB);
		} else {
			image = new BufferedImage(width-1, height-1, BufferedImage.TYPE_INT_ARGB);
		}
	//	Graphics2D g2 = image.createGraphics();
	//	g2.setColor(new Color(150,150,150));
	//	g2.fillRect(0,0,width,height);
		for(int y=0 ; y<height-1 ; y++) {
			for(int x=0 ; x<width-1 ; x++) {
				image.setRGB( x, y, background );
			}
		}
		return gridImage( h, image);
	}
	public BufferedImage gridImage(float[] h, BufferedImage image) {
		int width = image.getWidth()+1;
		int height = image.getHeight()+1;
		int iy, k;
		float z, nz, dx, dy;
		XYZ grad = new XYZ();;
		int size = h.length;
		for(int y=0 ; y<height-1 ; y++) {
			iy = y*width;
			for(int x=0 ; x<width-1 ; x++) {
				k = x+iy;
				z=0;
				nz=0;
				if(k<size && !Float.isNaN(h[k])) {
					z += h[k];
					nz++;
				}
				if(k+1<size && !Float.isNaN(h[k+1])) {
					z += h[k+1];
					nz++;
				}
				if(k+width<size && !Float.isNaN(h[k+width])) {
					z += h[k+width];
					nz++;
				}
				if(k+width+1<size && !Float.isNaN(h[k+width+1])) {
					z += h[k+width+1];
					nz++;
				}
				if(nz<3) {
				//	image.setRGB(x, y, background);
					continue;
				}
				z /= nz;
				dx = 0;
				nz = 0;
				if(!Float.isNaN(h[k]) && !Float.isNaN(h[k+1])) {
					dx += h[k+1] - h[k];
					nz++;
				}
				if(!Float.isNaN(h[k+width]) && !Float.isNaN(h[k+width+1])) {
					dx += h[k+width+1] - h[k+width];
					nz++;
				}
				dx /= nz;
				dy = 0;
				nz = 0;
				if(!Float.isNaN(h[k]) && !Float.isNaN(h[k+width])) {
					dy += h[k] - h[k+width];
					nz++;
				}
				if(!Float.isNaN(h[k+1]) && !Float.isNaN(h[k+width+1])) {
					dy += h[k+1] - h[k+width+1];
					nz++;
				}
				dy /= nz;
				grad.x = -dx*ve/unitsPerNode;
				grad.y = -dy*ve/unitsPerNode;
				grad.z = 1;
				try {
					image.setRGB(x, y, getRGB(z, grad));
				} catch( Exception ex) {
					System.out.println( z +"\t"+ grad.x +"\t"+ grad.y
							+"\t"+ h[k] +"\t"+ h[k+1] +"\t"+ h[k+width] +"\t"+ h[k+width+1]);
				}
			}
		}
		return image;
	}
	public BufferedImage grayImage(float[] h, BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int iy, k;
		float z, nz, dx, dy;
		XYZ grad = new XYZ();;
		int size = h.length;
		for(int y=0 ; y<height ; y++) {
			iy = y*width;
			for(int x=0 ; x<width ; x++) {
				k = x+iy;
				z=0;
				nz=0;
				if(Float.isNaN(h[k])) continue;
				image.setRGB(x, y, getRGB(h[k]));
			}
		}
		return image;
	}
	public float[] getColor(float h) {
	//	hScale = (ht[ht.length-1] - ht[0]) / (range[1]-range[0] );
		float z = ht[0] + (h-range[0]) * hScale;
//	System.out.println( h +"\t"+ z +"\t"+ ht[0] +"\t"+ ht[nht-1] +"\t"+ range[0] +"\t"+ range[1]);
		if(z<=ht[0]) return new float[] {red[0], green[0], blue[0] };
		if(z>=ht[nht-1]) return new float[] {red[nht-1], green[nht-1], blue[nht-1] };
		int i=(nht-1)/2;
		int k1 = 0;
		int k2 = nht-1;
		while(true) {
			if(z>ht[i+1]) {
				k1=i;
				i=(i+k2)/2;
			} else if(z<ht[i]) {
				k2 = i;
				i=(i+k1)/2;
			} else break;
		}
		float dx = (z-ht[i]) / (ht[i+1]-ht[i]);
		float[] rgb = new float[] {red[i]+dx*(red[i+1]-red[i]),
					green[i] + dx * (green[i+1]-green[i]),
					blue[i] + dx * (blue[i+1]-blue[i]) };
		return rgb;
	}
	public int getRGB(float z) {
		float[] col = getColor(z);
		int rgb = (new Color( col[0], col[1], col[2] )).getRGB();
		return rgb;
	}
	public int getRGB(float z, XYZ grad) {
		grad.normalize();
		float shade = (float)sun.dot(grad);
		return getRGB( z, shade );
	}
	public int getRGB(float z, float shade ) {
		if(shade<.1f)shade=.1f;
		float[] rgb = getColor(z);
		float s1 = (shade>.5f)? (shade-.5f)*(shade-.5f)/(.5f*.5f) : 0f;
		for(int i=0 ; i<3 ; i++) rgb[i] = shade*rgb[i]+s1*(1f-rgb[i]);
/*
		if( shade>.5f) {
			shade = (1f-shade)/.5f;
			int r = (int)(255.f*(shade*rgb[0]+(1f-shade)));
			if(r>255)r=255;
			int g = (int)(255.f*(shade*rgb[1]+(1f-shade)));
			if(g>255)g=255;
			int b = (int)(255.f*(shade*rgb[2]+(1f-shade)));
			if(b>255)b=255;
			return 0xff000000 | r<<16 | g<<8 | b;
		}
		shade = 1000f*shade/.5f;
*/
	//	shade = 1000f*shade;
	//	if(shade<100f)shade=100f;
	//	int c = 0xff000000 | rgbmap[(int)(rgb[0]*shade)]<<16
	//			| rgbmap[(int)(rgb[1]*shade)]<<8
	//			| rgbmap[(int)(rgb[2]*shade)];
		try {
			int c = 0xff000000 | rgbmap[(int)(rgb[0]*1000f)]<<16
					| rgbmap[(int)(rgb[1]*1000f)]<<8
					| rgbmap[(int)(rgb[2]*1000f)];
			return c;
		} catch(Exception e) {
			return 0;
		}
	}
	public static BufferedImage getImage(float[] z, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
		g2.setColor(new Color(150,150,150));
		g2.fillRect(0,0,width,height);
		int i;
		int n = 0;
		float min, max, val;
		min = 0;
		max = 0;
		i=0;
		float sumx2 = 0;
		int size = z.length;
		for(int y=0 ; y<height ; y++) {
			for( int x=0 ; x<width ; x++, i++) {
				if( i>=size || Float.isNaN(z[i])) continue;
				n++;
				val = z[i];
				sumx2 += val*val;
				if(n==1) {
					min = val;
					max = val;
				} else {
					if(val > max) max=val;
					else if(val < min) min=val;
				}
			}
		}
		if(n==0) return image;
		float rms = (float)Math.sqrt( (double)sumx2 / (double)n );
		min = -3*rms;
		min = 0;
		max = 3*rms;
		if(max==min) min-=1;
		float scale = 1f / (max-min);
		i=0;
		int color;
		for(int y=0 ; y<height ; y++) {
			for( int x=0 ; x<width ; x++, i++) {
				if( i>=size || Float.isNaN(z[i])) continue;
				val = .01f + (z[i] - min) * scale;
				if(val<.01f) val=.01f;
				if(val>1f) val=1f;
				val = (float)Math.sqrt((double)val);
				color = (new Color(val,val,val)).getRGB();
				image.setRGB(x, y, color);
			}
		}
		return image;
	}
}