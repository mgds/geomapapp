package haxby.worldwind.layers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWIcon;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/***
 * A WWSceneGraph represents a scene of SceneItems
 * It divides the world into NUM_LAT_DIVIDES by NUM_LON_DIVIDES
 * root nodes. When an item is added it is placed in one of these nodes.
 * If the node has reached it's capacity, 4 sub nodes are created by dividing the 
 * node's sector into equal portions and then placing the new node in the appropriate 
 * sector.  When querying the nodes for items to draw the needToSplit method is used
 * to determine if the Node's children should be visited as well.  Each node's capacity
 * is based on its level.  The capacity grows by maginitudes of two.
 * 
 * A WWSceneGraph supports multiple layers of items so that one layer does not
 * infringe upon a second layers capacity.  If no sublayer is specified the
 * layer is assumed to be layer 0.  Use createSubLayer() to request a subLayer key
 * and disposeSubLayer(subLayer) to dispose of all the items associated with a 
 * subLayer
 * 
 * @author justinc
 */
public class WWSceneGraph extends AbstractLayer {

	public static final int MAX_DRAWN_ITEMS = 2000;	
	
	private static final int NUM_LAT_DIVIDES = 30;
	private static final int NUM_LON_DIVIDES = 60;
	private static final int DEGREES_LAT_PER = 180 / NUM_LAT_DIVIDES;
	private static final int DEGREES_LON_PER = 360 / NUM_LON_DIVIDES;
	private static final int PIXEL_STEP = 10;
	private static final double ANGLETHRESHOLD = 80;
	
	private SGNode[][] root = new SGNode[NUM_LON_DIVIDES][NUM_LAT_DIVIDES];
	
	private Sector previouslyDisplayedSector; //This is not the View's sector, just the displayed portion
	private List<SceneItem> previouslyRenderedItems;
	
	private double splitScale = 2.2;
	private double density = 20;

	private HashSet<Integer> flatScenes = new HashSet<Integer>();
	
	public int total_item_count = 0;
	
	public int subLayers = 0;

	public WWSceneGraph() {
	}
	
	public int createSubLayer()
	{
		return subLayers++ + 1;
	}
	
	public void disposeSubLayer(int subLayer)
	{
		for (SGNode[] nodeRow : root)
			if (nodeRow != null)
				for (SGNode node : nodeRow)
					if (node != null)
						node.disposeSubLayer(subLayer);
		
		flatScenes.remove(subLayer);
		
		// Invalidate state
		previouslyDisplayedSector = null;
	}
	
	public void addItem(SceneItem item)
	{
		addItem(item, 0);
	}
	
	public void addItem(SceneItem item, int subLayer)
	{
		
		int latIndex = getLatIndex(item.getPosition().getLatitude(), false);
		int lonIndex = getLonIndex(item.getPosition().getLongitude());
		
		if (root[lonIndex][latIndex] == null)
			createNode(lonIndex, latIndex);

		root[lonIndex][latIndex].addItem(item, subLayer);
		
		total_item_count++;
		
		// This invalidates our saved state to force a redraw
		previouslyDisplayedSector = null;
	}
	
	public void removeItem(SceneItem item)
	{
		removeItem(item, 0);
	}
	
	public void removeItem(SceneItem item, int subLayer)
	{
		int latIndex = getLatIndex(item.getPosition().getLatitude(), false);
		int lonIndex = getLonIndex(item.getPosition().getLongitude());
		
		if (root[lonIndex][latIndex] != null) {
			if (root[lonIndex][latIndex].removeItem(item, subLayer))
				total_item_count--;
		
			// This invalidates our saved state to force a redraw
			previouslyDisplayedSector = null;
		}
	}
	
	private void createNode(int lonIndex, int latIndex) {
		Sector s = Sector.fromDegrees(
				latIndex * DEGREES_LAT_PER - 90,
				(latIndex + 1) * DEGREES_LAT_PER - 90, 
				lonIndex * DEGREES_LON_PER - 180,
				(lonIndex + 1) * DEGREES_LON_PER - 180);
		
		root[lonIndex][latIndex] = new SGNode(s, 0);
	}

