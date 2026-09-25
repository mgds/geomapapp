package org.geomapapp.db.dsdp;

public class DSDPCore {
	public float top;
	public float bottom;
	public float recovered;
	public byte nSection;
	public DSDPCore( float top,
			float bottom,
			float recovered,
			int nSection ) {
		this.top = top;
		this.bottom = bottom;
		this.recovered = recovered;
		this.nSection = (byte)nSection;
	}
}
