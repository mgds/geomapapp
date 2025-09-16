package haxby.util;

import haxby.map.MapApp;
import haxby.map.XMap;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SceneGraph {
	private int y_divides;
	private int x_divides;
	private float units_y_per; 
	private float units_x_per;
	private int width;
	private int height;
	private double wrap;

	private final int density;
	private SGNode[][] root;

	public int total_icon_count = 0;

	private boolean flatLayer = false;

	public SceneGraph(XMap map, int density) {
		Dimension dim = map.getDefaultSize();
		this.wrap = map.getWrap();
		this.width = wrap > 0 ? (int) wrap : dim.width;
		this.height = dim.height;
		this.y_divides = 85;
		this.density = density;

		switch (((MapApp)map.getApp()).getMapType()) {
		case MapApp.SOUTH_POLAR_MAP:
		case MapApp.NORTH_POLAR_MAP:
			this.x_divides = 50;
		case MapApp.MERCATOR_MAP:
		default:
			this.x_divides = 82;
			break;
		}

		this.units_x_per = 1f * width / x_divides;
		this.units_y_per = 1f * height / y_divides;

		root = new SGNode[x_divides][y_divides];
	}

	public SceneGraph(int width, int height, int x_divides, int y_divides, double wrap, int density) {
		this.width = width;
		this.height = height;
		this.x_divides = x_divides;
		this.y_divides = y_divides;
		this.wrap = wrap;
		this.density = density;

		this.units_x_per = 1f * width / x_divides;
		this.units_y_per = 1f * height / y_divides;

		root = new SGNode[x_divides][y_divides];
	}

	public void addEntry(SceneGraphEntry entry) {
		int yIndex = getYIndex(entry.getY());
		int xIndex = getXIndex(entry.getX());

		if (yIndex == -1 || xIndex == -1) return;

		if (root[xIndex][yIndex] == null)
			createNode(xIndex, yIndex);

		root[xIndex][yIndex].addEntry(entry);

		total_icon_count++;
	}

	public void removeEntry(SceneGraphEntry entry) {
		int yIndex = getYIndex(entry.getY());
		int xIndex = getXIndex(entry.getX());

		if (root[xIndex][yIndex] != null)
			if (root[xIndex][yIndex].removeEntry(entry))
				total_icon_count--;
	}

	private void createNode(int xIndex, int yIndex) {
		Rectangle2D r = new Rectangle2D.Double(
				xIndex * units_x_per,
				yIndex * units_y_per,
				(xIndex + 1) * units_x_per,
				(yIndex + 1) * units_y_per);
		root[xIndex][yIndex] = new SGNode(r, 0);
	}

	  //We're translating the lon by 180 to get our zero-based index
	private int getXIndex(double x){
		if (wrap > 0)
			while (x > width) x -= wrap;

		if (x > width || x < 0)
			return -1;

		int index = (int) Math.floor(x / units_x_per); 
		if (index == x_divides)
			index--;
		return index;
	}

	//We're translating the lat by 90 to get our zero-based index
	private int getYIndex(double y){
		if (y > height)
			return y_divides - 1;
		else if (y < 0)
			return 0;
		
		int index = (int) Math.floor( y / units_y_per);
		if (index == y_divides) index--;

		return index;
	}

	public List<SceneGraphEntry> getAllEntries(Rectangle2D visibleArea) {
		int startY = getYIndex(visibleArea.getMinY());
		int startX = getXIndex(visibleArea.getMinX());
		int endY = getYIndex(visibleArea.getMaxY());
		int endX = getXIndex(visibleArea.getMaxX());

		if (visibleArea.getWidth() > width && wrap > 0) {
			startX = 0;
			endX = x_divides - 1;
		}

		List<SceneGraphEntry> entries = new LinkedList<SceneGraphEntry>();
		if (startX <= endX) {
			entries.addAll(
					getAllEntriesFromRange(startY, endY, startX, endX)
					);
		} else if (wrap > 0) {
			entries.addAll(
					getAllEntriesFromRange(startY, endY, startX, x_divides - 1)
					);
			entries.addAll(
					getAllEntriesFromRange(startY, endY, 0, endX)
					);
		}
		else {
			throw new RuntimeException("Illegal visible area");
		}
		return entries;
	}

	public List<SceneGraphEntry> getEntriesToDraw(Rectangle2D visibleArea, double zoom) {
		int startY = getYIndex(visibleArea.getMinY());
		int startX = getXIndex(visibleArea.getMinX());
		int endY = getYIndex(visibleArea.getMaxY());
		int endX = getXIndex(visibleArea.getMaxX());
		if (visibleArea.getWidth() > width && wrap > 0) {
			startX = 0;
			endX = x_divides - 1;
		}

		List<SceneGraphEntry> entries = new LinkedList<SceneGraphEntry>();

		if (startX <= endX) {
			entries.addAll(
					getEntriesFromRange(startY, endY, startX, endX, zoom)
					);
		} else if (wrap > 0) {		
			entries.addAll(
				getEntriesFromRange(startY, endY, startX, x_divides - 1, zoom)
					);
			entries.addAll(
					getEntriesFromRange(startY, endY, 0, endX, zoom)
					);
		}
		else {
			throw new RuntimeException("Illegal visible area");
		}
		return entries;
	}
	
	private List<SceneGraphEntry> getAllEntriesFromRange(int startY, int endY, int startX,
			int endX) {
		List<SceneGraphEntry> entries = new LinkedList<SceneGraphEntry>();

		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++)
				if (root[x][y] != null)
					retriveAllEntries(root[x][y], entries);
		}
		return entries;
	}

	private List<SceneGraphEntry> getEntriesFromRange(int startY, int endY, int startX,
			int endX, double zoom) {
		List<SceneGraphEntry> entries = new LinkedList<SceneGraphEntry>();

		for (int y = startY; y <= endY; y++) {
			for (int x = startX; x <= endX; x++) {
				if (root[x][y] != null) {
					retriveEntries(root[x][y], zoom, entries);
					//System.out.println("root[" + x + "] y[" + y + "] zoom " + zoom + " entries " + entries.size()  + root[x][y].getSector());
				}
			}
		}
		return entries;
	}

	private void retriveAllEntries(SGNode node, List<SceneGraphEntry> entries) {
		Iterator<SceneGraphEntry> iter = node.getEntries().iterator();
		while (iter.hasNext()) {
			SceneGraphEntry entry = iter.next();
			if (entry != null && entry.isVisible())
				entries.add(entry);
		}

		SGNode[] children = node.getChildren();
		if (children != null)
			for (int i = 0; i < children.length; i++)
				if (children[i] != null)
					retriveAllEntries(children[i], entries);
	}

	private void retriveEntries(SGNode node, double zoom, List<SceneGraphEntry> entries) {
		boolean needToSplit = 1 << node.level < zoom;

		Iterator<SceneGraphEntry> iter = node.getEntries().iterator();
		int addedCount = 0;
		while (iter.hasNext()) {
			SceneGraphEntry entry = iter.next();
	
			if (entry != null && entry.isVisible()) {
				entries.add(entry);
				addedCount++;
			}
		}

		if (!needToSplit && (addedCount < node.getEntryCount())) {
			int x = node.entries_per_node - addedCount;
			SGNode[] children = node.getChildren();
			if (children != null)
				for (int i = 0; i < children.length; i++)
					if (children[i] != null) {
						x = retriveXIcons(x, children[i], entries);
						if (x == 0) break;
	 				}
		}

		if (needToSplit) {
			SGNode[] children = node.getChildren();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					if (children[i] != null) {
						retriveEntries(children[i], zoom, entries);
					}
				}
			}
		}
	}
	
	private int retriveXIcons(int x, SGNode node, List<SceneGraphEntry> entries) {
		Iterator<SceneGraphEntry> iter = node.getEntries().iterator();
		while (iter.hasNext()) {
			SceneGraphEntry entry = iter.next();
			if (entry != null && entry.isVisible()) {
				entries.add(entry);
				if (--x == 0) return 0;
			}
		}

		SGNode[] children = node.getChildren();
		if (children != null)
			for (int i = 0; i < children.length; i++)
				if (children[i] != null) {
				x = retriveXIcons(x, children[i], entries);
					if (x == 0) return 0;
				}
		return x;
	}

	public static interface SceneGraphEntry {
		public int getID();
		public double getX();
		public double getY();
		public boolean isVisible();
	}

	private class SGNode {
		private int entries_per_node;
		private int level;
		private List<SceneGraphEntry> entries = new LinkedList<SceneGraphEntry>();
		private SGNode[] children;
		private Rectangle2D sector;
		private double centerLat, centerLon; 

		public SGNode(Rectangle2D sector, int level) {
			this.sector = sector;
			this.level = level;
			this.entries_per_node = density << level;
			this.centerLon = sector.getCenterX();
			this.centerLat = sector.getCenterY();
		}

		public List<SceneGraphEntry> getEntries() {
			return entries;
		}

		public int getEntryCount() {
			return entries.size();
		}
		
		public Rectangle2D getSector() {
			return sector;
		}

		public SGNode[] getChildren() {
			return children;
		}

		public void addEntry(SceneGraphEntry entry) {
			// Node not full
			int count = getEntryCount();
			if (count < entries_per_node) {
				getEntries().add(entry);
				return;
			}

			if (children == null) 
				createChildren();

			// Add Child
			double lat = entry.getY();
			double lon = entry.getX();

			int index;
			if (lat < centerLat)
				if (lon < centerLon)
					index = 0;
				else 
					index = 1;
			else
				if (lon < centerLon)
					index = 2;
				else
					index = 3;

			addEntryToChild(entry, index);
		}

		public boolean removeEntry(SceneGraphEntry entry) {
			List<SceneGraphEntry> list = getEntries();
			int index = list.indexOf(entry);
			if (index != -1) {
				list.remove(index);
				return true;
			}

			if (children != null) {
				double lat = entry.getY();
				double lon = entry.getX();

				if (lat < centerLat)
					if (lon < centerLon)
						index = 0;
					else 
						index = 1;
				else
					if (lon < centerLon)
						index = 2;
					else
						index = 3;

				if (children[index] != null)
					return children[index].removeEntry(entry);
			}
			return false;
		}

		private void addEntryToChild(SceneGraphEntry entry, int childIndex) {
			if (children[childIndex] == null) {
				Rectangle2D r2 = null;
				switch (childIndex) {
				case 0:
					r2 = new Rectangle2D.Double(sector.getX(), sector.getY(), sector.getWidth() / 2, sector.getHeight() / 2);
				case 1:
					r2 = new Rectangle2D.Double(sector.getCenterX(), sector.getY(), sector.getWidth() / 2, sector.getHeight() / 2);
				case 2:
					r2 = new Rectangle2D.Double(sector.getX(), sector.getCenterY(), sector.getWidth() / 2, sector.getHeight() / 2);
				case 3:
					r2 = new Rectangle2D.Double(sector.getCenterX(), sector.getCenterY(), sector.getWidth() / 2, sector.getHeight() / 2);
				}
				children[childIndex] = new SGNode(r2 , level + 1);
			}

			children[childIndex].addEntry(entry);
		}

		private void createChildren(){
			children = new SGNode[4];
		}
	}

	public void setFlatLayer(boolean tf) {
		flatLayer = tf;
	}

	public boolean isAFlatLayer() {
		return flatLayer;
	}

	public void clearScene() {
		for (int i = 0; i < root.length; i++)
			for (int j = 0; j < root[i].length; j++)
				emptyNode(root[i][j]);
	}

	private void emptyNode(SGNode node) {
		if (node == null) return;
		
		node.entries.clear();
		if (node.children != null)
			for (SGNode child : node.children)
				emptyNode(child);
	}
}
