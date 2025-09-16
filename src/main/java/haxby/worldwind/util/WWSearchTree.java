package haxby.worldwind.util;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import haxby.map.XMap;
import haxby.util.SearchTree;
import haxby.worldwind.WWMapApp;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.geomapapp.util.XML_Menu;

public class WWSearchTree extends SearchTree {

	public WWSearchTree(String searchURL) {
		super(searchURL);
	}

	public WWSearchTree(XML_Menu searchMenu) {
		super(searchMenu);
	}

	public WWSearchTree(DefaultMutableTreeNode rootNode) {
		super(rootNode);
	}

	public WWSearchTree(DefaultMutableTreeNode rootNode, int xpos, int ypos) {
		super(rootNode, xpos, ypos);
	}

	protected SearchTree createSearchTree(DefaultMutableTreeNode rootNode,
			int xpos, int ypos) {
		return new WWSearchTree(rootNode, xpos, ypos);
	}

	protected SearchTreeOverlay createSearchTreeOverlay() {
		return new WWSearchTreeOverlay();
	}

	public class WWSearchTreeOverlay extends SearchTreeOverlay {
		protected RenderableLayer layer = new RenderableLayer();
		protected Map<SearchTreeItem, Polyline> polylines = new
			HashMap<SearchTreeItem, Polyline>();

		protected List<SearchTreeItem> orderedList 
			= new ArrayList<SearchTreeItem>();

		protected Font font = new Font("SansSerif", Font.PLAIN, 10);
		protected int dx, dy;

