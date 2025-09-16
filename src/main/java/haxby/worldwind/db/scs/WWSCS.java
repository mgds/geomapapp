package haxby.worldwind.db.scs;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.db.Database;
import haxby.db.scs.SCS;
import haxby.db.scs.SCSCruise;
import haxby.db.scs.SCSImage2;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TrackLine;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.layers.LayerSet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;

public class WWSCS extends SCS implements WWOverlay {

	protected WorldWindow ww;
	
	protected WWLayer layer;
	protected LayerSet layerSet;
	protected SCSTileLayer scsTiles;
	protected RenderableLayer trackLayer;
	protected IconLayer iconLayer;
	protected UserFacingIcon icon;
	
	protected List<Polyline> imageTrackLine = new LinkedList<Polyline>();
	protected List<Polyline> selectedTrackLine = new LinkedList<Polyline>(); 
	protected List<Polyline> currentSegment = new LinkedList<Polyline>();

	protected TerrainClickListener terrainListener =
		new TerrainClickListener();
	
	protected SCSImageMouseListener imageListener = 
		new SCSImageMouseListener();
	
	public WWSCS(WorldWindow ww, XMap map) {
		super(map);
		
		this.ww = ww;
		image.addMouseMotionListener( imageListener );
		image.addMouseListener( imageListener );
	}

	@Override
	protected SCSImage2 createSCSImage() { 
		return new WWSCSImage2(this);
	}
	
	@Override
	public boolean loadDB() {
		if (!super.loadDB())
			return false;
		
		trackLayer = new RenderableLayer();
		
		iconLayer = new IconLayer();
		icon = new UserFacingIcon("org/geomapapp/resources/icons/wdot.png", Position.fromDegrees(0, 0, 0));
		icon.setSize( new Dimension(16,16));
		icon.setVisible(false);
		iconLayer.addIcon(icon);
		
		layerSet = new LayerSet();
		layerSet.setName(getDBName());
		layerSet.add( trackLayer );
		layerSet.add( iconLayer );

		layer = new WWLayer(layerSet) {
			public void close() {
				((MapApp)map.getApp()).closeDB(WWSCS.this);
			}
			public Database getDB() {
				return WWSCS.this;
			}
		};
		
		scsTiles = new SCSTileLayer(
				new SCSTrackTiler(cruises));
		
		layerSet.add(scsTiles);
		
		ww.getInputHandler().addMouseListener(terrainListener);
		
		return true;
	}
	
	@Override
	public void disposeDB() {
		super.disposeDB();
		
		ww.getInputHandler().removeMouseListener(terrainListener);
		
		imageTrackLine.clear();
		currentSegment.clear();
		selectedTrackLine.clear();
		
		if (scsTiles != null)
		{
			scsTiles.dispose();
			layerSet.remove(scsTiles);
			scsTiles = null;
		}
		trackLayer.dispose();
		iconLayer.dispose();
		layerSet.dispose();
		layer.dispose();
		
		trackLayer = null;
		iconLayer = null;
		layerSet = null;
		layer = null;
	}
	
	@Override
	public void setEnabled(boolean tf) {
		if( tf == enabled) return;
		enabled = tf;
		if(enabled) {
			ww.getInputHandler().addMouseListener(terrainListener);
			map.addMouseListener( this);
		} else {
			ww.getInputHandler().removeMouseListener(terrainListener);
			map.removeMouseListener( this);
		}
	}
	
	public Layer getLayer() {
		return layer;
	}

	public SelectListener getSelectListener() {
		return null;
	}

