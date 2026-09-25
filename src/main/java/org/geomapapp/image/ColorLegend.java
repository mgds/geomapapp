package org.geomapapp.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.JPanel;

public class ColorLegend extends JPanel {
	private ColorScaleTool scaleTool;
	//private ColorHistogram cHistogram;
	//private float[] data;
	private Color[] colors;

	public ColorLegend(ColorScaleTool t) {
		scaleTool = t;
	}

	public void paintComponent( Graphics g ) {
		g.setColor(Color.WHITE);
		Dimension d = this.getSize();
		g.fillRect(d.width/2, 0, d.width/2, d.height);
		this.setBackground(Color.WHITE);
		colors = new Color[scaleTool.scaler.colorZAL.size()];

		//System.out.println("size " + scaleTool.scaler.colorZAL.size() + " r1o " + (float)scaleTool.scaler.colorZAL.get(0) + " r1 " + scaleTool.scaler.zHist.getRange()[0] + " r2o " + (float)scaleTool.scaler.colorZAL.get(colors.length-1) + " r2 "+  scaleTool.scaler.zHist.getRange()[1] + " color " + colors.length);
		float range = Math.abs((float)scaleTool.scaler.colorZAL.get(0)-(float)scaleTool.scaler.colorZAL.get(colors.length-1));
		//float range2 = (float)scaleTool.scaler.zHist.getRange()[1] - (float)scaleTool.scaler.zHist.getRange()[0];
		DecimalFormat df = new DecimalFormat("0.000E0");
		String sciNot = df.format(range);

		int exponent=Integer.parseInt(sciNot.split("E")[1]);

		for(int i=0; i<colors.length; i++) {
			//System.out.println(scaleTool.scaler.colorZAL.get(i));
			colors[i] = new Color(scaleTool.scaler.palette.getRGB(scaleTool.scaler.colorZAL.get(i)));
		}
		boolean hasDecimals;
		DecimalFormat fmt;
		if(range<10) {
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

		int x = d.height/colors.length;
		boolean hasZero = false;
		for (int i=0; i<colors.length ; i++ ) {
			//System.out.println(colors[i].toString());
			scaleTool.scaler=scaleTool.scaler;
			int roundOff = (int)Math.pow(10, exponent-1);

			if(roundOff != 1) {
				roundOff/=2;
			}

			if(i%(colors.length/10)==0) {
				g.setColor(Color.BLACK);
				float num = (float)scaleTool.scaler.colorZAL.get(i);

				if(!hasDecimals) {

					int roundedNum = (int) (Math.round((num / roundOff))) * roundOff;

					if(roundedNum==0)
						hasZero = true;

					g.drawString(fmt.format(roundedNum), (d.width/2)+1,d.height - (i*x));
					
				} else {
					if(num==0 && hasZero) {
						hasZero = true;
						continue;
					} else {

						if(num==0)
							hasZero=true;

						g.drawString(fmt.format(num), (d.width/2)+1,d.height - (i*x));
					}
				}
			}

			if((scaleTool.scaler.colorZAL.get(i) -(scaleTool.scaler.colorZAL.get(i)%roundOff) ==0)&&!hasZero) {
				//System.out.println("draw a zero");
				g.setColor(Color.BLACK);
				g.drawString("0", (d.width/2)+1,d.height - (i*x));
				hasZero = true;
			}
			g.setColor(colors[i]);
			g.fillRect(0,d.height - (i*x), d.width/2, x);
		}
		g.setColor(Color.WHITE);
	}
}