	@Override
	protected void doPick(DrawContext dc, java.awt.Point pickPoint) {
		for (SceneItem item : computeItemsInFieldOfView(dc))
			item.pick(dc, pickPoint, this);
	}
	
	@Override
	protected void doRender(DrawContext dc) {
		for (SceneItem item : computeItemsInFieldOfView(dc))
			item.render(dc, this.getOpacity());
	} 

	private Iterable<SceneItem> computeItemsInFieldOfView(DrawContext dc) {
		List<SceneItem> itemsInFieldOfView = null;
		LatLon[] samples = getSamples(dc);
		
		//Let's use a Sector to wrap those samples, using this to compare the viewport's content
		//because many rendering requests will come following mouse movements, so we don't want to
		//uselessly recompute stuff we've done before
		Sector currentlyVisibleSector = Sector.boundingSector(Arrays.asList(samples));
		if (currentlyVisibleSector.equals(previouslyDisplayedSector)) itemsInFieldOfView = previouslyRenderedItems;
		else {
			itemsInFieldOfView = new LinkedList<SceneItem>();
			//Let's analyse those samples
			Arrays.sort(samples,new Comparator<LatLon>(){
				public int compare(LatLon p1, LatLon p2) {
					return Double.compare(p1.getLongitude().getDegrees(),p2.getLongitude().getDegrees());
				}
			});
			
			//Typical situation
			if (samples[samples.length-1].getLongitude().getDegrees() - samples[0].getLongitude().getDegrees() < 180){
				Angle startLon = samples[0].getLongitude();
				Angle endLon = samples[samples.length-1].getLongitude();
				itemsInFieldOfView.addAll(getItems(dc, startLon, endLon, samples));
				
			 // Add all the flat scenes items now
				for (int subLayer : flatScenes) 
					itemsInFieldOfView.addAll(getAllItems(subLayer, dc, startLon,endLon,samples));
			} else { //Crossing the dateline, or looking at the pole
				//The lowest positive longitude
				Angle startLon = null;
				for (int i=0;i<samples.length;i++){
					if (samples[i].getLongitude().getDegrees() > 0){
						startLon = samples[i].getLongitude();
						break;
					}
				}
				//The highest negative longitude
				Angle endLon = null;
				for (int i=samples.length-1;i>=0;i--){
					if (samples[i].getLongitude().getDegrees() < 0){
						endLon = samples[i].getLongitude();
						break;
					}
				}
				
				// Traverse the Nodes
				itemsInFieldOfView.addAll(getItems(dc, startLon,endLon,samples));
//				iconsInFieldOfView.addAll(getIcons(dc, startLon,Angle.POS180,samples));
//				iconsInFieldOfView.addAll(getIcons(dc, Angle.NEG180,endLon,samples));
				
				// Add all the flat scenes icons now
				for (int subLayer : flatScenes) 
				{
					itemsInFieldOfView.addAll(getAllItems(subLayer, dc, startLon,endLon,samples));
//					iconsInFieldOfView.addAll(getAllIcons(subLayer, dc, startLon,Angle.POS180,samples));
//					iconsInFieldOfView.addAll(getAllIcons(subLayer, dc, Angle.NEG180,endLon,samples));
				}
			}
			if (itemsInFieldOfView.size() > MAX_DRAWN_ITEMS)
				itemsInFieldOfView = itemsInFieldOfView.subList(0, MAX_DRAWN_ITEMS);
			
			this.previouslyDisplayedSector = currentlyVisibleSector;
		}
		this.previouslyRenderedItems = itemsInFieldOfView;
		return itemsInFieldOfView;
	}

