package org.geomapapp.db.dsdp;

import java.util.StringTokenizer;
import java.util.Vector;

public class DSDPHole {
	public double lon, lat;
	public float totalPen;
	public AgeInterval[] ageIntervals = new AgeInterval[0];
	public DSDPCore[] cores;
	DSDPCore[] altCores;
	Vector fossils;
	Vector dataSets;
	Vector ageModels;
	Vector sources;
	String name;
	public DSDPHole(String name, double lon, double lat, float totalPen) {
		this.name = name;
		this.lon = lon;
		this.lat = lat;
		this.totalPen = totalPen;
	}
	public void setAgeIntervals( AgeInterval[] ages ) {
		ageIntervals = ages;
	}
	public DSDPCore[] getCores() {
		return cores;
	}
	public void setCores( DSDPCore[] cores ) {
		this.cores = cores;
	}
	public void setAltCores( DSDPCore[] cores ) {
		this.altCores = cores;
	}
	public int getLeg() {
		StringTokenizer st = new StringTokenizer(name,"-");
		return Integer.parseInt(st.nextToken());
	}
	public void addAgeModel( Vector model ) {
		if( ageModels==null )ageModels = new Vector();
		ageModels.add(model);
	}
	public void addSources( Vector holeSources ) {
		if( sources==null )sources = new Vector();
		sources.add(holeSources);
	}
	public Vector getAgeModel( ) {
		if( ageModels==null || ageModels.size()==0 ) {
			ageModels = new Vector();
			Vector model = new Vector();
			model.add( new float[] {0f, 0f} );
			ageModels.add(model);			
			return model;
		}
		return (Vector)ageModels.get(0);
	}
	public Vector getSources( ) {
		if( sources==null || sources.size()==0 ) {
			sources = new Vector();
			Vector model = new Vector();
			model.add( new String("") );
			sources.add(model);
			return model;
		}
		return (Vector)sources.get(0);
	}
	public void addData( DSDPData data ) {
		if( dataSets==null )dataSets = new Vector();
		dataSets.add(data);
	}
	public void removeData( DSDPData data ) {
		if( dataSets==null )return;
		dataSets.remove(data);
	}
	public void addFossilAssembly( FossilAssembly fossil ) {
		if( fossils==null )fossils = new Vector();
		fossils.add(fossil);
		fossils.trimToSize();
	}
	public FossilAssembly getFossilAssembly( String groupName ) {
		if(fossils==null)return null;
		for( int k=0 ; k<fossils.size() ; k++) {
			FossilAssembly f = (FossilAssembly)fossils.get(k);
			if( f.getGroupName().equals(groupName) )return f;
		}
		return null;
	}
	public int indexOf( String groupName ) {
		if( fossils==null )return -1;
		for( int k=0 ; k<fossils.size() ; k++) {
			FossilAssembly fossil = (FossilAssembly)fossils.get(k);
			if( fossil.getGroupName().equals(groupName) ) return k;
		}
		return -1;
	}
	public void removeFossilAssembly( String groupName ) {
		int i=indexOf( groupName );
		if( i==-1 )return;
		fossils.remove(i);
		fossils.trimToSize();
	}
	public AgeInterval isAgeRepresented( float age ) {
	//	float[] range = Ages.getAgeRange(age);
		for( int k=0 ; k<ageIntervals.length ; k++) {
			float[] r = ageIntervals[k].getAgeRange();
			if( r[1]<age )continue;
			if( r[0]>age ) return ageIntervals[k];
			else return null;
		}
		return null;
	}
	public AgeInterval ageAtDepth( float depth ) {
	//	float[] range = Ages.getAgeRange(age);
		for( int k=0 ; k<ageIntervals.length ; k++) {
			AgeInterval age = ageIntervals[k];
			if( age.bottom<depth )continue;
			if( age.top<depth )return age;
			else return null;
		}
		return null;
	}
	public DSDPCore coreAtDepth( float z ) {
		if( cores==null )return null;
		for( int k=0 ; k<cores.length ; k++) {
			if (cores[k] == null) continue;
			if( cores[k].bottom<=z )continue;
			if( cores[k].top<=z )return cores[k];
			else return null;
		}
		return null;
	}
	public int coreNumberAtDepth( float z ) {
		if( cores==null )return -1;
		for( int k=0 ; k<cores.length ; k++) {
			if (cores[k] == null) continue;
			if( cores[k].bottom<=z )continue;
			if( cores[k].top<=z )return k;
			else return -1;
		}
		return -1;
	}
	public String toString() {
		return name;
	}
}
