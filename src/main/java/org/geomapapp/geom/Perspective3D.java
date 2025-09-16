package org.geomapapp.geom;

public class Perspective3D implements Transform3D {
	public final static int VERTICAL_NP=0;
	public final static int VERTICAL_VP=1;
	XYZ viewPoint;
	XYZ[] axes;
	double scale;	// e.g. x = scale * DOT( axes[0], p ) / DOT( axes[2], p);
	public Perspective3D( XYZ viewPoint, 
				XYZ[] axes, 
				double scale ) {
		this.viewPoint = viewPoint;
		this.axes = axes;
		this.scale = scale;
	}
	public Perspective3D( GCPoint view,
				GCPoint focus,
				GCPoint verticalRef,
				double scale ) {
		viewPoint = view.getXYZ();
		XYZ focalPoint = focus.getXYZ();
		axes = new XYZ[3];
		axes[2] = focalPoint.minus(viewPoint).normalize();
		axes[0] = axes[2].cross( verticalRef.getXYZ() ).normalize();
		axes[1] = axes[2].cross( axes[0] ).normalize();
		this.scale = scale;
	}
	public Perspective3D( GCPoint view,
				GCPoint focus,
				GCPoint verticalRef,
				double field_of_view,
				double width ) {
		this(	view, 
			focus, 
			verticalRef, 
			width/Math.tan(Math.toRadians(field_of_view))
		);
	}
	public Perspective3D( GCPoint view,
				GCPoint focus,
				int vref,
				double field_of_view,
				double width ) {
		this(	view,
			focus,
			vref==0 ? new GCPoint(0., 0., 0.)
				: view,
			field_of_view,
			width);
	}
	public Perspective3D( GCPoint view,
				GCPoint focus,
				double field_of_view,
				double width ) {
		this(	view,
			focus,
			view,
			field_of_view,
			width);
	}
	public XYZ minusVP( XYZ point ) {
		return point.minus(viewPoint);
	}
	public XYZ forward( XYZ ref ) {
		if( ref==null )return null;
		XYZ p = ref.minus( viewPoint );
		XYZ prj = new XYZ( p.dot(axes[0]),
				p.dot(axes[1]),
				p.dot(axes[2]));
		if( prj.z<=0. )return null;
		double s = scale/prj.z;
		prj.x *= s;
		prj.y *= s;
		return prj;
	}
	public XYZ forward( GCPoint p ) {
		return forward( p.getXYZ() );
	}
	public XYZ inverse( XYZ prj ) {
		XYZ p = new XYZ(0., 0., prj.z);
		p.x = prj.x *prj.z/scale;
		p.y = prj.y *prj.z/scale;
		XYZ ref = viewPoint.plus(axes[0].times(p.x)).plus(axes[1].times(p.y)).plus(axes[2].times(p.z));
		return ref;
	}
}