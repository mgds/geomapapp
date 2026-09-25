package haxby.grid;

import haxby.db.all.AccessAllData;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.TreeNode;

import org.geomapapp.util.ParseLink;
import org.geomapapp.util.XML_Menu;

public class ContributedGridsOverlay implements Overlay, MouseListener {

	protected MapApp mapApp;
	protected XMap map;
	protected AccessAllData dataAccessor;

	protected String[] names;
	protected Rectangle2D.Double[] gridBounds;
	protected int selectedIndex = -1;

	protected static Font font = new Font("SansSerif", Font.PLAIN, 10);
	protected int dx, dy;

	public ContributedGridsOverlay(MapApp mapApp, AccessAllData dataAccessor, XMap map, int mapType) {
		this.map = map;
		this.mapApp = mapApp;
		this.dataAccessor = dataAccessor;

		switch (mapType) {
		default:
		case MapApp.MERCATOR_MAP:
			dx = 0;
			dy = 260;
			break;
		case MapApp.SOUTH_POLAR_MAP:
		case MapApp.NORTH_POLAR_MAP:
			dx = 320;
			dy = 320;
			break;
		}

		TreeNode root = dataAccessor.top;
		TreeNode node = null;

		for (int i = 0; i < root.getChildCount(); i++) {
			node = root.getChildAt(i);
			if ("General Data Viewers".equals(node.toString()))
				break;
			else
				node = null;
		}

		if (node == null)
			return;

		root = node;

		for (int i = 0; i < root.getChildCount(); i++) {
			node = root.getChildAt(i);
			if ("Layers".equals(node.toString()))
				break;
			else
				node = null;
		}

		if (node == null)
			return;

		root = node;

		Hashtable<String, Rectangle2D.Double> namesToBounds = new Hashtable<String, Rectangle2D.Double>();
		
		for (int i = 0; i < root.getChildCount(); i++) {
			node = root.getChildAt(i);
			addContributedGrids(namesToBounds, node);
		}

		int i = 0;
		names = new String[namesToBounds.size()];
		gridBounds = new Rectangle2D.Double[namesToBounds.size()];
		
		for (Entry<String, Rectangle2D.Double> entry : namesToBounds.entrySet()) {
			names[i] = entry.getKey();
			gridBounds[i] = entry.getValue();
			i++;
		}

		// Sort by area
		for (i = 0; i < gridBounds.length; i++) {
			int smallest = i;
			double smallestArea = gridBounds[i].width * gridBounds[i].height;
			for (int j = i + 1; j < gridBounds.length; j++) {
				double area = gridBounds[j].width * gridBounds[j].height;
				if (area < smallestArea) {
					smallestArea = area;
					smallest = j;
				}
			}

			String tmpName = names[i];
			Rectangle2D.Double tmpBounds = gridBounds[i];
			names[i] = names[smallest];
			gridBounds[i] = gridBounds[smallest];
			names[smallest] = tmpName;
			gridBounds[smallest] = tmpBounds;
		}
	}

	public ContributedGridsOverlay(MapApp mapApp, XMap map, int mapType) {
		this.map = map;
		this.mapApp = mapApp;
		this.dataAccessor = null;

		switch (mapType) {
		default:
		case MapApp.MERCATOR_MAP:
			dx = 0;
			dy = 260;
			break;
		case MapApp.SOUTH_POLAR_MAP:
		case MapApp.NORTH_POLAR_MAP:
			dx = 320;
			dy = 320;
			break;
		}

		Vector<String> namesV = new Vector<String>();
		Vector<Rectangle2D.Double> gridBoundsV = new Vector<Rectangle2D.Double>();
		int i = 0;

		addContributedGrids(namesV, gridBoundsV, XML_Menu.possibleGrids);

		names = new String[namesV.size()];
		gridBounds = new Rectangle2D.Double[namesV.size()];

/*
		for ( int k = 0; k < XML_Menu.possibleGrids.size(); i++ ) {
		}
*/

		while ( i < namesV.size() ) {
			names[i] = namesV.get(i);
			gridBounds[i] = gridBoundsV.get(i);
			i++;
		}

		// Sort by area
		for (i = 0; i < gridBounds.length; i++) {
			int smallest = i;
			double smallestArea = gridBounds[i].width * gridBounds[i].height;
			for (int j = i + 1; j < gridBounds.length; j++) {
				double area = gridBounds[j].width * gridBounds[j].height;
				if (area < smallestArea) {
					smallestArea = area;
					smallest = j;
				}
			}

			String tmpName = names[i];
			Rectangle2D.Double tmpBounds = gridBounds[i];
			names[i] = names[smallest];
			gridBounds[i] = gridBounds[smallest];
			names[smallest] = tmpName;
			gridBounds[smallest] = tmpBounds;
		}
	}

