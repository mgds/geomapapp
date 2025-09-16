package haxby.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.Layer;
import haxby.map.XMap;
import haxby.util.BrowseURL;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.geomapapp.util.Icons;

public class LayerManager extends JPanel {
	
	public static interface LayerListListener
	{
		public void remove(ILayer l);
		public void up(ILayer l);
		public void down(ILayer l);
	}
	
	public List<LayerListListener> listeners 
		= new LinkedList<LayerListListener>();
	
	protected List<LayerPanel> layerPanels = new LinkedList<LayerPanel>();
	protected List<Layer> ignoreLayers = new LinkedList<Layer>();
	
	protected Zoomer zoomer;
	
	public LayerManager(Zoomer zoomer) {
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		this.zoomer = zoomer;
	}
	
	@Override
	public Dimension getMaximumSize() {
		int width = 0;
		for (LayerPanel lp : layerPanels)
			width = Math.max(lp.prefered_width, width);
		
		return new Dimension( width == 0 ? 83 : width,
							50 * getComponentCount() + 50);
	}
	
	public void remove(ILayer l)
	{
		for (LayerListListener listener : listeners)
			listener.remove(l);
	}
	
	public void up(ILayer l)
	{
		for (LayerListListener listener : listeners)
			listener.up(l);
	}
	
	public void down(ILayer l)
	{
		for (LayerListListener listener : listeners)
			listener.down(l);	
	}
	
	public void layerStateChanged()
	{
		for (LayerPanel p : layerPanels)
			p.stateChanged();
	}
	
	public void setLayerList(List<ILayer> layers) {
		this.removeAll();
		this.layerPanels.clear();
		
		for (int i = layers.size() - 1; i  >= 0; i--) {
			ILayer layer = layers.get(i);
			if (ignoreLayers.contains(layer.getLayer()))
				continue;
			
			
			LayerPanel p = new LayerPanel(layer);
			add(p);
			
			this.layerPanels.add(p);
		}
		
		this.setMaximumSize(getPreferredSize());
		this.revalidate();
		this.repaint();
	}
	
	private class LayerPanel extends JPanel
	{
		public ILayer layer;
		
		private JCheckBox visible;
		private JSlider slider;
		
		private int prefered_width;
		
