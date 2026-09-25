package haxby.grid;

import haxby.proj.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class CombineZW {
	public static void main(String[] args) {
		if(args.length!=3) {
			System.out.println("usage: haxby.grid.CombineZW dir proj scale");
			System.exit(0);
		}
		try {
			String dir=args[0];
			int scale = Integer.parseInt( args[2] );
			int type = Integer.parseInt( args[1]);
			String baseDir = (type==0) ? "/scratch/ridgembs/bill/grid/final"
						:  "/scratch/ridgembs/bill/antarctic/public";
			GridderZW zw;
			GridderZW zw0;
			int nGrid = 1024/scale;
			int nLevel = 0;
			int ng = nGrid;
			while( ng>8 ) {
				nLevel++;
				ng /= 8;
			}
			File[] files=null;
			if( type==0 ) {
				Mercator merc = ProjectionFactory.getMercator( 320*1024/scale );
				zw = new GridderZW( 320, 1, nLevel, merc, dir+"/merc_320_"+nGrid);
				zw0 = new GridderZW( 320, 1, nLevel, merc, baseDir+"/merc_320_"+nGrid);
				files = getFiles(new File(dir + "/merc_320_"+ nGrid +"/zw"));
			} else {
				PolarStereo proj = new PolarStereo( new Point(0, 0),
						180., scale*50., -71.,
						PolarStereo.SOUTH, PolarStereo.WGS84);
				int res = 50*scale;
				zw = new GridderZW( 320, 1, nLevel, proj, dir + "/SP_320_"+res );
				zw0 = new GridderZW( 320, 1, nLevel, proj, baseDir + "/SP_320_"+res );
				files = getFiles(new File(dir + "/SP_320_"+ res +"/zw"));
			}
			zw.setReadonly( true );
			System.out.println( files.length +" files");
			for( int k=0 ; k<files.length ; k++) {
				String name = files[k].getName();
				System.out.println( name +"\t"+ (k+1) +" of\t"+ files.length );
				XGrid_ZW grid = zw.getGrid( name );
				XGrid_ZW grid0 = zw0.getGrid( name );
				float[][] g = grid.getGrid();
				float[][] g0 = grid0.getGrid();
				for( int i=0 ; i<g.length ; i++) {
					g0[i][0] += g[i][0];
					g0[i][1] += g[i][1];
				}
			}
			zw0.finish();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	public static File[] getFiles(File file) {
		Vector files = new Vector();
		File[] list = file.listFiles();
		for( int i=0 ; i<list.length ; i++) {
			String name = list[i].getName();
			if( name.endsWith(".xgrid") ) {
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