	public void setArea(Rectangle2D bounds) {

	}
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {

			if (cruiseList.getSelectedIndex() == -1) {
				selCruise = -1;
				makeSelectedTrack();
			} else {
				if (cruiseListPopulated)	{
					double zoom = map.getZoom();
					Nearest nearest = new Nearest(null, 0, 0, Math.pow(2./zoom, 2) );
					int c = cruiseList.getSelectedIndex();
					String text = cruises[c].getName() +" "+ 
						SCSCruise.dateString(cruises[c].getTime(nearest));
					selPanel = cruises[c].getPanel( cruises[c].getTime(nearest) );
					selPath = cruises[c].getPanelPath( selPanel );
			
					label.setText( text );
					
					selCruise = c;
					
					makeSelectedTrack();
					makeSegmentPath();
				}
			}
		}
	}
	
	protected void setCurrentPoint(Point2D currentPoint) {
		icon.setVisible( currentPoint != null );
		
		if (currentPoint != null)
		{
			Position p = Position.fromDegrees(currentPoint.getY(), 
					currentPoint.getX(), 0);
			icon.setPosition(p);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void makeSegmentPath() {
		
		for (Polyline line : currentSegment)
			trackLayer.removeRenderable(line);
		currentSegment.clear();
		
		if (selPath == null)
			return;
		
		PathIterator pi =
			selPath.getPathIterator(new AffineTransform());
		List<LatLon> pos = null;

		float [] coords = new float[6];
		int t;
		while (!pi.isDone())
		{
			t = pi.currentSegment(coords);
			
			if (t == PathIterator.SEG_MOVETO)
			{
				if (pos != null)
				{
					Polyline line = new Polyline(pos, 0);
					line.setLineWidth(4);
					line.setFollowTerrain(true);
					line.setColor( Color.red );
					currentSegment.add(line);
					trackLayer.addRenderable(line);
				}
				
				pos = new LinkedList<LatLon>();
				pos.add( LatLon.fromDegrees(coords[1], coords[0]));
			}
			else if (t == PathIterator.SEG_LINETO && 
					!(coords[1] == 0 && coords[0]==0))
				pos.add( LatLon.fromDegrees(coords[1], coords[0]));
			else if (t == PathIterator.SEG_CLOSE)
				pos.add( pos.get(0) );
			
			pi.next();
		}
		
		Polyline line = new Polyline(pos, 0);
		line.setLineWidth(4);
		line.setFollowTerrain(true);
		line.setColor( Color.red );
		currentSegment.add(line);
		trackLayer.addRenderable(line);
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void makeSelectedTrack() {
		for (Polyline line : selectedTrackLine)
			trackLayer.removeRenderable(line);
		selectedTrackLine.clear();
		
		if (selCruise == -1)
			return;
		
		SCSCruise cruise = cruises[selCruise];
		
		if (cruise == null) return;
		
		TrackLine nav = cruise.getNav();
		
		if (nav == null) return;
		
		ControlPt[][] cpts = nav.getCpts();
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			List<LatLon> pos = new LinkedList<LatLon>();
			
			for(int i=0 ; i<cpts[seg].length ; i++) {
				pos.add(LatLon.fromDegrees(cpts[seg][i].getY(),
						cpts[seg][i].getX()));
			}
			
			Polyline line = new Polyline(pos, 0);
			line.setFollowTerrain(true);
			trackLayer.addRenderable(line);
			selectedTrackLine.add(line);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	protected void makeImageTrack() {
		for (Polyline line : imageTrackLine)
			trackLayer.removeRenderable(line);
		imageTrackLine.clear();
		
		SCSCruise cruise = image.getCruise();
		
		if (cruise == null) return;
		
		TrackLine nav = cruise.getNav();
		
		if (nav == null) return;
		
		ControlPt[][] cpts = nav.getCpts();
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			List<LatLon> pos = new LinkedList<LatLon>();
			
			for(int i=0 ; i<cpts[seg].length ; i++) {
				pos.add(LatLon.fromDegrees(cpts[seg][i].getY(),
						cpts[seg][i].getX()));
			}
			
			Polyline line = new Polyline(pos, 0);
			line.setFollowTerrain(true);
			line.setColor(Color.blue);
			trackLayer.addRenderable(line);
			imageTrackLine.add(line);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
//	
//	public void adjustmentValueChanged(AdjustmentEvent e) {
//		selPath = image.getCurrentPath();
//		makeSegmentPath();
//	}

	private class TerrainClickListener extends MouseAdapter
	{
		public void mouseClicked( MouseEvent evt) {
			if(evt.isControlDown())return;
			
			OrbitView view = (OrbitView) ww.getView();
			Globe globe = ww.getModel().getGlobe();
			
			Position pos =
				view.computePositionFromScreenPoint(evt.getX(), evt.getY());
			
			if (pos == null) return;
			
			double pixelSizeD = Angle.fromRadians(view.computePixelSizeAtDistance(view.getZoom())
					/ globe.getEquatorialRadius()).degrees;
			
			double x = pos.getLongitude().degrees;
			double y = pos.getLatitude().degrees;
//			
			Nearest nearest = new Nearest(null, 0, 0, 2 * pixelSizeD );
			
			if( image.getCruise() !=null && 
					firstNearPoint(image.getCruise().getNav(), x, y, nearest)) {
				image.scrollTo( 
						(int)(image.getCruise().getTime(nearest)/1000L) );

				selCruise = -1;
				makeSelectedTrack();
				return;
			}
			int c = selCruise;
			int kk0 = c;
			for( int kk=0 ; kk<=cruises.length ; kk++) {
				int ic = (kk+kk0+1)%cruises.length;
				while( ic>=cruises.length ) ic -= cruises.length;
				try {
					if( !cruises[ic].contains(x, y ) ) continue;
				} catch( NullPointerException ex) {
					System.out.println("null pointer, ic = "+ic);
					continue;
				}
				if( !firstNearPoint(
						cruises[ic].getNav(), x, y, nearest) ) continue;
				
				String text = cruises[ic].getName() +" "+ 
						SCSCruise.dateString(cruises[ic].getTime(nearest));
				
				selPanel = cruises[ic].getPanel( cruises[ic].getTime(nearest) );
				selPath = cruises[ic].getPanelPath( selPanel );
				selCruise = ic;
				
				label.setText( text );
				
				makeSelectedTrack();
				makeSegmentPath();
				if (evt.getClickCount() == 2)
					WWSCS.this.view.doClick();
				
				evt.consume();

				return;
			}
			
				if (selCruise != -1) evt.consume();
			
			selCruise = -1;
			selPath = image.getPaths();
			
			makeSelectedTrack();
			makeSegmentPath();
		}
		
		private boolean firstNearPoint(TrackLine nav, double x, double y,
				Nearest nearest) {
			double wrap = nav.getWrap();
			Rectangle2D bounds = nav.getBounds();
			ControlPt[][] cpts = nav.getCpts();
			
			double dx, dy, dx0, dy0, r, r0, test, xx;
			xx = r = 0;
			double x1, y1, x2, y2;
			if(wrap>0) {
				if( y<bounds.getY() || 
					y>bounds.getY()+bounds.getHeight() )return false;
				while(x<bounds.getX()) x+=wrap;
				while(x>bounds.getX()+bounds.getWidth()) x-=wrap;
				if(x<bounds.getX())return false;
			} else {
				if(!bounds.contains(x, y)) return false;
			}
			for( int seg=0 ; seg<cpts.length ; seg++ ) {
				x1 = cpts[seg][0].getX();
				y1 = cpts[seg][0].getY();
				for( int i=0 ; i<cpts[seg].length-1 ; i++ ) {
					x2 = cpts[seg][i+1].getX();
					y2 = cpts[seg][i+1].getY();

					int [] wrapArray = new int[] {0};
					if (Math.abs(x1 - x2) > 180) 
					{
						if (x2 > x1)
						{
							x2 -= 360;
							wrapArray = new int[] {0, 360};
						}
						else 
						{
							x2 += 360;
							wrapArray = new int[] {0, -360};
						}
					}
					
					if(x1==x2 && y1==y2) continue;
					
					boolean pass = false;
					for (int wrapX : wrapArray)
					{
						dx0 = x2-x1;
						dy0 = y2-y1;
						dx = x-x1 - wrapX;
						dy = y-y1;
						r0 = dx0*dx0 + dy0*dy0;
						test = dx*dx0 + dy*dy0;
//						System.out.println(x1 + "\t" + x2 + "\t" + y1 + "\t" + y2 + "\t" + x + "\t" + y + "\t" + wrapX);
						if(test<0) {
							r = dx*dx + dy*dy;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = i;
							break;
						} else if( test>r0 ) {
							dx -= dx0;
							dy -= dy0;
							r = dx*dx + dy*dy;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = (i+1);
							break;
						} else {
							r = -dx*dy0 + dy*dx0;
							r *= r/r0;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = i + test/r0;
							break;
						}
					}
					
					x1 = x2;
					y1 = y2;
					
					if (!pass) continue;
					
					nearest.rtest = r;
					nearest.x = xx;
					nearest.seg = seg;
					nearest.track = nav;
					return true;
				}
			}
			return false;
		}
	}
	
	private class SCSImageMouseListener extends MouseInputAdapter
	{
		@Override
		public void mouseMoved(MouseEvent e) {
			Integer t = image.getTimeAtPoint(e.getPoint());
			if (t == null) return;
			Point2D p = image.getCruise().getNav().positionAtTime( t );
			setCurrentPoint(p);
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			setCurrentPoint(null);
		}
	}
	
	private class WWSCSImage2 extends SCSImage2 {

		public WWSCSImage2(SCS scs) {
			super(scs);
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);

			selPath = this.getPaths();
			makeSegmentPath();
		}
		
		@Override
		public void paintWhole(Graphics g) {
			super.paintWhole(g);
			
			selPath = this.getPaths();
			makeSegmentPath();
		}
		
		@Override
		public void setCruise(SCSCruise cruise, boolean load)
				throws IOException {
			super.setCruise(cruise, load);
			selCruise = -1;
			makeSelectedTrack();
			makeImageTrack();
		}
	}
}