		public LayerPanel(ILayer layer) {
			this.layer = layer;
			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			
			this.setLayout(gb);

			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			
			c.weightx = 5;
			c.gridwidth = GridBagConstraints.RELATIVE;
			
			String displayName = layer.getName();
			if (displayName.length() >= 40) {
				displayName = displayName.substring(0, 40);
			}
			
			visible = new JCheckBox(displayName, layer.isVisible());
			visible.setFont(new Font("Arial", Font.BOLD, 11));
			visible.setMaximumSize(new Dimension(500,100));
			visible.setBorderPainted(false);
			visible.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LayerPanel.this.layer.setVisible(((JCheckBox)e.getSource()).isSelected());
				}
			});
			gb.setConstraints(visible, c);
			add(visible);
			
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			
			JButton up = new JButton("\u039B");
			up.setMargin(new Insets(1,1,1,1));
			up.setFont(new Font("Arial", Font.BOLD, 11));
			up.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					up(LayerPanel.this.layer);
				}
			});
			gb.setConstraints(up, c);
			add(up);
			
			Box box = new Box(BoxLayout.X_AXIS);
			box.add(Box.createHorizontalStrut(2));
			
			JButton remove = createButton(Icons.CLOSE);
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LayerManager.this.remove(LayerPanel.this.layer);
				}
			});
			box.add(remove);
			
			String layerURLString = layer.getInfoURL();
			if ( layerURLString != null && layerURLString.length() > 0 ) {
				JButton infoB = createButton(Icons.INFO);
				infoB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BrowseURL.browseURL(LayerPanel.this.layer.getInfoURL());
					}
				});
				box.add(infoB);
			}
			
			String legendURLString = layer.getLegendURL();
			if ( legendURLString != null && legendURLString.length() > 0 ) {
				JButton infoB = createButton(Icons.LEGEND);
				infoB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BrowseURL.browseURL(LayerPanel.this.layer.getLegendURL());
					}
				});
				box.add(infoB);
			}
			
			//Gets WESN location and Zoom Icon Button
			double[] wesn = layer.getWESN();
			if (wesn != null) 
			{
				JButton zoomB = createButton(Icons.ZOOM_IN);
				zoomB.setToolTipText("Zoom To");
				zoomB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						zoomer.zoomToWESN(LayerPanel.this.layer.getWESN());
					}
				});
				box.add(zoomB);
			}
			
			c.insets = new Insets(0,1,0,1);
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.anchor = GridBagConstraints.CENTER;
			c.gridwidth = 1;
			gb.setConstraints(box, c);
			add(box);
			
			c.insets = new Insets(0,0,0,0);
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.gridwidth = 1;
			JLabel l = new JLabel(" Opacity:");
			l.setFont(new Font("Arial", Font.PLAIN, 11));
			gb.setConstraints(l, c);
			add(l);
			
			c.weightx = 5;
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.RELATIVE;
			slider = new JSlider(0,100);
			slider.setFont(new Font("Arial", Font.PLAIN, 11));
			slider.setSize(4, 4);
			slider.setValue((int) (layer.getOpacity() * 100));
			slider.setToolTipText("Opacity");
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					LayerPanel.this.layer.setOpacity(
						((JSlider) e.getSource()).getValue() / 100f);
				}
			});
			
			gb.setConstraints(slider, c);
			add(slider);
			
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.LINE_END;
			JButton down = new JButton("V");
			down.setMargin(new Insets(1,1,1,1));
			down.setFont(new Font("Arial", Font.PLAIN, 11));
			down.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					down(LayerPanel.this.layer);
				}
			});
			gb.setConstraints(down, c);
			add(down);

			setBorder( BorderFactory.createLineBorder(Color.black));
			
			prefered_width = gb.preferredLayoutSize(this).width;
			
			int minWidth = getMinimumSize().width;
			int minHeight = getMinimumSize().height;
			int maxWidth = getMaximumSize().width;
			
			setPreferredSize(new Dimension(minWidth, minHeight));
			setMaximumSize(new Dimension(maxWidth, minHeight));
			
			setColor();
		}
		
		private JButton createButton(int icon) {
			JButton button = new JButton( Icons.getIcon(icon,false));
			button.setPressedIcon( Icons.getIcon(icon, true) );
			button.setDisabledIcon( Icons.getDisabledIcon( icon, false ));
			button.setBorder( BorderFactory.createLineBorder(Color.black));
			button.setMargin(new Insets(1,0,1,0));
			return button;
		}

		public void setColor() {
			Color c = Color.LIGHT_GRAY;
			this.setBackground(c);
			for (Component comp : this.getComponents())
				if (!(comp instanceof JButton))
					comp.setBackground(c);
		}

		public void stateChanged() {
			slider.setValue((int)(layer.getOpacity() * 100));
			visible.setSelected(layer.isVisible());
			
			setColor();
		}
	}
	
	public static interface ILayer
	{
		public String getName();
		public double[] getWESN();
		public String getInfoURL();
		public String getLegendURL();
		public boolean isVisible();
		public double getOpacity();
		public void setVisible(boolean tf);
		public void setOpacity(double opacity); 
		public Object getLayer();
	}
	
	public static class WWILayer implements ILayer
	{
		private Layer layer;
		private String infoURL;
		private String legendURL;
		private double[] wesn;
		
		public boolean equals(Object obj) {
			if (obj instanceof WWILayer) {
				WWILayer that = (WWILayer) obj;
				return this.layer == that.layer;
			}
			return false;
		} 
		
		public WWILayer(Layer layer){
			this.layer = layer;
		}
		
		public void setInfoURL(String infoURL) {
			this.infoURL = infoURL;
		}
		
		public void setLegendURL(String legendURL) {
			this.legendURL = legendURL;
		}
		
		public String getLegendURL() {
			return legendURL;
		}
		
		public String getInfoURL() {
			return infoURL;
		}
		
		public String getName() {
			return layer.getName();
		}

		public double getOpacity() {
			return layer.getOpacity();
		}

		public boolean isVisible() {
			return layer.isEnabled();
		}

		public void setOpacity(double opacity) {
			layer.setOpacity(opacity);
			layer.firePropertyChange(AVKey.LAYER, null, layer);
		}

		public void setVisible(boolean tf) {
			layer.setEnabled(tf);
			layer.firePropertyChange(AVKey.LAYER, null, layer);
		}
		
		public Object getLayer() {
			return layer;
		}

		public void setWESN(double[] wesn) {
			this.wesn = wesn;
		}

		public double[] getWESN() {
			return wesn;
		}
	}
	
	public void addIgnoredLayer(Layer layer) {
		ignoreLayers.add(layer);
	}
	
	private interface Zoomer {
		public void zoomToWESN(double[] wesn);
	}
	
	public static class XMapZoomer implements Zoomer {
		private XMap map;
		public XMapZoomer(XMap map) {
			this.map = map;
		}
		public void zoomToWESN(double[] wesn) {
			map.zoomToWESN(wesn);
		}
	}

	public void setZoomer(Zoomer zoomer) {
		this.zoomer = zoomer;
	}
}