	private void addContributedGrids(Map<String, Rectangle2D.Double> namesToBounds, TreeNode root) {
		if (root.isLeaf())
			addContributedGrid(namesToBounds, root);
		else
			for (int i = 0; i < root.getChildCount(); i++) {
				TreeNode node = root.getChildAt(i);
				addContributedGrids(namesToBounds, node);
			}
	}

//	private void addContributedGrids(Map<String, Rectangle2D.Double> namesToBounds, Vector possibleGrids) {
//		for (int i = 0; i < possibleGrids.size(); i++) {
//			addContributedGrids(namesToBounds, node);
//		}
//	}

	private void addContributedGrid(Map<String, Rectangle2D.Double> namesToBounds, TreeNode node) {
		String name = node.toString();
		String urlStr = (String) dataAccessor.layerNameToURL.get(name);

		if (urlStr == null)
			return;

		int i = urlStr.lastIndexOf("/") + 1;
		String urlPath = urlStr.substring(0, i);
		String urlName = urlStr.substring(i);
		urlName = urlName.substring(0, urlName.lastIndexOf("."));

		BufferedReader reader = null;
		try {
			URL url = URLFactory.url(urlPath + urlName + ".link");
			InputStream in = url.openStream();
			reader = new BufferedReader(
					new InputStreamReader(in));
			Vector properties = ParseLink.parse(reader, null);
			Vector props = (Vector) ParseLink.getProperty(properties, "data");
			String linkType = (String) ParseLink.getProperty(props, "type");

			if (linkType.equals("tiled_grid")) {
				double xmin = java.lang.Double.parseDouble((String) ParseLink
						.getProperty(props, "x_min"));
				double xmax = java.lang.Double.parseDouble((String) ParseLink
						.getProperty(props, "x_max"));
				double ymin = java.lang.Double.parseDouble((String) ParseLink
						.getProperty(props, "y_min"));
				double ymax = java.lang.Double.parseDouble((String) ParseLink
						.getProperty(props, "y_max"));
				namesToBounds.put(name, 
						new Rectangle2D.Double(xmin + dx, ymin + dy, xmax - xmin, ymax - ymin));
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}
	}
	public static void main( String[] args) {
		JDialog loadingDialog = new JDialog();
		JPanel textPanel = new JPanel(new BorderLayout());
		JLabel textLabel = new JLabel("wArra");
		textLabel.setText("Wslsls");
		textLabel.setToolTipText("dlkas");
		textPanel.add(textLabel);
		loadingDialog.add(textPanel);
		loadingDialog.pack();
		loadingDialog.setSize(300,100);
		loadingDialog.setVisible(true);
	}

	private void addContributedGrids(Vector<String> namesV, Vector<Rectangle2D.Double> gridBoundsV, Vector possibleGrids) {
		if (possibleGrids == null) return;

		int size = possibleGrids.size();
		for ( int j = 0; j < size; j++ ) {
			String urlString = ((String[])possibleGrids.get(j))[1];

			int indexOfSlash = urlString.lastIndexOf("/");
			if (indexOfSlash == -1) continue;
			String urlPath = urlString.substring(0,indexOfSlash+1);
			int indexOfDot = urlString.lastIndexOf(".");
			if (indexOfDot == -1 || indexOfDot < indexOfSlash) continue;

			String urlName = urlString.substring(indexOfSlash + 1, indexOfDot);
			String name = ((String[])possibleGrids.get(j))[0];
			BufferedReader reader = null;
			try {
				URL url = URLFactory.url(urlPath + urlName + ".link");
				InputStream in = url.openStream();
				reader = new BufferedReader(
						new InputStreamReader(in));
				Vector properties = ParseLink.parse(reader, null);
				Vector props = (Vector) ParseLink.getProperty(properties, "data");
				String linkType = (String) ParseLink.getProperty(props, "type");

				if (linkType.equals("tiled_grid")) {
					double xmin = java.lang.Double.parseDouble((String) ParseLink
							.getProperty(props, "x_min"));
					double xmax = java.lang.Double.parseDouble((String) ParseLink
							.getProperty(props, "x_max"));
					double ymin = java.lang.Double.parseDouble((String) ParseLink
							.getProperty(props, "y_min"));
					double ymax = java.lang.Double.parseDouble((String) ParseLink
							.getProperty(props, "y_max"));

					namesV.add(name+":::"+urlPath+urlName);
					gridBoundsV.add(new Rectangle2D.Double(xmin + dx, ymin + dy, xmax - xmin, ymax - ymin));
				}
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
					}
			}
		}
	}

	public void draw(Graphics2D g) {
		for (int i = 0; i < names.length; i++)
		{
			draw(g, i);
		}
		
		draw(g, selectedIndex);
	}

