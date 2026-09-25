package org.geomapapp.db.dsdp;

public class FossilRange {
	public float minZ;
	public float maxZ;
	public FossilDatum datum;
	public FossilRange(FossilDatum datum, float minZ, float maxZ) {
		this.datum = datum;
		this.minZ = minZ;
		this.maxZ = maxZ;
	}
}