	private Collection<SceneItem> getAllItems(int subLayer,
			DrawContext dc, Angle startLon, Angle endLon, LatLon[] samples) {
		List<SceneItem> items = new LinkedList<SceneItem>();
		
		//Let's get the latitude range
		Angle[] latitudes = getLatitudeRange(samples);
		
		//We round for the polar cap for the latitudes closest to the pole
		int startLatIndex = getLatIndex(latitudes[0],latitudes[0].getDegrees()<0);
		int endLatIndex = getLatIndex(latitudes[1],latitudes[1].getDegrees()>0);
		
		int startLonIndex = getLonIndex(startLon);
		int endLonIndex = getLonIndex(endLon);
		
		//Check if we are looking at a pole, in which case we need the whole range
		if (startLatIndex == 0 || endLatIndex == NUM_LAT_DIVIDES-1) {
			startLonIndex = 0;
			endLonIndex = NUM_LON_DIVIDES-1;
		}

		LatLon center = dc.getViewportCenterPosition();
		final int midLonIndex = getLonIndex(center.getLongitude());
		final int midLatIndex = getLatIndex(center.getLatitude(), false);

		List<int[]> nodes = new LinkedList<int[]>();
		
		if (startLon.degrees > endLon.degrees) {
			for (int lat = startLatIndex; lat <= endLatIndex; lat++)
			{
				int midAIndex = getLonIndex(Angle.POS180);
				int midBIndex = getLonIndex(Angle.NEG180);
				for (int lon = startLonIndex; lon <= midAIndex; lon++)
					nodes.add(new int[] {lon,lat});
				for (int lon = midBIndex; lon <= endLonIndex; lon++)
					nodes.add(new int[] {lon,lat});
			}
		}
		else
			for (int lat = startLatIndex; lat <= endLatIndex; lat++)
				for (int lon = startLonIndex; lon <= endLonIndex; lon++)
					nodes.add(new int[] {lon,lat});
		
		Collections.sort(nodes, new Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				int dx = Math.abs(a[0] - midLonIndex);
				if (dx >= NUM_LON_DIVIDES / 2)
					dx = NUM_LON_DIVIDES - dx;
				
				double d1 = Math.pow(dx, 2) + 
							Math.pow(a[1] - midLatIndex, 2);
				
				dx = Math.abs(b[0] - midLonIndex);
				if (dx > NUM_LON_DIVIDES / 2)
					dx = NUM_LON_DIVIDES - dx;
				
				double d2 = Math.pow(dx, 2) + 
							Math.pow(b[1] - midLatIndex, 2);
				return (int) (10 * (d1 - d2));
			}
		});
		
		for (int[] node : nodes)
			if (root[node[0]][node[1]] != null) {
				retriveAllItems(subLayer, dc, root[node[0]][node[1]], items);
				if (items.size() > MAX_DRAWN_ITEMS)
					break;
			}
		return items;
	}

	private Collection<SceneItem> getItems(DrawContext dc, Angle startLon, Angle endLon,
			LatLon[] samples) {
		List<SceneItem> items = new LinkedList<SceneItem>();
		
		//Let's get the latitude range
		Angle[] latitudes = getLatitudeRange(samples);
		
		//We round for the polar cap for the latitudes closest to the pole
		int startLatIndex = getLatIndex(latitudes[0],latitudes[0].getDegrees()<0);
		int endLatIndex = getLatIndex(latitudes[1],latitudes[1].getDegrees()>0);
		
		int startLonIndex = getLonIndex(startLon);
		int endLonIndex = getLonIndex(endLon);
		
		//Check if we are looking at a pole, in which case we need the whole range
		if (startLatIndex == 0 || endLatIndex == NUM_LAT_DIVIDES-1) {
			startLonIndex = 0;
			endLonIndex = NUM_LON_DIVIDES-1;
		}
		
		final int midLonIndex;
		final int midLatIndex;
		
		Position centerPosition = dc.getViewportCenterPosition();
		if (centerPosition != null)
		{
			LatLon center = centerPosition;
			midLonIndex = getLonIndex(center.getLongitude());
			midLatIndex = getLatIndex(center.getLatitude(), false);
		} else
		{
			midLatIndex = (startLatIndex + endLatIndex) / 2;
			if (startLon.degrees > endLon.degrees)
			{
				int midAIndex = getLonIndex(Angle.POS180);
				int index = (startLatIndex + endLatIndex + midAIndex) / 2;
				if (index > midAIndex)
					index -= midAIndex;
				midLonIndex = index;
			} else
				midLonIndex = (startLonIndex + endLonIndex) / 2;

		}

		List<int[]> nodes = new LinkedList<int[]>();
		
		if (startLon.degrees > endLon.degrees) {
			int midAIndex = getLonIndex(Angle.POS180);
			int midBIndex = getLonIndex(Angle.NEG180);
			for (int lat = startLatIndex; lat <= endLatIndex; lat++)
			{
				for (int lon = startLonIndex; lon <= midAIndex; lon++)
					nodes.add(new int[] {lon,lat});
				for (int lon = midBIndex; lon <= endLonIndex; lon++)
					nodes.add(new int[] {lon,lat});
			}
		}
		else
			for (int lat = startLatIndex; lat <= endLatIndex; lat++)
				for (int lon = startLonIndex; lon <= endLonIndex; lon++)
					nodes.add(new int[] {lon,lat});
		
		Collections.sort(nodes, new Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				int dx = Math.abs(a[0] - midLonIndex);
				if (dx >= NUM_LON_DIVIDES / 2)
					dx = NUM_LON_DIVIDES - dx;
				
				double d1 = Math.pow(dx, 2) + 
							Math.pow(a[1] - midLatIndex, 2);
				
				dx = Math.abs(b[0] - midLonIndex);
				if (dx > NUM_LON_DIVIDES / 2)
					dx = NUM_LON_DIVIDES - dx;
				
				double d2 = Math.pow(dx, 2) + 
							Math.pow(b[1] - midLatIndex, 2);
				return (int) (10 * (d1 - d2));
			}
		});

		for (int[] node : nodes)
			if (root[node[0]][node[1]] != null) {
				retriveItems(dc, root[node[0]][node[1]], items);
				if (items.size() > MAX_DRAWN_ITEMS)
					break;
			}
		return items;
	}

	private void retriveItems(DrawContext dc, SGNode node, List<SceneItem> items) {
		boolean needToSplit = needToSplit(dc, node.getSector());
		
		for (Entry<Integer, List<SceneItem>> entry : node.items.entrySet()) {
			if (flatScenes.contains(entry.getKey()))
				continue;
			
			List<SceneItem> list = entry.getValue();
			int addedCount = 0;

			for (SceneItem item : list) {
				if (item != null && item.isVisible())
				{
					items.add(item);
					addedCount++;
				}
			}
			
			if (!needToSplit && (addedCount < list.size()))
			{
				int x = node.items_per_node - addedCount;
				SGNode[] children = node.getChildren();
				if (children != null)
					for (SGNode child : children)
						if (child != null) {
							x = retriveXItems(x, entry.getKey(), dc, child, items);
							if (x == 0) break;
						}
			}
		}
		
		if (needToSplit)
		{
			SGNode[] children = node.getChildren();
			if (children != null)
				for (SGNode child : children)
					if (child != null)
						retriveItems(dc, child, items);
		}
	}
	
	private int retriveXItems(int x, int subLayer, DrawContext dc, SGNode node, List<SceneItem> items) {
		for (SceneItem item : node.getItems(subLayer))
			if (item != null && item.isVisible()) {
				items.add(item);
				if (--x == 0) return 0;
			}
		
		SGNode[] children = node.getChildren();
		if (children != null)
			for (SGNode child : children)
				if (child != null) {
					x = retriveXItems(x, subLayer, dc, child, items);
					if (x == 0) return 0;
				}
		
		return x;
	}
	
	private void retriveAllItems(int subLayer, DrawContext dc, SGNode node, List<SceneItem> items)
	{
		List<SceneItem> list = node.items.get(subLayer);
		
		if (list != null)
			for (SceneItem item : list)
				if (item != null && item.isVisible())
					items.add(item);
		
		SGNode[] children = node.getChildren();
		if (children != null)
			for (SGNode child : children)
				if (child != null)
					retriveAllItems(subLayer, dc, child, items);
	}

	private boolean needToSplit(DrawContext dc, Sector sector)
	{
		Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());
		Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

		View view = dc.getView();
		double d1 = view.getEyePoint().distanceTo3(corners[0]);
		double d2 = view.getEyePoint().distanceTo3(corners[1]);
		double d3 = view.getEyePoint().distanceTo3(corners[2]);
		double d4 = view.getEyePoint().distanceTo3(corners[3]);
		double d5 = view.getEyePoint().distanceTo3(centerPoint);

		double minDistance = d1;
		if (d2 < minDistance)
			minDistance = d2;
		if (d3 < minDistance)
			minDistance = d3;
		if (d4 < minDistance)
			minDistance = d4;
		if (d5 < minDistance)
			minDistance = d5;

		double cellSize = (Math.PI * sector.getDeltaLatRadians() * dc.getGlobe().getRadius()) / this.density ; // TODO

		return !(Math.log10(cellSize) <= (Math.log10(minDistance) - this.splitScale ));
	}
	
	private Angle[] getLatitudeRange(final LatLon[] corners) {
		Angle[] latitudes = new Angle[2];
		Arrays.sort(corners,new Comparator<LatLon>(){
			public int compare(LatLon p1, LatLon p2) {
				return Double.compare(p1.getLatitude().getDegrees(),p2.getLatitude().getDegrees());
			}
			
		});
		latitudes[0] = corners[0].getLatitude();
		latitudes[1] = corners[corners.length-1].getLatitude();
		return latitudes;
	}
	
  //We're translating the lon by 180 to get our zero-based index
	private int getLonIndex(Angle a){
		int index = (int)Math.floor(a.getDegrees()+180.0) / DEGREES_LON_PER; 
		if (index == NUM_LON_DIVIDES)
			index--;
		return index;
	}
	
	//We're translating the lat by 90 to get our zero-based index
	private int getLatIndex(Angle a,boolean roundForCap){
		//Let's show the whole polar cap if it's in the vicinity, otherwise we end up seeing the curvature
		if (roundForCap){
			if (a.getDegrees() < - ANGLETHRESHOLD) return 0;
			else if (a.getDegrees() > ANGLETHRESHOLD) return NUM_LAT_DIVIDES - 1;
		} 
		
		int index = (int)Math.floor(a.getDegrees()+90) / DEGREES_LAT_PER;
		if (index == NUM_LAT_DIVIDES) index--;
		
		return index;
	}
	
	private LatLon[] getSamples(DrawContext dc) {
		Rectangle rec = dc.getView().getViewport();
		Position upperLeft = scanForPosition(dc,0,0);
		Position upperRight = scanForPosition(dc,rec.width,0);
		Position lowerLeft = scanForPosition(dc,0,rec.height);
		Position lowerRight = scanForPosition(dc,rec.width,rec.height);
		Position middleTop = scanForPosition(dc,rec.width/2,0);
		Position middleBottom = scanForPosition(dc,rec.width/2,rec.height);
		Position middleUpThird = scanForPosition(dc,rec.width/2,rec.height/3);
		Position middle = scanForPosition(dc,rec.width/2,rec.height/2-1);
		
		return new LatLon[]{
			upperLeft,
			upperRight,
			lowerLeft,
			lowerRight,
			middleTop,
			middleBottom,
			middleUpThird,
			middle
		};
	}
	
	private Position scanForPosition(DrawContext dc, int startX, int startY){
		Position pos = null;
		int x = startX;
		int y = startY;
		Rectangle viewPort = dc.getView().getViewport();
		boolean fromLeft = startX < viewPort.width/2;
		boolean fromTop = startY<viewPort.height/2;
		
		while (pos == null && x > -1 && x <= viewPort.width){
			//Are we scanning from top or from bottom towards the middle?
			while (fromTop ? y < viewPort.height : y > viewPort.height/2){
				pos = dc.getView().computePositionFromScreenPoint(x,y);
				if (pos != null) break;
				else {
					if (fromTop) y += PIXEL_STEP;
					else y -= PIXEL_STEP;
				}
			}
			if (pos == null) {//Nothing found
				if (fromLeft) x += PIXEL_STEP;
				else x -= PIXEL_STEP;
				y = startY;
			}
		}
		if (pos == null) {
			throw new IllegalStateException("oops, nothing found!");
		}
		return pos;
	}

	private static class SGNode 
	{
		private int items_per_node;
		private int level;
		private HashMap<Integer, List<SceneItem>> items 
			= new HashMap<Integer, List<SceneItem>>();
		private SGNode[] children;
		private Sector sector;
		private double centerLat, centerLon; 

		public SGNode(Sector sector, int level) {
			this.sector = sector;
			this.level = level;
			this.items_per_node = 2 << level;
			
			this.centerLat = sector.getCentroid().getLatitude().degrees;
			this.centerLon = sector.getCentroid().getLongitude().degrees;
		}
		
		public void disposeSubLayer(int subLayer) {
			items.remove(subLayer);
			if (children != null)
				for (SGNode node : children)
					if (node != null)
						node.disposeSubLayer(subLayer);
		}

		public List<SceneItem> getItems() {
			return getItems(0);
		}
		
		public List<SceneItem> getItems(int subLayer) {
			List<SceneItem> list = items.get(subLayer);
			if (list == null) {
				list = new LinkedList<SceneItem>();
				items.put(subLayer, list);
			}
			
			return list;
		}

		public int getItemCount() {
			return getItemCount(0);
		}
		
		public int getItemCount(int subLayer) {
			return getItems(subLayer).size();
		}
		
		public Sector getSector() {
			return sector;
		}
		
		public SGNode[] getChildren() {
			return children;
		}
		
		public void addItem(SceneItem item, int subLayer) {
			// Node not full
			int count = getItemCount(subLayer);
			if (count < items_per_node)
			{
				getItems(subLayer).add(item);
				return;
			}
			
			if (children == null) 
				createChildren();
			 
			// Add Child
			Position p = item.getPosition();
			double lat = p.getLatitude().degrees;
			double lon = p.getLongitude().degrees;
			
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
			
			addItemToChild(item, index, subLayer);
		}
		
		public boolean removeItem(SceneItem item, int subLayer) {
			List<SceneItem> list = getItems(subLayer);
			int index = list.indexOf(item);
			if (index != -1) {
				list.remove(index);
				return true;
			}
				
			if (children != null)
			{
				Position p = item.getPosition();
				double lat = p.getLatitude().degrees;
				double lon = p.getLongitude().degrees;
				
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
					return children[index].removeItem(item, subLayer);
			}
			return false;
		}
		
		private void addItemToChild(SceneItem item, int childIndex, int subLayer) {
			if (children[childIndex] == null)
				children[childIndex] = new SGNode(sector.subdivide()[childIndex], level + 1);

			children[childIndex].addItem(item, subLayer);
		}

		private void createChildren(){
			children = new SGNode[4];
		}
	}

	public void makeFlatLayer(int subLayer) {
		flatScenes.add(subLayer);
		previouslyDisplayedSector = null;
	}

	public void makeDeepLayer(int subLayer) {
		flatScenes.remove(subLayer);
		previouslyDisplayedSector = null;
	}

	public boolean isAFlatScene(int layerKey) {
		return flatScenes.contains(layerKey);
	}

	public void clearPreviousScan() {
		previouslyDisplayedSector = null;
	}

	public static interface SceneItem
	{
		public void render(DrawContext dc, double opacity);
		public void pick(DrawContext dc, Point pickPoint, Layer layer);
		public Position getPosition();
		public boolean isVisible();
	}
	
	public static class SceneItemIcon implements SceneItem
	{
		private DetailedIcon icon;
		private DetailedIconRenderer renderer;
		
		public SceneItemIcon(DetailedIcon icon, DetailedIconRenderer renderer)
		{
			this.icon = icon;
			this.renderer = renderer;
		}
		
		public Position getPosition() {
			return icon.getPosition();
		}
		
		public boolean isVisible() {
			return icon.isVisible();
		}
		
		public void pick(DrawContext dc, Point pickPoint, Layer layer) {
			LinkedList<WWIcon> icons = new LinkedList<WWIcon>();
			icons.add(icon);
			renderer.pick(dc, icons, pickPoint, layer);
		}
		
		public void render(DrawContext dc, double opacity) {
			LinkedList<WWIcon> icons = new LinkedList<WWIcon>();
			icons.add(icon);
			renderer.render(dc, icons, opacity);
		}
	}
}
