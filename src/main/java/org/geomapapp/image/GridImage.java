package org.geomapapp.image;

import org.geomapapp.grid.*;
import org.geomapapp.util.*;

import java.awt.Rectangle;

public class GridImage extends ImageComponent {
	Grid2D grid;
	Grid2D.Boolean landMask;
	boolean land, ocean;
	public GridImage() {
		setGrid( 
			new Grid2D.Byte( 
				new Rectangle(0, 0, 50, 50),
				new haxby.proj.IdentityProjection() 
			)
		);
		
	}
	public GridImage(Grid2D grid) {
		setGrid(grid);
	}
	public void setGrid(Grid2D grid) {
		this.grid = grid;
		Rectangle bounds = grid.getBounds();
		width = bounds.width-1;
		height = bounds.height-1;
		landMask = new Grid2D.Boolean(grid.getBounds(),
				grid.getProjection());
		land = false;
		ocean = true;
	}
	public Grid2D getGrid() {
		return grid;
	}
	public void setLandMask( Grid2D.Boolean landMask ) {
		Rectangle maskBounds = landMask.getBounds();
		Rectangle bounds = grid.getBounds();
		if( !maskBounds.equals(bounds) ) return;
		this.landMask = landMask;
		land = false;
		ocean = false;
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( landMask.booleanValue(x,y) ) {
					land=true;
				} else {
					ocean=true;
				}
				if( land==ocean )return;
			}
		}
	}
	public Grid2D.Boolean getLandMask() {
		return landMask;
	}
	public boolean hasLand() {
		return land;
	}
	public boolean hasOcean() {
		return ocean;
	}
}
