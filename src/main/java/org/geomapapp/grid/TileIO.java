package org.geomapapp.grid;

import haxby.util.URLFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

import org.geomapapp.geom.MapProjection;

public abstract class TileIO {
	protected MapProjection proj;
	protected int gridSize;
	protected String root;
	protected boolean remote;
	protected boolean readonly;
	protected String separator;
	protected int nLevel;
	protected JLabel label;
	protected TileIO parent;
	DataInputStream in;
	protected TileIO(MapProjection proj, 
			String root,
			int gridSize) {
		this( proj, root, gridSize, 0);
		getNLevel();
	}
	protected TileIO(MapProjection proj, 
			String root,
			int gridSize,
			int nLevel) {
		this(proj, root, gridSize, nLevel, null);
	}
	protected TileIO(MapProjection proj, 
			String root,
			int gridSize,
			int nLevel,
			TileIO parent) {
		this.proj = proj;
		this.gridSize = gridSize;
		readonly = true;
		remote = root.toLowerCase().startsWith("http");
		if( root.toLowerCase().startsWith("file:") ) {
			root = (new File(root)).getPath();
			root = root.replace("file:", "");
			remote = false;
		}
		separator = remote ? "/" :
			System.getProperty("file.separator");
		if( !root.endsWith(separator) ) root += separator;
		this.root = root;
		this.nLevel = nLevel;
		this.parent = parent;
	}
	public void abort() {
		try {
			in.close();
		} catch(Exception ex) {
		}
	}
	public void setReadonly(boolean tf) {
		if(remote)return;
		readonly = tf;
	}
	public boolean isReadonly() {
		return readonly;
	}
	public void setLabel(JLabel label) {
		this.label = label;
	}
	public static int[] getIndices(String tileName) {
		StringTokenizer st = new StringTokenizer(tileName, "EWNS_", true);
		if( tileName.indexOf(".")>0 ) st = new StringTokenizer(tileName, "EWNS_.", true);
		try {
			boolean west = st.nextToken().equals("W");
			int E = Integer.parseInt( st.nextToken() );
			if( west ) E = -E;
			boolean south = st.nextToken().equals("S");
			int N = Integer.parseInt( st.nextToken() );
			if( south ) N = -N;
			return new int[] {E, N};
		} catch(Exception ex) {
			return null;
		}
	}
	public static String getName(int x, int y, int gridSize) {
		int xx = (int)Math.floor( (double)x/(double)gridSize );
		int yy = (int)Math.floor( (double)y/(double)gridSize );
		return ( (xx>=0) ? "E"+xx : "W"+(-xx) )
			+ ( (yy>=0) ? "N"+yy : "S"+(-yy) )
			+ "_" + gridSize;
	}
	public String getName(int x, int y) {
		return getName( x, y, gridSize);
	}
	public String getDirPath( int x, int y) {
		int xx = (int)Math.floor( (double)x/(double)gridSize );
		int yy = (int)Math.floor( (double)y/(double)gridSize );
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String path = root;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)xx / (double)factor);
			int yG = factor*(int)Math.floor( (double)yy / (double)factor);
			path += getName(gridSize*xG, gridSize*yG)+separator;
			factor /= 8;
		}
		return path;
	}
	public void setNLevel(int n) {
		nLevel = n;
	}
	void getNLevel() {
		try {
			BufferedReader in = new BufferedReader(
				remote ?
				  new InputStreamReader(
					(URLFactory.url(root+"n_level")).openStream())
				: new FileReader(root+"n_level") );
			nLevel = Integer.parseInt( in.readLine() );
			in.close();
		} catch (IOException ex) {
			nLevel=0;
		}
	}
	public abstract Grid2D readGridTile(int x, int y)
					throws IOException;

	public abstract Grid2D createGridTile(int x, int y);

	public abstract void writeGridTile(Grid2D tile)
					throws IOException;

	public static class Float extends TileIO {
		public Float(MapProjection proj, 
				String root, 
				int gridSize) {
			super( proj, root, gridSize);
		}
		public Float(MapProjection proj, 
				String root, 
				int gridSize,
				int nLevel) {
			super( proj, root, gridSize, nLevel);
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			String path = getDirPath( x, y);
			path += getName(x, y)+".zgrid";
			if( label!=null) label.setText( "reading "+path);
//			System.out.println(path);

			in =null;
			if(remote) {
				URL url = URLFactory.url(path);
				in = new DataInputStream(
					url.openStream());
			} else {
				File file = new File(path);
				if( !file.exists() )return null;
				in = new DataInputStream(
					new BufferedInputStream(
					new FileInputStream(file)));
			}
			Grid2D.Float tile = (Grid2D.Float)createGridTile(x,y);
			Rectangle bounds = tile.getBounds();
			int i=0;
			int n;
			int size = gridSize*gridSize;
			while( i<size ) {
				n = in.readInt();
				i += n;
				if( i<size ) {
					n = in.readInt();
					x = i%gridSize;
					y = i/gridSize;
					for( int k=0 ; k<n ; k++) {
						tile.setValue(bounds.x+x, 
							bounds.y+y, 
							in.readFloat());
						x++;
						if(x==gridSize) {
							x=0;
							y++;
						}
					}
					i += n;
				}
			}
			in.close();
			return tile;
		}
		public Grid2D createGridTile(int x, int y) {
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.Float(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) 
		 			throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.Float) ) {
				throw new IOException("wrong grid type");
			}
			float[] grid = ((Grid2D.Float)tile).getBuffer();
			if( grid==null )return;
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".zgrid");
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(file)));
			int i=0;
			int n;
			while(i<grid.length) {
				n=0;
				while( i<grid.length && java.lang.Float.isNaN(grid[i]) ) {
					n++;
					i++;
				}
				out.writeInt(n);
				if( i>=grid.length ) break;
				n=0;
				while( i+n<grid.length && !java.lang.Float.isNaN(grid[i+n]) ) n++;
				out.writeInt(n);
				for( int k=0 ; k<n ; k++) {
					out.writeFloat(grid[i]);
					i++;
				}
			}
			out.close();
		}
	}
	public static class FloatWT extends TileIO {
		public FloatWT(MapProjection proj, 
				String root, 
				int gridSize) {
			super( proj, root, gridSize);
		}
		public FloatWT(MapProjection proj, 
				String root, 
				int gridSize,
				int nLevel) {
			super( proj, root, gridSize, nLevel);
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			try {
				String path = getDirPath( x, y);
				path += getName(x, y)+".xgrid";
				if( label!=null) label.setText( "reading "+path);
	// System.out.println(path);
				in =null;
				if(remote) {
					URL url = URLFactory.url(path);
					in = new DataInputStream(
						url.openStream());
				} else {
					File file = new File(path);
					if( !file.exists() )return null;
					in = new DataInputStream(
						new BufferedInputStream(
						new FileInputStream(file)));
				}
				Grid2D.FloatWT tile = (Grid2D.FloatWT)createGridTile(x,y);
				tile.initGrid();
				Rectangle bounds = tile.getBounds();
				int i=0;
				int n;
				int size = gridSize*gridSize;
				while( i<size ) {
					n = in.readInt();
					i += n;
					if( i<size ) {
						n = in.readInt();
						x = i%gridSize;
						y = i/gridSize;
						for( int k=0 ; k<n ; k++) {
							float z = in.readFloat();
							float w = in.readFloat();
							tile.setValue(bounds.x+x, 
								bounds.y+y, 
								z/w, w);
							x++;
							if(x==gridSize) {
								x=0;
								y++;
							}
						}
						i += n;
					}
				}
				in.close();
				return tile;
			} catch(IOException e) {
				if(parent==null) throw e;
			}
			return parent.readGridTile(x, y);
		}
		public Grid2D createGridTile(int x, int y) {
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.FloatWT(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) 
		 			throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.FloatWT) ) {
				throw new IOException("wrong grid type");
			}
			float[] grid = ((Grid2D.FloatWT)tile).getBuffer();
			if( grid==null )return;
			float[] weight = ((Grid2D.FloatWT)tile).getWeights();
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".xgrid");
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(file)));
			int i=0;
			int n;
			while(i<grid.length) {
				n=0;
				while( i<grid.length && weight[i]==0f ) {
					n++;
					i++;
				}
				out.writeInt(n);
				if( i>=grid.length ) break;
				n=0;
				while( i+n<grid.length && (weight[i+n]!=0f) ) n++;
				out.writeInt(n);
				for( int k=0 ; k<n ; k++) {
					out.writeFloat(grid[i]);
					out.writeFloat(weight[i]);
					i++;
				}
			}
			out.close();
		}
	}
	public static class Short extends TileIO {
		public final static short NaN = -32768;
		public Short(MapProjection proj, 
					String root, 
					int gridSize) {
			super( proj, root, gridSize);
		}
		public Short(MapProjection proj, 
					String root, 
					int gridSize,
					int nLevel) {
			super( proj, root, gridSize, nLevel);
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			String path = getDirPath( x, y);
			path += getName(x, y)+".igrid.gz";
			if( label!=null) label.setText( "reading "+path);
//			System.out.println(path);
	
			in =null;
			if(remote) {
				URL url = URLFactory.url(path);
				in = new DataInputStream(new GZIPInputStream(url.openStream()));
			} else {
				File file = new File(path);
				if( !file.exists() )throw new IOException("non-existent file");
				in = new DataInputStream(
					new BufferedInputStream(
					new GZIPInputStream(
					new FileInputStream(file))));
			}
			Grid2D.Short tile = (Grid2D.Short)createGridTile(x, y);
			int i=0;
			int size = gridSize*gridSize;
			int n = in.readInt();
			double offset = 0.;
			double scale = 1.;
			if( n<0 ) {
				offset = in.readDouble();
				scale = in.readDouble();
				tile.scale(offset, scale);
				n = in.readInt();
			}
			byte[] buf = new byte[n];
			in.readFully( buf );
			in.close();

			short[] grid = XgrdIO.decode( buf, size );
			if( grid.length!=size ) {
				throw new IOException( "incorrect length: "+
						grid.length +" ("+ (size*size) +")");
			}
			tile.setBuffer( grid );
	int count=0;
			for(y=0 ; y<gridSize ; y++) {
				for(x=0 ; x<gridSize ; x++) {
	if(grid[i]!=NaN) count++;
					tile.setValue(x, y, grid[i++]);
				}
			}
//	System.out.println("\t"+count);
			return tile;
		}
		public Grid2D createGridTile(int x, int y) {
//	System.out.println("+ "+getDirPath(x, y));
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.Short(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) 
		 			throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.Short) ) {
				throw new IOException("wrong grid type");
			}
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			Grid2D.Short sTile = (Grid2D.Short)tile;
			short[] grid = sTile.getBuffer();
			if( grid==null )return;
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".igrid.gz");
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new GZIPOutputStream(
					new FileOutputStream(file))));
			if( sTile.isScaled() ) {
				double[] scales = sTile.getScales();
				out.writeInt( -1 );
				out.writeDouble( scales[0] );
				out.writeDouble( scales[1] );
			}
			int i=0;
			int n;
			byte[] buf = XgrdIO.encode( grid );
			n = buf.length;
			out.writeInt( n );
			out.write( buf, 0, n);
			out.close();
		}
	}
	public static class JPG extends TileIO {
		public final static int NaN = 0;
		int pad;
		public JPG(MapProjection proj, 
					String root, 
					int gridSize,
					int pad) {
			super( proj, root, pad, gridSize);
		}
		public JPG(MapProjection proj, 
					String root, 
					int gridSize,
					int pad,
					int nLevel) {
			super( proj, root, gridSize, nLevel);
			this.pad = pad;
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			String path = getDirPath( x, y);
			path += getName(x, y)+".jpg";
			if( label!=null) label.setText( "reading "+path);
//	System.out.println(path);
			in =null;
			if(remote) {
				URL url = URLFactory.url(path);
				in = new DataInputStream(
					url.openStream());
			} else {
				File file = new File(path);
				if( !file.exists() )throw new IOException("non-existent file");
				in = new DataInputStream(
					new BufferedInputStream(
					new FileInputStream(file)));
			}
			Grid2D.Image tile = (Grid2D.Image)createGridTile(x, y);
			if( pad==0 ) {
				tile.setBuffer( ImageIO.read(in) );
			} else {
				BufferedImage im0 = ImageIO.read(in);
				BufferedImage im = new BufferedImage( im0.getWidth()-pad*2,
								im0.getHeight()-pad*2,
								BufferedImage.TYPE_INT_RGB);
				for( int ix=0 ; ix<gridSize ; ix++) {
					for( int iy=0 ; iy<gridSize ; iy++) {
						im.setRGB(ix,iy,im0.getRGB(ix+pad, iy+pad));
					}
				}
				tile.setBuffer( im );
			}
			return tile;
		}
		public Grid2D createGridTile(int x, int y) {
//	System.out.println("+ "+getDirPath(x, y));
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.Image(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) 
		 			throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.Image) ) {
				throw new IOException("wrong grid type");
			}
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			Grid2D.Image sTile = (Grid2D.Image)tile;
			BufferedImage grid = sTile.getBuffer();
			if( grid==null )return;
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".jpg");
			ImageIO.write( grid, "jpg", file);
		}
	}
	public static class PNG extends TileIO {
		public final static int NaN = 0;
		public PNG(MapProjection proj, 
					String root, 
					int gridSize) {
			super( proj, root, gridSize);
		}
		public PNG(MapProjection proj, 
					String root, 
					int gridSize,
					int nLevel) {
			super( proj, root, gridSize, nLevel);
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			String path = getDirPath( x, y);
			path += getName(x, y)+".png";
			if( label!=null) label.setText( "reading "+path);
//	System.out.println(path);
			in =null;
			if(remote) {
				URL url = URLFactory.url(path);
				in = new DataInputStream(
					url.openStream());
			} else {
				File file = new File(path);
				if( !file.exists() )throw new IOException("non-existent file");
				in = new DataInputStream(
					new BufferedInputStream(
					new FileInputStream(file)));
			}
			Grid2D.Image tile = (Grid2D.Image)createGridTile(x, y);
			tile.setBuffer( ImageIO.read(in) );
			return tile;
		}
		public Grid2D createGridTile(int x, int y) {
//	System.out.println("+ "+getDirPath(x, y));
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.Image(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.Image) ) {
				throw new IOException("wrong grid type");
			}
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			Grid2D.Image sTile = (Grid2D.Image)tile;
			BufferedImage grid = sTile.getBuffer();
			if( grid==null )return;
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".png");
			ImageIO.write( grid, "png", file);
		}
	}
	public static class Boolean extends TileIO {
		public Boolean(MapProjection proj, 
					String root, 
					int gridSize) {
			super( proj, root, gridSize);
		}
		public Boolean(MapProjection proj, 
					String root, 
					int gridSize,
					int nLevel) {
			super( proj, root, gridSize, nLevel);
		}
		public Grid2D readGridTile(int x, int y) 
					throws IOException {
			String path = getDirPath( x, y);
			path += getName(x, y)+".bgrid.gz";
			if( label!=null) label.setText( "reading "+path);
//	System.out.println(path);
			in =null;
			if(remote) {
				URL url = URLFactory.url(path);
				in = new DataInputStream(
					new GZIPInputStream(
					url.openStream()));
			} else {
				File file = new File(path);
				if( !file.exists() )throw new IOException("non-existent file");
				in = new DataInputStream(
					new BufferedInputStream(
					new GZIPInputStream(
					new FileInputStream(file))));
			}
			Grid2D.Boolean tile = (Grid2D.Boolean)createGridTile(x, y);
			int i=0;
			int size = gridSize*gridSize;
			size = (size+7)>>3;
			byte[] buf = new byte[size];
			in.readFully( buf );
			in.close();

			tile.setBuffer(buf);
			return tile;
		}
		public Grid2D createGridTile(int x, int y) {
//	System.out.println("+ "+getDirPath(x, y));
			x = (int)Math.floor( (double)x/(double)gridSize );
			y = (int)Math.floor( (double)y/(double)gridSize );
			return new Grid2D.Boolean(
					new Rectangle(x*gridSize,
						y*gridSize,
						gridSize,
						gridSize),
					proj);
		}
		public void writeGridTile(Grid2D tile) 
		 			throws IOException {
			if( remote )throw new IOException("cannot write to URL");
			if( !(tile instanceof Grid2D.Boolean) ) {
				throw new IOException("wrong grid type");
			}
			Rectangle bounds = tile.getBounds();
			if( gridSize != bounds.width ) {
				throw new IOException("inconsistent grid size");
			}
			byte[] grid = ((Grid2D.Boolean)tile).getBuffer();
			if( grid==null )return;
			int x = bounds.x;
			int y = bounds.y;
			String path = getDirPath(x, y);
			File file = new File(path);
			if( !file.exists() ) file.mkdirs();
			file = new File(file, getName(x, y)+".bgrid.gz");
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new GZIPOutputStream(
					new FileOutputStream(file))));
			int i=0;
			int n;
			out.write( grid, 0, grid.length);
			out.close();
		}
	}
}
