package org.geomapapp.db.util;

import haxby.map.*;

import java.awt.event.*;
import java.io.*;

import org.geomapapp.grid.*;
import org.geomapapp.map.*;

public class PC {
	MapApp app;
	MapImage mi;
	public static void main(String[] args) {
		new PC();
	}
	XMap map;
	public PC() {
		String base = "file:///local/data/home/bill/db/";
		MapApp.setBaseURL(base);
		GridComposer.setBaseURL("/local/data/home/bill/db/merc_320_1024/");
		SSGridComposer.setBaseURL("/local/data/home/bill/grids/Smith_Sandwell/");
		MMapServer.setAlternateURL(base+"merc_320_1024/");
		app = new MapApp(MapApp.MERCATOR_MAP);
		map = app.getMap();
		mi = new MapImage(map);
		map.addKeyListener( new KeyAdapter() {
			public void keyReleased( KeyEvent e ) {
				if( e.getKeyCode()==KeyEvent.VK_I && e.isControlDown() ) {
					Grid2D.Image im = mi.getImage(true);
					try {
						KMZSave kmz = new KMZSave(null);
						kmz.save( map.getTopLevelAncestor(), im );
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
	//	new org.geomapapp.gis.shape.ShapeViewer( app.getMap() );
	}
	public MapApp getApp() {
		return app;
	}
}
