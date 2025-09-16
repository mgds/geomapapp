package org.geomapapp.grid;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JTextArea;

import org.geomapapp.io.FileUtility;

import haxby.proj.IdentityProjection;

public class DecimateXBG {
	JTextArea area;
	ImportGrid ig;
	
	public DecimateXBG(JTextArea area, ImportGrid ig) {
		this.ig = ig;
		this.area = area;
	}
	
	public DecimateXBG(JTextArea area) {
		this.area = area;
	}
	public void process(File root, boolean isWrap) {
		if( root.getName().startsWith("z_") ) {
			decimate(root, isWrap);
			return;
		}
		File[] dirs = root.listFiles( new FileFilter() {
			public boolean accept( File f ) {
				if( !f.isDirectory() )return false;
				return f.getName().startsWith("z_");
			}
		});
		if( dirs==null || dirs.length==0 )return;
		int k0=0;
		int res = Integer.parseInt( dirs[0].getName().substring(2) );
		for( int k=1 ; k<dirs.length ; k++) {
			int r = Integer.parseInt( dirs[k].getName().substring(2) );
			if( r<res ) {
				r=res;
				k0=k;
			}
		}
		decimate(dirs[k0], isWrap);
	}
	public void decimate( File root , boolean isWrap) {
		File parent = root.getParentFile();
		area.setText( parent.getName() +", "+ root.getName() );
	//	area.update(area.getGraphics());
		int scl = Integer.parseInt(root.getName().substring(2));
		if (scl / 2 == 0) return;
		
		int scl1 = scl*2;
		int size = 320;
		int wrap = isWrap ? size* 2 * scl : -1;
		File root1 = new File(parent, "z_"+(scl/2));
		File[] files = FileUtility.getFiles(root, ".igrid.gz");
		if( files==null || files.length==0 ) {
			if (ig != null) ig.appendNewText("\n No Tiles");
			return;
		}
		if (ig != null) ig.appendNewText( ", "+files.length +" tiles");

		int res = scl;
		int nLevel = 0;
		int nLevel1 = 0;
		int xMin, xMax, yMin, yMax;
		xMin = yMin = Integer.MAX_VALUE; 
		xMax = yMax = -Integer.MAX_VALUE;
		for( int i=0 ; i<files.length ; i++) {
			int[] indices = TileIO.getIndices( files[i].getName() );
			int x0 = indices[0];
			int y0 = indices[1];
			xMin = Math.min(xMin, x0);
			yMin = Math.min(yMin, y0);
			xMax = Math.max(x0, xMax);
			yMax = Math.max(y0, yMax);
		}
		IdentityProjection proj = new IdentityProjection();
		IdentityProjection proj1 = proj;

		try {
			TileIO.Short tileIO = new TileIO.Short(proj,
					root.getPath(),
					size, nLevel);
			long[][] bnds = getBounds( tileIO, files );
			if( (bnds[0][1]-bnds[0][0]) * (bnds[0][1]-bnds[0][0])<300*300) return;
			double offset = (bnds[2][0]+bnds[2][1])/20.;
			double spread = (bnds[2][1]-bnds[2][0])/10.;
			if( spread==0. )spread=1.;
			double scale = 1.;
			while( spread*scale < 16000. ) scale*=2;
			if( scale>100. )scale=100.;
			if (ig != null) ig.appendNewText( "\noffset, scale = "+ offset +", "+scale);
			
			TiledGrid tiler = new TiledGrid(
					proj,
					new Rectangle( 
						size*xMin,
						size*yMin,
						size*(xMax-xMin+1),
						size*(yMax-yMin+1)),
					tileIO, size, 16, null);
			if(wrap>0)tiler.setWrap( wrap );
			TileIO.Short halfIO = new TileIO.Short(proj1,
					root1.getPath(),
					size, nLevel1);
			xMin = (int)Math.floor(xMin*.5);
			xMax = (int)Math.floor(xMax*.5);
			yMin = (int)Math.floor(yMin*.5);
			yMax = (int)Math.floor(yMax*.5);
			boolean[][] hasdata = new boolean[yMax-yMin+1][xMax-xMin+1];
			for( int y=0 ; y<hasdata.length ; y++ ) {
				for( int x=0 ; x<hasdata[y].length ; x++ ) {
					hasdata[y][x]=false;
				}
			}
			Vector corners = new Vector(files.length);
			for( int i=0 ; i<files.length ; i++) {
				int[] indices = tileIO.getIndices( files[i].getName() );
				int x0 = indices[0];
				int y0 = indices[1];
				if( x0<0 ) x0 = (x0-1) / 2;
				else x0 /= 2;
				if( y0<0 ) y0 = (y0-1) / 2;
				else y0 /= 2;
				hasdata[y0-yMin][x0-xMin] = true;
			}
			for( int y=0 ; y<hasdata.length ; y++ ) {
				for( int x=0 ; x<hasdata[y].length ; x++ ) {
					if( hasdata[y][x] ) corners.add( new int[] {x+xMin, y+yMin});
				}
			}
			if (ig != null) {
				ig.appendNewText( "\n"+ files.length +" files,\t" + corners.size() +" new tiles");	
				ig.waiting = true;
				//ig.displayWaitingDots();
			}
			
			for( int k=0 ; k<corners.size() ; k++) {
				if(ig != null) {
					ig.showPercent(100 * k / corners.size());
				}
				int[] xy = (int[]) corners.get(k);
				int x0 = xy[0]*size;
				int y0 = xy[1]*size;
				Grid2D.Short tile = (Grid2D.Short)halfIO.createGridTile(x0,y0);
				tile.scale( offset, scale );
				int kount=0;
				for( int y=y0 ; y<y0+size ; y++ ) {
					int yy = 2*y;
					for( int x=x0 ; x<x0+size ; x++ ) {
						int xx = 2*x;
						double ht = 0.;
						double wt = 0.;
						double z = tiler.valueAt(xx,yy);
						if( !Double.isNaN(z) ) {
							ht += z;
							wt += 1.;
						}
						z = tiler.valueAt(xx-1,yy);
						double z1 = tiler.valueAt(xx+1,yy);
						if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
							ht += .3*(z+z1);
							wt += .6;
						}
						z = tiler.valueAt(xx,yy-1);
						z1 = tiler.valueAt(xx,yy+1);
						if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
							ht += .3*(z+z1);
							wt += .6;
						}
						z = tiler.valueAt(xx+1,yy-1);
						z1 = tiler.valueAt(xx-1,yy+1);
						if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
							ht += .1*(z+z1);
							wt += .2;
						}
						z = tiler.valueAt(xx-1,yy-1);
						z1 = tiler.valueAt(xx+1,yy+1);
						if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
							ht += .1*(z+z1);
							wt += .2;
						}
						if( wt==0. ) {
							continue;
						}
						tile.setValue(x,y,ht/wt);
						kount++;
					}
				}
