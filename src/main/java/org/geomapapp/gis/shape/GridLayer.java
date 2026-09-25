package org.geomapapp.gis.shape;

import org.geomapapp.grid.*;
import org.geomapapp.util.ParseLink;

import haxby.map.XMap;

import java.util.*;
import java.net.URL;
import java.io.IOException;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;

public class GridLayer {
	ESRIShapefile shape;
	String[] template;
	JLabel info;
	Grid2DOverlay overlay;
	String urlTemplate;
	public GridLayer(Vector props, ESRIShapefile shape, JLabel info, XMap map ) {
		if( props==null ) {
			info.setText("null properties vector");
			return;
		}
		urlTemplate = (String)ParseLink.getProperty(props, "url");
		if( urlTemplate==null ) {
			String msg = (String)ParseLink.getProperty(props, "message");
			if(msg==null)return;
			info.setText(msg);
			return;
		}
		this.shape = shape;
		if( info == null )info = new JLabel();
		this.info = info;
		info.setText( shape.toString() );
		overlay = new Grid2DOverlay(map, shape.toString());
	}
}
