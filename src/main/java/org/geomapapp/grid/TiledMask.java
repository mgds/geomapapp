package org.geomapapp.grid;

import org.geomapapp.geom.MapProjection;

import java.util.Vector;
import java.io.*;
import java.net.URL;
import java.awt.Rectangle;

public class TiledMask extends TiledGrid {
	public TiledMask(MapProjection proj, 
			Rectangle bounds,
			TileIO.Boolean tileIO,
			int gridSize,
			int capacity,
			TiledMask child) {
		super(proj, bounds, tileIO, gridSize, capacity, child);
	}
	public boolean booleanValue( int x, int y ) {
		Grid2D.Boolean tile = (Grid2D.Boolean)getTile(x, y);
		if( tile==null )return false;
		return tile.booleanValue(x, y);
	}
	public void setValue(int x, int y, boolean tf) {
		Grid2D.Boolean tile = (Grid2D.Boolean)getTile(x, y);
		if( tile==null )return;
		tile.setValue(x, y, tf);
	}
	public Grid2D composeGrid( Rectangle bnds ) {
		Grid2D.Boolean grid = new Grid2D.Boolean( bnds, projection );
		return composeGrid( grid );
	}
	public Grid2D composeGrid( Grid2D.Boolean grid) {
		Rectangle bnds = grid.bounds;
		int x1 = gridSize*(int)Math.floor(bnds.getX()/gridSize);
		int x2 = gridSize*(int)Math.floor((bnds.getX()+bnds.width)/gridSize);
		int y1 = gridSize*(int)Math.floor(bnds.getY()/gridSize);
		int y2 = gridSize*(int)Math.floor((bnds.getY()+bnds.height)/gridSize);
		if(capacity==0) setCapacity(1);
		for( int y=y1 ; y<=y2 ; y+=gridSize) {
			for( int x=x1 ; x<=x2 ; x+=gridSize) {
				int xx1=(int)Math.max(x,bnds.x);
				int xx2=(int)Math.min(x+gridSize,bnds.x+bnds.width);
				int yy1=(int)Math.max(y,bnds.y);
				int yy2=(int)Math.min(y+gridSize,bnds.y+bnds.height);
				Grid2D.Boolean g = (Grid2D.Boolean)getTile(xx1, yy1);
				if(g==null)continue;
				for( int yy=yy1 ; yy<yy2 ; yy++) {
					for( int xx=xx1 ; xx<xx2 ; xx++) {
						int x0 = xx;
						if( wrap>0 ) {
							while( x0<0 ) x0+=wrap;
							while( x0>=wrap ) x0-=wrap;
						}
						if( grid.booleanValue(xx, yy) )continue;
						grid.setValue(xx,yy,
							g.booleanValue(x0,yy));
			//	System.out.println( xx +"\t"+
			//			yy  +"\t"+
			//			g.valueAt(x0,yy) +"\t"+
			//			grid.valueAt(xx, yy));
					}
				}
			}
		}
		return grid;
	}
}