//				if (ig != null) ig.appendNewText( "\n"+ halfIO.getName(x0,y0) +"\t"+ 
//						(k+1) +" of "+ 
//						corners.size()
//						+"\t"+ kount +" points" );

				if( kount>0 ) halfIO.writeGridTile(tile);
			}
			decimate( root1, isWrap);
			if (ig != null) ig.waiting = false;
		} catch (IOException ex) {
			if (ig != null) ig.waiting = false;
			ex.printStackTrace();
		}
	}
	long[][] getBounds( TileIO tileIO, File[] files ) throws IOException{
		long[][] bounds = new long[3][2];
		for( int i=0 ; i<3 ; i++) 
			for( int j=0 ; j<2 ; j++ ) bounds[i][j]=0;
		boolean start = true;
		for( int k=0 ; k<files.length ; k++) {
			int[] xy = TileIO.getIndices(files[k].getName());
			int x0 = xy[0]*320;
			int y0 = xy[1]*320;
			Grid2D g = tileIO.readGridTile( x0, y0 );
			for( int y=y0 ; y<y0+320 ; y++) {
				for( int x=x0 ; x<x0+320 ; x++) {
					double val = g.valueAt(x,y);
					if( Double.isNaN(val) )continue;
					int z = (int)Math.rint(val*10);
					if( start ) {
						start=false;
						bounds[0][0] = bounds[0][1] = x;
						bounds[1][0] = bounds[1][1] = y;
						bounds[2][0] = bounds[2][1] = z;
						continue;
					}
					if( x<bounds[0][0] )bounds[0][0]=x;
					else if( x>bounds[0][1] )bounds[0][1]=x;
					if( y<bounds[1][0] )bounds[1][0]=y;
					else if( y>bounds[1][1] )bounds[1][1]=y;
					if( z<bounds[2][0] )bounds[2][0]=z;
					else if( z>bounds[2][1] )bounds[2][1]=z;
				}
			}
		}
		return bounds;
	}
}
