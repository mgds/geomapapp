package haxby.db.fms;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;

public class FMSDraw {
	private static ArrayList<float[]> data;
	private static int object = 0;

	private static int slices = 20;
	private static float[][] circle;
	static 
	{
		circle = new float[slices][2];
		for (int i = 0 ; i < slices; i++) {
			double theta = i * Math.PI * 2 / slices;
			circle[i][0] = (float) Math.cos(theta);
			circle[i][1] = (float) Math.sin(theta);
		}
	}

	static double null_axis_dip(float strike1,float dip1,float strike2, float dip2){
		double den;

		den = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(dip1))*Math.sin(Math.toRadians(dip2))*Math.sin(Math.toRadians(strike1-strike2))));

		if(den<0.)
			den=-den;
		return den;
	}

	static double null_axis_strike(float strike1,float dip1,float strike2, float dip2){
		double phn,cosphn,sinphn;
		double sd1,cd1,sd2,cd2,ss1,cs1,ss2,cs2;

		sd1 = Math.sin(Math.toRadians(dip1));
		cd1 = Math.cos(Math.toRadians(dip1));

		sd2 = Math.sin(Math.toRadians(dip2));
		cd2 = Math.cos(Math.toRadians(dip2));

		ss1 = Math.sin(Math.toRadians(strike1));
		cs1 = Math.cos(Math.toRadians(strike1));

		ss2 = Math.sin(Math.toRadians(strike2));
		cs2 = Math.cos(Math.toRadians(strike2));

		cosphn = sd1*cs1*cd2 - sd2*cs2*cd1;
		sinphn = sd1*ss1*cd2 - sd2*ss2*cd1;

		if(Math.sin(Math.toRadians(strike1-strike2))<0.0){
			cosphn=-cosphn;
			sinphn=-sinphn;
		}

		phn = Math.toDegrees(Math.atan2(sinphn, cosphn));

		if(phn<0)
			phn+=360.0;

		return phn;
	}

	static double proj_radius(float strike1,float dip1,float str){
		double dip,r;

		if(Math.abs(dip1-90.0)<.0001){
			r = (str == strike1 || str == (strike1 + 180.0))?1.0:0.0;
		}
		else{
			dip = (Math.atan(Math.tan(Math.toRadians(dip1)) * Math.sin(Math.toRadians(str-strike1))));
			r = Math.sqrt(2.0) * Math.sin((Math.PI/4.0) - (dip/2.0));
		}
		return r;
	}

	static double zero_360 (double str)
	{	/* By Genevieve Patau: put an angle between 0 and 360 degrees */
		if (str >= 360.0)
			str -= 360.0;
		else if (str < 0.0)
			str += 360.0;
		return (str);
	}

	public static void drawFMS(Graphics2D g,
						float scale,
						float strike,
						float dip, 
						float rake,
						float strike2,
						float dip2,
						float rake2)
	{
		double pos_NP1_NP2 = Math.sin(Math.toRadians(strike-strike2));	
		double fault = rake==0.0?(rake2/Math.abs(rake2)):(rake/Math.abs(rake));
		double radius;
		float str;

		double n_axis_dip = null_axis_dip(strike,dip,strike2,dip2);
		double n_axis_strike;

		if(Math.abs(90.0-n_axis_dip)<.0001)
			n_axis_strike = strike;
		else
			n_axis_strike = null_axis_strike(strike,dip,strike2,dip2);

		// Fill white backing
		GeneralPath circle = getPath(rotate(0), scale);
		g.setColor(Color.WHITE);
		g.fill(circle);

		//GeneralPath path;

		double increment;
		double[] xCo = new double[1000];
		double[] yCo = new double[1000];

		if((Math.abs(pos_NP1_NP2)<.0001)){
			int i = -1;

			str = strike;
			while(str<=(strike+180.0)){
				i++;
				radius = proj_radius(strike,dip,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str++;
			}
			if(Math.abs(fault+1.0)<.0001){
				str = strike+180.0f;
				while(str>=strike){
					i++;
					double si = Math.sin(Math.toRadians(str));
					double co = Math.cos(Math.toRadians(str));
					xCo[i]= si*scale;
					yCo[i]= co*scale;
					str--;
				}
				drawCoOrdinates(xCo,yCo,i,g);
			}
			str = strike2;
			i=-1;
			while(str<=(strike2+180.0)){
				i++;
				radius = proj_radius(strike2,dip2,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str++;
			}
			if(Math.abs(fault-1.0)<.0001){
				drawCoOrdinates(xCo,yCo,i,g);
			}
			else{
				str = strike2+180.0f;
				while(str>=(strike2+180.0)){
					i++;
					double si = Math.sin(Math.toRadians(str));
					double co = Math.cos(Math.toRadians(str));
					xCo[i]= si*scale;
					yCo[i]= co*scale;
					str--;
				}
				drawCoOrdinates(xCo,yCo,i,g);
			}
		}

		//pure strike slip
		else if(((90.0 - dip)<.0001)&&((90.0-dip2)<.0001)){
			increment = (Math.abs(rake)<.0001)?1.0:-1.0;
			int i =0;
			str = strike;
			while(increment == 1 ? (str <=(strike+90.0)):(str >=(strike-90.0))){
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*scale;
				yCo[i]= co*scale;
				str+=increment;
				i++;
			}
			xCo[i]=0;
			yCo[i]=0;
			drawCoOrdinates(xCo,yCo,i,g);
			i=0;
			str = strike+180.0f;
			while(increment == 1 ? (str <=(strike+270.0)):(str >=(strike+90.0))){
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*scale;
				yCo[i]= co*scale;
				str+=increment;
				i++;
			}
			xCo[i]=0;
			yCo[i]=0;
			drawCoOrdinates(xCo,yCo,i,g);
		}
		else {
			int i=-1;
			increment = 1;

			if(strike > n_axis_strike)
				strike -=360.0;

			str = strike;

			while((Math.abs(90.0-dip)<.0001)?(str<=strike):(str<=n_axis_strike)){
				i++;
				radius = proj_radius(strike,dip,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str+=increment;
			}

			//second nodal plane from null axis
			strike2+= ((1.0+fault)*90.0);
			if(strike2>=360)
				strike2-=360.0;

			increment = fault;
			if((fault*(strike2-n_axis_strike))<-.0001)
				strike2 += (fault*360.0);

			str = (float) ((Math.abs(90.0-dip2)<.0001)?strike2:n_axis_strike);

			while((increment==1.0)?(str<=strike2):(str>=strike2)){
				i++;
				radius = proj_radius((float)(strike2-(1.0+fault)*90.0),dip2,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str+=increment;
			}
			//close the first compression part
			strike = (float) zero_360(strike);
			strike2 = (float) zero_360(strike2);

			increment = pos_NP1_NP2 >=0. ? -fault:fault;

			if((increment * (strike-strike2)) < -.0001)
				strike += increment*360.0;

			str = strike2;

			while(increment == 1. ? (str<=strike):(str>=strike)){
				i++;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*scale;
				yCo[i]= co*scale;
				str+=increment;
			}
			drawCoOrdinates(xCo,yCo,i,g);
			i=-1;
			//first nodal plane till null axis
			strike = (float) zero_360(strike+180.0);
			if((strike-n_axis_strike)<-.0001)
				strike+=360;

			increment = -1;
			str = strike;

			while(((Math.abs(90.0-dip)<.0001))?(str>=strike):(str>=n_axis_strike)){
				i++;
				radius = proj_radius(strike-180,dip,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str+=increment;
			}

			//second nodal plane from null axis
			strike2 = (float) zero_360(strike2+180.0);
			increment = -fault;
			if((fault*(n_axis_strike-strike2))<-.0001)
				strike2 -= fault*360.0;

			str = (float) ((Math.abs(90.0-dip2)<.0001)?strike2:n_axis_strike);
			while((increment==1.) ? (str<=strike2):(str>=strike2)){
				i++;
				radius = proj_radius((float)(strike2-(1.0-fault)*90.0),dip2,str)*scale;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*radius;
				yCo[i]= co*radius;
				str+=increment;
			}
			//close the second compression part
			strike = (float) zero_360(strike);
			strike2 = (float)zero_360(strike2);
			increment = (pos_NP1_NP2>=0.0)? -fault:fault;
			if((increment*(strike-strike2))<-.0001)
				strike+=increment*360.0;

			str = strike2;
			while(increment==1. ? (str<=strike):(str>=strike)){
				i++;
				double si = Math.sin(Math.toRadians(str));
				double co = Math.cos(Math.toRadians(str));
				xCo[i]= si*scale;
				yCo[i]= co*scale;
				str+=increment;
			}
			drawCoOrdinates(xCo,yCo,i,g);
		}

		/*
		// Get path1 
		GeneralPath path = getPath(rotate(dip), scale);

		AffineTransform at = g.getTransform();

		// Draw path1 (black) , rotated strike
		g.rotate( Math.toRadians(strike) );
		g.setColor(Color.BLACK);
		g.fill(path);

		// Reset Transform
		g.setTransform(at);

		// Get path2
		GeneralPath path2 = getPath(rotate(dip2), scale);

		// Draw path2 (black) rotated strike2
		g.rotate( Math.toRadians(strike2));
		g.setColor(Color.BLACK);
		g.fill(path2);

		// Set clip to path2
		g.setClip(path2);

		// Reset transform
		g.setTransform(at);

		// Draw path1 again, white this time and using path2 as a clip
		g.rotate(Math.toRadians(strike));
		g.setColor(Color.WHITE);
		g.fill(path);

		// Reset the transform & clip
		g.setClip(null);
		g.setTransform(at);

		// Draw Border
		g.setColor(Color.BLACK);
		g.draw(circle);
		*/
	}

	public static GeneralPath getPath(float[][] rotation, float scale) {
		GeneralPath path = new GeneralPath();
		path.moveTo(scale * rotation[0][0], scale * rotation[0][1]);

		for (int i = 1; i < slices; i++) {
			if (rotation[i][2] < -.0001){
				path.lineTo(scale * circle[i][0],scale * circle[i][1]);
//				continue;
			}else{
				path.lineTo(scale * rotation[i][0],scale * rotation[i][1]);
			}
		}
		path.closePath();
		return path;
	}

	public static void drawCoOrdinates(double[]x,double[]y,int size, Graphics2D g){

		GeneralPath path = new GeneralPath();
		path.moveTo(x[0],-y[0]);

		for(int j=1;j<(size+1);j++){
			path.lineTo(x[j], -y[j]);
		}
		path.closePath();
		g.setColor(Color.BLACK);
		g.fill(path);

	}

	public static float[][] rotate(float theta) {
		theta = (float) Math.toRadians(theta);

		float rotated[][] = new float[slices][3];
		for (int i = 0; i < slices; i++) {
			rotated[i][1] = circle[i][1];
			rotated[i][0] = circle[i][0] * (float) Math.cos(theta);
			rotated[i][2] = circle[i][0] * (float) Math.sin(theta);
		}
		return rotated;
	}

	public static void main(String[] args) throws IOException {
	BufferedReader in = new BufferedReader(
			new FileReader("/Users/orion/workspace/JOGLTest/Book1.txt"));
	data = new ArrayList<float[]>();
	String read = in.readLine();
	while (read!=null) {
		String [] split = read.split("\t");
		float[] f = new float[6];
		for (int i = 0; i < f.length; i++) {
			f[i] = Float.parseFloat(split[i]);
		}
		data.add(f);
		read = in.readLine();
	}

		final JFrame f = new JFrame();
		f.setSize(100, 100);
		f.getContentPane().add(
				new Component() {

					@Override
					public void paint(Graphics g) {
						super.paint(g);
						float[] f = data.get(object);
						g.translate(50, 50);
						drawFMS((Graphics2D)g, 
								10,
								f[0],f[1],f[2],f[3],f[4],f[5]);
					}
				});
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				object++;
				if (object >= data.size()) object = 0;
				f.repaint();
			}
		
		});
	}
}