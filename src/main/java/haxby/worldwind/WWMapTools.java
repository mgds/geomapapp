package haxby.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import haxby.map.MapApp;
import haxby.map.MapTools;
import haxby.map.XMap;
import haxby.worldwind.layers.QueryableGridLayer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.util.Icons;

public class WWMapTools extends MapTools {
	protected JButton layerManagerB;
	protected JButton creditB;
	protected JSlider veSlider;
	protected JLabel veDisplay, processDisplay;

	protected WorldWindowGLCanvas wwCanvas;

	protected Point lastPoint;
	protected String gridUnits = "";
	protected double lat, lon, elev, alt, gridValue; {
		lat = lon = elev = alt = gridValue = Double.NaN;
	}

	public WWMapTools(MapApp app, XMap map, WorldWindowGLCanvas wwCanvas) {
		super(app, map);
		this.wwCanvas = wwCanvas;

		wwCanvas.getInputHandler().addMouseMotionListener(
				new MouseInputAdapter() {
					@Override
					public void mouseMoved(MouseEvent e) {
						lastPoint = e.getPoint();
						
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								updateInfo();
							}
						});
					}
				});

		wwCanvas.addRenderingListener(new RenderingListener() {
			public void stageChanged(RenderingEvent event) {
				if (event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP)) {
					alt = WWMapTools.this.wwCanvas.getView().getEyePosition().getElevation()/1000;

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							updateInfo();
						}
					});
				}
			}
		});
	}

	protected void updateInfo() {
		if (lastPoint != null) {
			Position position = WWMapTools.this.wwCanvas.getView().computePositionFromScreenPoint(lastPoint.x, lastPoint.y);
			if (position != null) {
				elev = WWMapTools.this.wwCanvas.getModel().getGlobe().getElevation(position.getLatitude(), position.getLongitude());
				lat = position.getLatitude().getDegrees();
				lon = position.getLongitude().getDegrees();

				gridValue = Double.NaN;

				LayerList ll = WWMapTools.this.wwCanvas.getModel().getLayers();
				outer: for (int i = ll.size() - 1; i >= 0; i--) {
					Layer l = ll.get(i);
					if (l.isEnabled() && (l.getOpacity() > .4))
						for (Class<?> c : l.getClass().getInterfaces())
							if (c.equals( QueryableGridLayer.class )) {
								QueryableGridLayer grid = (QueryableGridLayer) l;
								if (grid.getValueUnits() == null) continue;
								gridUnits = grid.getValueUnits().replaceAll("%", "%%");
								gridValue = grid.getValueAt(lat, lon);
								break outer;
							}
				}
			}
		}
		updateInfoLabel();
	}

	protected void updateInfoLabel() {
		String las = Double.isNaN(lat) ? ""
				: String.format("Lat: %7.3f\u00B0", lat);
		String los = Double.isNaN(lon) ? ""
				: String.format("Lon: %7.3f\u00B0", lon);
		String els = Double.isNaN(elev) ? ""
				: String.format("Elev: %7dm", (int) elev);
		String als = Double.isNaN(alt) ? ""
				: String.format("Alt: %7.0fkm", alt);
		String gridS = Double.isNaN(gridValue) ? ""
				: String.format("Z: %7.0f" + gridUnits, gridValue);

		info.setText("  " + las + "  " + los + "  " + els + "  " + als + "  " + gridS);
	}

	protected void save() {
		super.save();
	}

	protected void init() {
		box = Box.createHorizontalBox();

		save = new JButton(Icons.getIcon(Icons.SAVE,false));
		save.setPressedIcon(Icons.getIcon(Icons.SAVE,true));
		save.setBorder( BorderFactory.createRaisedBevelBorder() );
		save.setEnabled( true );
		save.addActionListener(this);
		save.setToolTipText("Save As");
		box.add(save);
//		box.add( Box.createHorizontalStrut(5));

		layerManagerB = new JButton( Icons.getIcon(Icons.LAYERS, false));
		layerManagerB.setPressedIcon(Icons.getIcon( Icons.LAYERS, true));
		layerManagerB.setBorder( BorderFactory.createRaisedBevelBorder());
		layerManagerB.setEnabled(true);
		layerManagerB.addActionListener(this);
		layerManagerB.setToolTipText("Layer Manager");
		box.add(layerManagerB);

		creditB = new JButton (Icons.getIcon( Icons.INFO, false));
		creditB.setPressedIcon( Icons.getIcon(Icons.INFO, true));
		creditB.setBorder( BorderFactory.createRaisedBevelBorder());
		creditB.setEnabled(true);
		creditB.addActionListener(this);
		creditB.setToolTipText("Credit");
		box.add(creditB);

		maskB = new JToggleButton ( Icons.getIcon( Icons.MASK,  false) );
		maskB.setPressedIcon( Icons.getIcon(Icons.MASK, true));
		maskB.setBorder( BorderFactory.createRaisedBevelBorder());
		maskB.setEnabled(true);
		maskB.addActionListener(this);
		maskB.setToolTipText("Mask");

		box.add(maskB);
		box.add( Box.createHorizontalStrut(5));
		box.add( new JLabel("VE: "));

		veSlider = new JSlider(0,100,4);
		veSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				wwCanvas.getSceneController().setVerticalExaggeration(veSlider.getValue() / 4.);
				veDisplay.setText((100 * veSlider.getValue() / 4) / 100f + "");
			}
		});
		veSlider.setMaximumSize(new Dimension(25,20));
		box.add(veSlider);

		veDisplay = new JLabel("1");
		box.add(veDisplay);

		box.add( Box.createHorizontalStrut(5));
		JPanel panel = new JPanel(new BorderLayout());

		processDisplay = new JLabel("Processing");
		processDisplay.setForeground( Color.black);
		processDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(processDisplay, BorderLayout.WEST);

		info = new JLabel();
		info.setForeground( Color.black);
		info.setBorder( BorderFactory.createEmptyBorder(1, 5, 1, 1));
		panel.add(info);

		box.add(panel);

		processTimer();
	}

	private void processTimer() {
		Timer processTimer = new Timer(100, new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
				java.awt.Color color = processDisplay.getForeground();
				int alpha = color.getAlpha();

				if (WorldWind.getRetrievalService().hasActiveTasks()) {
					if (alpha == 255)
						alpha = 255;
					else
						alpha = alpha < 16 ? 16 : Math.min(255, alpha + 20);
				}
				else {
					alpha = Math.max(0, alpha - 20);
				}
				processDisplay.setForeground(new java.awt.Color(255, 0, 0, alpha));
			}
		});
		processTimer.start();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == layerManagerB) {
			((WWMapApp) app).setLayerManagerVisible(true);
		}
		else if (evt.getSource() == creditB) {
			((WWMapApp) app).showCredit();
		}
		else if (evt.getSource() == maskB) {
			((WWMapApp) app).setMask( maskB.isSelected() );
		}
		else {
			super.actionPerformed(evt);
		}
	}

	public Cursor getCurrentCursor() {
		return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}
}
