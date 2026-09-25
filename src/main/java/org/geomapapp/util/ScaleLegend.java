package org.geomapapp.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

import haxby.map.XMap;


public class ScaleLegend extends JPanel {

	private SymbolScaleTool scaleTool;


	public ScaleLegend(SymbolScaleTool t) {
		scaleTool = t;
	}

	public void paintComponent( Graphics g ) {
		//g.setColor(Color.WHITE);
		Dimension d = this.getSize();
		float [] range = scaleTool.scaler.getRange();	
		int symbolSize = 100;
		double scale = symbolSize / 50.;
		XMap map = scaleTool.getMap();
		double mapZoom = map.getZoom();
		//when we zoom in on the map, the symbols on the legend 
		//will also change size. Since we are not changing the legend
		//window size, the legend zoom is different to the factor used
		//in UnkownDataSet.
		double legzoom = Math.pow( mapZoom, .25 );

		
		this.setBackground(Color.WHITE);
		//show this many symbols on the legend
		float numSymbols = 6;
		//determine spacing in both data range and plot location
		float spacing = (float) (d.width / (numSymbols + 1));
		float rangeSpacing = (range[1] - range[0]) / (numSymbols - 1);
		float r;
		
		Graphics2D g2d = (Graphics2D)g;
		g2d.setStroke(new BasicStroke(1));
		AffineTransform at = g2d.getTransform();
		
		//determine label formatting - taken from the ColorLegend code
		ArrayList<Float> scaleZAL = scaleTool.scaler.getScaleZAL();
		float xrange = Math.abs((float)scaleZAL.get(0)-(float)scaleZAL.get(scaleZAL.size()-1));
		//float xrange = Math.abs(range[0]-range[1]);
		DecimalFormat df = new DecimalFormat("0.000E0");
		String sciNot = df.format(xrange);
		int exponent=Integer.parseInt(sciNot.split("E")[1]);
		boolean hasDecimals;
		DecimalFormat fmt;
		if(xrange<10) {
			int numberOfAmpersands = (-1)*exponent;
			String ampersandStr = "#.";
			for(int i=0;i<numberOfAmpersands+1;i++) {
				ampersandStr = ampersandStr.concat("#");
			}
			fmt = new DecimalFormat(ampersandStr);
			hasDecimals = true;
		}else {
			fmt = new DecimalFormat("#");
			hasDecimals = false;
		}

		//plot symbols in legend
		for (int i=0; i<numSymbols; i++) {
			
			float num = range[0] + i * rangeSpacing;
			
			//apply rounding to make legend values nicer
			int roundOff = (int)Math.pow(10, exponent-1);
			if(roundOff != 1) {
				roundOff/=2;
			}
			if(!hasDecimals) {
				num = (int) (Math.round((num / roundOff))) * roundOff;
			}
			
			//determine radius of each symbol
			r = (float) (scale * 1.5 * legzoom * (scaleTool.getSizeRatio(num)));
			
			//plot the sized symbol on the legend
			float x = spacing * (i + 1);
			float y = d.height/2;
			Ellipse2D.Double circle = new Ellipse2D.Double(-r, -r, r * 2, r * 2);
			g2d.translate(x, y);
			g2d.setColor(Color.LIGHT_GRAY);
			g2d.fill(circle);
			g2d.setColor(Color.BLUE);
			g2d.draw(circle);
			g2d.setTransform(at);

			g2d.setColor(Color.BLACK);
			drawStringCenter(g2d, fmt.format(num), x, d.height - 5);

		}
	}
	private void drawStringCenter (Graphics2D g2d, String s, float x, int y) {
		int stringLen = (int) g2d.getFontMetrics().getStringBounds(s, g2d).getWidth();
	    g2d.drawString(s, x - stringLen/2, y);
	}
}
