package haxby.grid;

import haxby.proj.*;
import haxby.grid.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
//import com.sun.image.codec.jpeg.*;

public class SRTMImage {
	TilerZ tiler, tiler0, tiler1;
	BufferedImage image;
	int x0, y0;
	XGrid_Z grid, grid0, grid1;
	GridImager imager;
	File[] files;
	public SRTMImage(String dir, int res) {
		Mercator proj = ProjectionFactory.getMercator( 320*1024/res );
		Mercator proj0 = ProjectionFactory.getMercator( 320*1024/32 );
		Mercator proj1 = ProjectionFactory.getMercator( 320*1024/4 );
		int nLevel = 0;
		int nGrid = 1024/res;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int nLevel0 = 0;
		int nGrid0 = 32;
		while( nGrid0>8 ) {
			nLevel0++;
			nGrid0 /= 8;
		}
		int nLevel1 = 0;
		int nGrid1 = 256;
		while( nGrid1>8 ) {
			nLevel1++;
			nGrid1 /= 8;
		}
		tiler0 = null;
		tiler1 = null;
		tiler = null;
		files = getFiles( new File(dir+"/merc_320_1024/z_"+res) );
		Vector names = new Vector();
		for(int i=0 ; i<files.length ; i++) names.add(files[i].getName());
		try {
			if( res<=32 ) {
				tiler0 = new TilerZ( 320, 32, 3, nLevel0, proj0,
					"/data/ridgembs/scratch/bill/grid/final/merc_320_1024");
				tiler0.setWrap( 320*1024/32 );
			} else {
				tiler0 = new TilerZ( 320, res, 3, nLevel, proj,
					"/data/ridgembs/scratch/bill/grid/final/merc_320_1024");
				tiler0.setWrap( 320*1024/res );
			}
			tiler0.setReadonly(true);
			TilerZ tilerX = new TilerZ( 320, res, 1, nLevel, proj,
				"/data/ridgembs/scratch/bill/grid/final/merc_320_1024");
			tilerX.setWrap( 320*1024/res );
			tilerX.setReadonly(true);
			tiler = new TilerZ( 320, res, 6, nLevel, proj,
				dir+"/merc_320_1024", tilerX);
			tiler.setWrap( 320*1024/res );
			tiler.setReadonly(true);
			if( res<4 ) {
				tilerX = new TilerZ( 320, 4, 1, nLevel1, proj1,
					"/data/ridgembs/scratch/bill/grid/final/merc_320_1024");
				tilerX.setWrap( 320*1024/4 );
				tilerX.setReadonly(true);
				tiler1 = new TilerZShort( 320, 4, 3, nLevel1, proj1,
					dir+"/merc_320_1024", tilerX);
				tiler1.setWrap( 320*1024/4 );
				tiler1.setReadonly(true);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		tiler.setReadonly(true);
		imager = new GridImager();
		double ve = 2.5;
		int ires = res;
		while( ires>32 ) {
			ve *= 1.5;
			ires /=2;
		}
		imager.setVE(ve);
		imager.setUnitsPerNode( res*100. );
		imager.setGamma(1.5);
		imager.setBackground( 0xff808080 );
		image = null;
		x0 = 0;
		y0 = 0;
		int size0 = 320*res/32;
		int size1 = 320*res/4;
		for( int i=0 ; i<names.size() ; i++) {
			String name = (String) names.get(i);
			StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
			int x0 = (st.nextToken().equals("E")) ? 1 : -1;
			x0 *= Integer.parseInt(st.nextToken());
			int y0 = (st.nextToken().equals("N")) ? 1 : -1;
			y0 *= Integer.parseInt(st.nextToken());
			grid = new XGrid_Z( x0*320-8, y0*320-8, 337, 337, tiler);
			if( res<4 ){
				grid0 = new XGrid_Z( (x0-1)*size0, (y0-1)*size0, 3*size0, 3*size0, tiler0);
				grid1 = new XGrid_Z( (x0-1)*size1, (y0-1)*size1, 3*size1, 3*size1, tiler1);
				int k=0;
				float[] g = grid.getGrid();
				float[] gtmp = new float[g.length];
				Point p = new Point();
				for( int y=0 ; y<337 ; y++ ) {
					p.y = y;
					for( int x=0 ; x<337 ; x++, k++ ) {
					//	if( Float.isNaN(g[k]) ) {
							p.x = x;
							Point2D p0 = grid.getProjection().getRefXY( p );
							Point2D pt = grid1.getProjection().getMapXY(p0);
							gtmp[k] = grid1.valueAt(pt.getX(), pt.getY());
							if( Float.isNaN(g[k]) ) {
								pt = grid0.getProjection().getMapXY(p0);
								gtmp[k] = grid0.valueAt(pt.getX(), pt.getY());
							}
							if( !Float.isNaN(gtmp[k]) && gtmp[k]>-10f) gtmp[k]=-10f;
					//	}
					}
				}
				image = imager.gridImage( gtmp, 337, 337);
			} else if( res<32 ){
				grid0 = new XGrid_Z( (x0-1)*size0, (y0-1)*size0, 3*size0, 3*size0, tiler0);
				int k=0;
				float[] g = grid.getGrid();
				float[] gtmp = new float[g.length];
				Point p = new Point();
				for( int y=0 ; y<337 ; y++ ) {
					p.y = y;
					for( int x=0 ; x<337 ; x++, k++ ) {
					//	if( Float.isNaN(g[k]) ) {
							p.x = x;
							Point2D p0 = grid.getProjection().getRefXY( p );
							Point2D pt = grid0.getProjection().getMapXY( p0 );
							gtmp[k] = grid0.valueAt(pt.getX(), pt.getY());
							if( p0.getY()<60. && !Float.isNaN(gtmp[k]) && gtmp[k]>-10f) gtmp[k]=-10f;
					//	}
					}
				}
				image = imager.gridImage( gtmp, 337, 337);
			} else {
				grid0 = new XGrid_Z(x0*320-8, y0*320-8, 337, 337, tiler0);
				float[] g = grid.getGrid();
				float[] g0 = grid0.getGrid();
				for( int k=0 ; k<g.length ; k++) {
					if( Float.isNaN(g[k]) ) g[k]=g0[k];
				}
			}
		//	image = imager.gridImage( grid.getGrid(), 337, 337);
			if( image != null ) {
				image = imager.gridImage( grid.getGrid(), image );
			} else {
				image = imager.gridImage( grid.getGrid(), 337, 337);
			}
			String fileName = files[i].getPath();
			int index = fileName.indexOf("z_"+ res) + 3;
			if( res>=10 )index++;
			if( res>=100 )index++;
			int index2 = fileName.indexOf("zgrid");
			String newFile = dir +"/merc_320_1024/i_" + res + fileName.substring(index,index2)+"jpg";
			File file = new File(newFile);
			if( !file.getParentFile().exists() ) file.getParentFile().mkdirs();
			System.out.println( newFile +"\t"+ i +" of "+ files.length );
			try {
				BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream( newFile ));
				//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
				//encoder.encode(image);
				ImageIO.write(image, "JPEG", out);
				out.flush();
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(0);
			}
		}
	}
	public static void main(String[] args) {
		if( args.length != 2 ) {
			System.out.println("usage: java SRTMImage directory resolution");
			System.exit(0);
		}
		new SRTMImage(args[0], Integer.parseInt(args[1]) );
	}
	public static File[] getFiles(File file) {
		Vector files = new Vector();
		File[] list = file.listFiles();
		for( int i=0 ; i<list.length ; i++) {
			String name = list[i].getName();
			if( name.endsWith(".zgrid") ) {
				files.add(list[i]);
			} else if( (name.startsWith("E") || name.startsWith("W"))
					&& list[i].isDirectory() ) {
				File[] tmp = getFiles(list[i]);
				for( int k=0 ; k<tmp.length ; k++) {
					files.add(tmp[k]);
				}
			}
		}
		list = new File[files.size()];
		for(int i=0 ; i<list.length ; i++) {
			list[i] = (File)files.get(i);
		}
		return list;
	}
}
