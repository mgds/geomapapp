package org.geomapapp.grid;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.util.*;

import java.util.Vector;
import java.io.*;
import java.net.URL;
import java.awt.Rectangle;

public class TiledGrid extends Grid2D
		implements Runnable, Abortable {
	Vector tiles;
	int capacity;
	int gridSize;
	int wrap;
	TiledGrid child, parent;
	TileIO tileIO;
	Thread thread;
	public TiledGrid(MapProjection proj, 
			Rectangle bounds,
			TileIO tileIO,
			int gridSize,
			int capacity,
			TiledGrid child) {
		super(bounds, proj);

		this.tileIO = tileIO;
		this.capacity = capacity;
		tiles = new Vector(capacity);
		this.gridSize = gridSize;
		this.bounds = bounds;
		wrap = -1;
		this.child = child;
		if(child!=null) child.setParent(this);
		parent = null;
	}
	public void setReadonly(boolean tf) {
		tileIO.setReadonly(tf);
	}
	public boolean isReadonly() {
		return tileIO.isReadonly();
	}
	public void setParent(TiledGrid parent) {
		this.parent = parent;
	}
	public void setWrap( int width ) {
		wrap = width;
	}
	public boolean contains( int x, int y ) {
		if( wrap>0 ) {
			while( x<bounds.x ) x+=wrap;
			while( x>=bounds.x+bounds.width ) x-=wrap;
		}
		return super.contains(x,y);
	}
	public boolean contains( double x, double y ) {
		if( wrap>0 ) {
			while( x<bounds.x ) x+=wrap;
			while( x>=bounds.x+bounds.width ) x-=wrap;
		}
		return super.contains(x,y);
	}
	public double valueAt( int x, int y ) {
		Grid2D tile = getTile(x, y);
		if( tile==null )return Double.NaN;
		return tile.valueAt(x, y);
	}
	public void setValue(int x, int y, double val) {
		Grid2D tile = getTile(x, y);
		if( tile==null )return;
		tile.setValue(x, y, val);
	}
	Grid2D grid;
	public Grid2D composeGrid( Rectangle bnds ) {
		grid = new Grid2D.Short( bnds, projection );
		return composeGrid( grid );
	}
	public Grid2D composeGrid( Grid2D grid ) {
		this.grid = grid;
	//	thread = new Thread(this);
	//	thread.start();
		return compose();
	}
	public void run() {
		abort = false;
		progress = haxby.map.MapApp.getProgressDialog();
		javax.swing.JLabel label = new javax.swing.JLabel("Loading grids");
		tileIO.setLabel(label);
		if( !progress.showProgress(label, this) )return;
		compose();
		progress.setAlive(false);
	}
	boolean abort;
	ProgressDialog progress;
	public void abort() {
		abort = true;
		tileIO.abort();
		progress.setAlive(false);
	}
//	public Grid2D composeGrid( Rectangle bnds ) {
//		Grid2D.Short grid = new Grid2D.Short( bnds, projection );
//		return composeGrid( grid );
//	}
//	public Grid2D composeGrid( Grid2D grid) {
	public Grid2D compose() {
		Rectangle bnds = grid.bounds;
		int x1 = gridSize*(int)Math.floor(bnds.getX()/gridSize);
		int x2 = gridSize*(int)Math.floor((bnds.getX()+bnds.width)/gridSize);
		int y1 = gridSize*(int)Math.floor(bnds.getY()/gridSize);
		int y2 = gridSize*(int)Math.floor((bnds.getY()+bnds.height)/gridSize);
		if(capacity==0) setCapacity(1);
		int count = 0;
		for( int y=y1 ; y<=y2 ; y+=gridSize) {
			for( int x=x1 ; x<=x2 ; x+=gridSize) {
				if( abort ) return null;
				int xx1=(int)Math.max(x,bnds.x);
				int xx2=(int)Math.min(x+gridSize,bnds.x+bnds.width);
				int yy1=(int)Math.max(y,bnds.y);
				int yy2=(int)Math.min(y+gridSize,bnds.y+bnds.height);
				Grid2D g = getTile(xx1, yy1);
				if(g==null)continue;
				for( int yy=yy1 ; yy<yy2 ; yy++) {
					for( int xx=xx1 ; xx<xx2 ; xx++) {
						int x0 = xx;
						if( wrap>0 ) {
							while( x0<0 ) x0+=wrap;
							while( x0>=wrap ) x0-=wrap;
						}
						if( !java.lang.Double.isNaN(grid.valueAt(xx, yy)) )continue;
						double val = g.valueAt(x0,yy);
						if( java.lang.Double.isNaN(val) )continue;
						grid.setValue(xx,yy, val);
					count++;
			//	System.out.println( xx +"\t"+
			//			yy  +"\t"+
			//			g.valueAt(x0,yy) +"\t"+
			//			grid.valueAt(xx, yy));
					}
				}
			}
		}
	//	System.out.println(count +" counts");
		return grid;
	}
	public Grid2D getTile(int x, int y) {
		if( wrap>0 ) {
			while( x<0 ) x+=wrap;
			while( x>=wrap ) x-=wrap;
		}

		if(!contains(x,y))return null;

		if( child!=null ) {
			Grid2D tile = child.getTile(x, y);
			if(tile!=null)return tile;
		}

		for( int k=0 ; k<tiles.size() ; k++) {
			Grid2D tile = (Grid2D)tiles.get(k);
			if( tile.contains(x, y) ) {
				if(k!=0) tiles.add(0,tiles.remove(k));
				return tile;
			}
		}
		Grid2D tile = getGridTile( x, y);
		if( tile==null )return null;
		if( tiles.size()<capacity ) {
			tiles.add(0, tile);
		} else {
			Grid2D oldTile = (Grid2D)tiles.remove( capacity-1 );
			if( !isReadonly() ) {
				try {
					tileIO.writeGridTile( oldTile );
				} catch(IOException e) {
				}
			}
			tiles.add(0,tile);
		}
		return tile;
	}
	public void setCapacity( int capacity ) {
		this.capacity = capacity;
		if( tiles.size()>capacity ) {
			tiles.setSize(capacity);
		}
	}
	Grid2D getGridTile( int x, int y ) {
		try {
			return tileIO.readGridTile(x, y);
		} catch (IOException ex) {
//			System.out.println(ex.toString());
			if( parent!=null) return null;
			return tileIO.createGridTile(x, y);
		}
	}
}
