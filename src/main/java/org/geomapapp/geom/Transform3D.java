package org.geomapapp.geom;

public interface Transform3D {
	public abstract XYZ forward( XYZ refXYZ );
	public abstract XYZ inverse( XYZ mapXYZ );
}