		public WWSearchTreeOverlay() {
			DefaultMutableTreeNode rootNode = 
				(DefaultMutableTreeNode) tree.getModel().getRoot();
			Enumeration<?> nodes = rootNode.depthFirstEnumeration();
			while (nodes.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				if (!node.isLeaf()) continue;

				XML_Menu obj = (XML_Menu) node.getUserObject();
				if (obj == null) continue;
				if (obj.wesn == null) continue;
				String[] wesnS = obj.wesn.split(",");
				if (wesnS.length != 4) continue;

				float[] wesn = new float[4];
				try {
				for (int i = 0; i < wesn.length; i++) {
					wesn[i] = Float.parseFloat(wesnS[i]);
				}
				} catch (NumberFormatException ex) { continue; }

				Rectangle2D.Double bounds = new Rectangle2D.Double();

				bounds.x = wesn[0];
				bounds.y = wesn[2];
				bounds.width = wesn[1] - wesn[0];
				bounds.height = wesn[3] - wesn[2];

				if (Double.isNaN(bounds.width) ||
						Double.isNaN(bounds.height) ||
						Double.isNaN(bounds.x) ||
						Double.isNaN(bounds.y) ||
						Double.isInfinite(bounds.width) ||
						Double.isInfinite(bounds.height) ||
						Double.isInfinite(bounds.x) ||
						Double.isInfinite(bounds.y)) {
					continue;
				}

				if (bounds.width * bounds.height == 0) {
					continue;
				}

				// The plus 20 is to catch less than boundary cases.
				// We don't want to show Global Grids
				if (bounds.width + 20 > 360) continue;

				SearchTreeItem sti = new SearchTreeItem(bounds, node);
				orderedList.add(sti);
			}

			Collections.sort(orderedList, new Comparator<SearchTreeItem>() {
				public int compare(SearchTreeItem o1, SearchTreeItem o2) {
					double a1 = o1.bounds.width * o1.bounds.height;
					double a2 = o2.bounds.width * o2.bounds.height;
					if (a1 > a2)
						return 1;
					else if (a1 < a2)
						return -1;
					else 
						return 0;
				}
			});
			for (SearchTreeItem item : orderedList) {
				Rectangle2D.Double bounds = item.bounds;
				List<Position> latLons = new LinkedList<Position>();

				int[] indices = new int[] {0,0,
						0,1,
						1,1,
						1,0};

				for (int i = 0; i < indices.length; i+=2) {
					latLons.add( new Position(
							LatLon.fromDegrees(
									bounds.y + indices[i+1] * bounds.height,
									bounds.x + indices[i] * bounds.width), 
							0));
				}

				Polyline polyline = new Polyline(latLons);
				polyline.setLineWidth(1f);
				polyline.setClosed(true);
				polyline.setFollowTerrain(true);
				polyline.setPathType(Polyline.LINEAR);
				if (isNodeSelected(item.node))
					polyline.setColor(Color.red);
				layer.addRenderable(polyline);

				polylines.put(item, polyline);
			}

			((WWMapApp) mapApp).makeLayerVisible(layer, true);

			tree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					for (SearchTreeItem item : polylines.keySet()) {
						int size = 1;
						Color c = Color.white;
						if (isNodeSelected(item.node)) {
							c = Color.red;
							size = 2;
						}
						Polyline poly = polylines.get(item);
						poly.setColor(c);
						poly.setLineWidth(size);
					}
					layer.firePropertyChange(AVKey.LAYER, null, layer);
				}
			});
		}

		private boolean isNodeSelected(DefaultMutableTreeNode node) {
			Enumeration path = node.pathFromAncestorEnumeration(
					(TreeNode) tree.getModel().getRoot());
			List<Object> pathL = new LinkedList<Object>();
			while (path.hasMoreElements())
				pathL.add(path.nextElement());

			return tree.getSelectionModel().isPathSelected( 
					new TreePath( pathL.toArray() ));
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			Position pos = ((WWMapApp) mapApp).getCanvas().getView().computePositionFromScreenPoint(e.getX(), e.getY());

			int startI = -1;

			if (tree.getSelectionPath() != null) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) 
					tree.getSelectionPath().getLastPathComponent();

				if (selectedNode.isLeaf()) {
					int j = 0;
					for (SearchTreeItem sti : orderedList) {
						if (sti.node == selectedNode) {
							if (select(sti.bounds, pos))
								startI = j;
							break;
						} 
						j++;
					}
				}
			}

			startI++;

			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
			
			for (int i = 0; i < orderedList.size(); i++) 
			{
				SearchTreeItem sti = orderedList.get((startI + i) % orderedList.size());
				
				if (select(sti.bounds, pos))
				{
					Enumeration<?> pathFrom = sti.node.pathFromAncestorEnumeration(rootNode);
					List<Object> pathL = new LinkedList<Object>();
					while (pathFrom.hasMoreElements()) 
						pathL.add(pathFrom.nextElement());
					TreePath treePath = new TreePath(pathL.toArray());

					tree.clearSelection();
					tree.getSelectionModel().setSelectionPath(treePath);
					tree.scrollPathToVisible(treePath);

					e.consume();
					return;
				}
			}

			if (startI != 0) return;

			tree.getSelectionModel().clearSelection();
		}

		private boolean select(Rectangle2D bounds, Position pos) {
			return bounds.contains(pos.longitude.degrees, pos.latitude.degrees);
		}
	}

	@Override
	public void makeOverlayCurrent(SearchTreeOverlay overlay,
			JCheckBox placesCB, XMap map) {
		removeCurrentOverlay(map);

		InputHandler ih = ((WWMapApp) mapApp).getCanvas().getInputHandler();
		ih.addMouseListener(overlay);

		((WWMapApp) mapApp).makeLayerVisible(
				((WWSearchTreeOverlay)overlay).layer);
		placesCB.setSelected(true);

		map.repaint();

		currentOverlay = overlay;
		currentShowPlacesCB = placesCB;
	}

	public void removeCurrentOverlay(XMap map) {
		if (currentOverlay != null ) {
			((WWMapApp) mapApp).hideLayer(
				((WWSearchTreeOverlay)currentOverlay).layer);

			InputHandler ih = ((WWMapApp) mapApp).getCanvas().getInputHandler();
			ih.removeMouseListener(currentOverlay);

			currentOverlay = null;
		}

		if (currentShowPlacesCB != null) {
			currentShowPlacesCB.setSelected(false);
			currentShowPlacesCB = null;
		}
	}
}
