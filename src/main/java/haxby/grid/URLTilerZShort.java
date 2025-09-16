package haxby.grid;

import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.io.IOException;
import java.net.URL;

public class URLTilerZShort extends URLTilerZ {
	public URLTilerZShort(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String url,
			URLTilerZ tiler) throws IOException {
		super( gridSize, scale, numGrids, nLevel, proj, url, tiler);
	}
	public URLTilerZShort(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String url) throws IOException {
		super( gridSize, scale, numGrids, nLevel, proj, url);
	}
	public URL getURL(int x, int y) throws IOException {
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String url = baseURL;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			url += "/"+getName(xG, yG);
			factor /= 8;
		}
		url += "/" + getName(x, y) + ".igrid.gz";
		return URLFactory.url(url);
	}
	XGridTile getGridTile( int x, int y, URL url ) throws IOException {
		return new XGridTileShort(x, y, gridSize, url, proj);
	}
}
