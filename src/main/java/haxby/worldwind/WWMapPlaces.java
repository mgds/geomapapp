package haxby.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.map.MapApp;
import haxby.map.MapTools;

import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

import org.geomapapp.map.MapPlace;
import org.geomapapp.map.MapPlaces;

public class WWMapPlaces extends MapPlaces {
	public WWMapPlaces(WWMap map, MapTools tools) {
		super(map, tools);
	}
	@Override
	protected String createLocStr(MapPlace loc) {
		if (!(loc instanceof WWMapPlace))
			return super.createLocStr(loc);

		WWMapPlace wloc = (WWMapPlace) loc;

		return 
			loc.name
			+"\t"+ wloc.lon
			+"\t"+ wloc.lat
			+"\t"+ wloc.zoom
			+"\t"+ wloc.pitch
			+"\t"+ wloc.heading
			+"\t"+ wloc.zoom2
			+"\t"+ wloc.ve;
	}

	@Override
	protected MapPlace createPlace(MapPlace root, String text, double x,
			double y, double zoom) {
		WorldWindowGLCanvas wwd = ((WWMap) map).wwd;
		OrbitView view = (OrbitView) wwd.getView();

		zoom = ((WWMap) map).getGMAZoom();
		Position pos = view.getCenterPosition();
		double pitch = view.getPitch().degrees;
		double heading = view.getHeading().degrees;
		double zoom2 = view.getZoom();
		double ve = wwd.getSceneController().getVerticalExaggeration();

		return new WWMapPlace( 
				root,
				text, 
				pos.getLongitude().degrees,
				pos.getLatitude().degrees,
				zoom,
				pitch,
				heading,
				zoom2,
				ve);
	}

	@Override
	protected MapPlace readPlace(MapPlace parent, StringTokenizer st) {
		String[] t = new String[st.countTokens()];
		for (int i = 0; i < t.length; i++)
			t[i] = st.nextToken();

		if (t.length == 4) {
			return new MapPlace(
					parent,
					t[0],
					Double.parseDouble(t[1]),
					Double.parseDouble(t[2]),
					Double.parseDouble(t[3])
				);
		} else { 
			double ve = t.length == 8 ? Double.parseDouble(t[7]) : 1;
			return new WWMapPlace(
					parent,
					t[0],
					Double.parseDouble(t[1]),
					Double.parseDouble(t[2]),
					Double.parseDouble(t[3]),
					Double.parseDouble(t[4]),
					Double.parseDouble(t[5]),
					Double.parseDouble(t[6]),
					ve
				);
		}
	}

	@Override
	protected void goTo(MapPlace loc) {
		if (!(loc instanceof WWMapPlace)) {
			super.goTo(loc);
			return;
		}

		WWMapPlace wloc = (WWMapPlace) loc;
		final OrbitView view = (OrbitView) ((WWMap)map).wwd.getView();

		((WWMap)map).wwd.getSceneController().setVerticalExaggeration(wloc.ve);

		Position center = Position.fromDegrees(wloc.lat, wloc.lon, 0);
		Angle heading = Angle.fromDegrees(wloc.heading);
		Angle pitch = Angle.fromDegrees(wloc.pitch);
		double zoom = wloc.zoom2;

		FlyToOrbitViewAnimator fto = 
			FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(
				view, 
				view.getCenterPosition(), center,
				view.getHeading(), heading,
				view.getPitch(), pitch,
				view.getZoom(), zoom,
				5000, WorldWind.CONSTANT); // was true

		view.addAnimator(fto);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				((MapApp)map.getApp()).getFrame().toFront();
				view.firePropertyChange(AVKey.VIEW,  null, view);
			}
		});
	}
}