	public void draw(Graphics2D g, int i) {
		if (i == -1) return;

		String name = names[i].substring(0,names[i].indexOf(":::"));
		Rectangle2D.Double bounds = gridBounds[i];
		bounds = new Rectangle2D.Double(
				bounds.x, 
				bounds.y,
				bounds.width,
				bounds.height);

		double wrap = map.getWrap();
		double zoom = map.getZoom();

		if (wrap > 0) {
			// The plus 20 is to catch less than boundary cases.
			// We don't want to show Global Grids
			if (bounds.width + 20 > wrap) return;
		}

		boolean selected = selectedIndex == i;

		Rectangle2D.Double r = (Rectangle2D.Double)map.getClipRect2D();
		g.setColor( selected ? Color.white : Color.black);
		g.setStroke( new BasicStroke(1f/(float) zoom));
		float size = 9f/(float) zoom;
		Font dFont = font.deriveFont(size );
		g.setFont( dFont );

		boolean drawName = zoom > 2;

		if( wrap>0 ) {
			while( bounds.x>r.x+r.width)bounds.x-=wrap;
			while( bounds.x+bounds.width<r.x)bounds.x+=wrap;
			while( bounds.x<r.x+r.width) {
				g.draw(bounds);
				if (drawName) {
					float x = .1f*size+(float)(bounds.x);
					float y = -.2f*size+(float)(bounds.y);

					if (selected) {
						TextLayout tl = new TextLayout(name, dFont, g.getFontRenderContext());

						g.setColor(Color.white);

						double border = 2 / zoom;

						Rectangle2D stringBounds = tl.getBounds();
						stringBounds.setRect(
								x + stringBounds.getX() - border, 
								y + stringBounds.getY() - border, 
								stringBounds.getWidth() + 2 * border,
								stringBounds.getHeight() + 2 * border);
						g.fill(stringBounds);

						g.setColor(Color.black);
						
						g.draw(stringBounds);
					}

					g.drawString(name, x, y);
				}
				bounds.x+=wrap;
			}
		} else {
			g.draw(bounds);
			if (drawName) {
				float x = .1f*size+(float)(bounds.x);
				float y = -.2f*size+(float)(bounds.y);

				if (selected) {
					TextLayout tl = new TextLayout(name, dFont, g.getFontRenderContext());

					g.setColor(Color.white);

					double border = 2 / zoom;

					Rectangle2D stringBounds = tl.getBounds();
					stringBounds.setRect(
							x + stringBounds.getX() - border, 
							y + stringBounds.getY() - border, 
							stringBounds.getWidth() + 2 * border,
							stringBounds.getHeight() + 2 * border);
					g.fill(stringBounds);
					g.setColor(Color.black);
					g.draw(stringBounds);
				}
				g.drawString(name, 
					.1f*size+(float)(bounds.x),
					-.2f*size+(float)(bounds.y));
			}
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.isConsumed() || e.isControlDown())
			return;

		if (e.getButton() == MouseEvent.BUTTON3 || (System.getProperty("os.name").toLowerCase().contains("mac") && e.isControlDown()) ) {
			System.out.println("Loading Contributed Grid");
			loadSelection();
			return;
		}

		Point2D point = map.getScaledPoint( e.getPoint() );
		double wrap = map.getWrap();
		if (wrap > 0)
			while (point.getX() > wrap)
				point.setLocation(point.getX() - wrap, point.getY());

		if (!select(selectedIndex, point))
			selectedIndex = -1;

		int i;
		for (i = selectedIndex + 1; i < names.length; i++) {
			if (select(i, point)) {
				selectedIndex = i;
				map.repaint();
				return;
			}
		}
		for (i = 0; i < selectedIndex + 1; i++) {
			if (select(i, point)) {
				selectedIndex = i;
				map.repaint();
				return;
			}
		}
		selectedIndex = -1;
		map.repaint();
	}

/*
	private void loadSelection() {
		if (selectedIndex == -1)
			return;

		String url = (String) 
			dataAccessor.layerNameToURL.get(
					names[selectedIndex]);

		if (url != null)
			mapApp.addShapeFile(url);
	}
*/

	private void loadSelection() {
		if (selectedIndex == -1)
			return;

		String url = names[selectedIndex].substring(names[selectedIndex].indexOf(":::")+3)+".shp";
		XML_Menu temp = XML_Menu.menuHash.get(names[selectedIndex].replace(":::","")+".shp");

		System.out.println(url);
		if (url != null)
			mapApp.addShapeFile(url,temp);
	}

	public boolean select(int i, Point2D point) {
		if (i == -1) return false;

		Rectangle2D.Double bounds = gridBounds[i];

		double wrap = map.getWrap();

		if (wrap > 0) {
			// The plus 20 is to catch less than boundary cases.
			// We don't want to show Global Grids
			if (bounds.width + 20 > wrap) return false;
		} else if (mapApp.getMapType() != MapApp.MERCATOR_MAP) {
			if (bounds.width >= 640 &&
					bounds.height >= 640)
				return false;
		}

		Rectangle2D.Double r = (Rectangle2D.Double) map.getClipRect2D();
		if (wrap > 0)
			while (r.x > wrap)
				r.x -= wrap;

		if (bounds.x < r.x &&
				bounds.y < r.y &&
				bounds.getMaxX() > r.getMaxX() &&
				bounds.getMaxY() > r.getMaxY())
			return false;

		return bounds.contains( point );
	}

	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
}
