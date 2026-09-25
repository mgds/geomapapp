package org.geomapapp.grid;

import ucar.nc2.*;
import ucar.ma2.*;
import java.util.*;
import java.io.*;

public class TestCDF {
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("usage: java testCDF filename");
			System.exit(0);
		}
		new TestCDF(args[0]);
	}
	public TestCDF(String fileName) {
		try {
			
//			***** GMA 1.6.4: TESTING
//			NetcdfFile nc = new NetcdfFile(fileName);
		
			NetcdfFile nc = NetcdfFile.open(fileName);
//			***** GMA 1.6.4
			
		//	System.out.println(nc);
			
//			***** GMA 1.6.4: TESTING
//			Iterator vi = nc.getGlobalAttributeIterator();
			
			List globalList = nc.getGlobalAttributes();
			Iterator vi = globalList.iterator();
//			***** GMA 1.6.4
			
			while(vi.hasNext()) {
				Attribute v = (Attribute) vi.next();
				System.out.println("attribute: "+v.getName()+" "+v.getStringValue());
			}
			
//			***** GMA 1.6.4: TESTING
//			vi = nc.getVariableIterator();
			
			List variableList = nc.getVariables();
			vi = variableList.iterator();
//			***** GMA 1.6.4
			
			while(vi.hasNext()) {
				Variable v = (Variable) vi.next();
				int rank = v.getRank();
				System.out.println("variable: "+v.getName() 
						+": rank = "+rank);
				
//				***** GMA 1.6.4: TESTING
//				System.out.println("  class:\t"+ v.getElementType().toString());
				
				System.out.println("  class:\t"+ v.getDataType().toString());
//				***** GMA 1.6.4
				
				int[] shape = v.getShape();
				for(int i=0 ; i<shape.length ; i++) {
					System.out.println("\t"+ i +":\t"
						+ v.getDimension(i).getLength() 
						+" elements\t");
				}
				if(v.getRank() == 1) {
					
//					***** GMA 1.6.4: TESTING
//					Iterator it = v.getAttributeIterator();
					
					List attributeList = v.getAttributes();
					Iterator it = attributeList.iterator();
//					***** GMA 1.6.4
					
					while(it.hasNext()) {
						Attribute att = (Attribute) it.next();
						System.out.println("\tattribute: "
								
//						***** GMA 1.6.4: TESTING
//							+att.getName()+" "+att.getValueType().toString());
								
							+att.getName()+" "+att.getDataType().toString());
//						***** GMA 1.6.4
							
					}
					Array a = v.read();
					
//					***** GMA 1.6.4: TESTING
//					System.out.println( v.getElementType().toString() );
//					if(v.getElementType().equals(float.class)) {
//						float[] fArray = (float[]) a.copyTo1DJavaArray();
//						System.out.println("\t\t"+fArray[0] +"\t"+ fArray[1]);
//					} else if(v.getElementType().equals(double.class)) {
//						double[] dArray = (double[]) a.copyTo1DJavaArray();
//						System.out.println("\t\t"+dArray[0] +"\t"+ dArray[1]);
//					} else if(v.getElementType().equals(int.class)) {
						
					System.out.println( v.getDataType().toString() );
					if(v.getDataType().equals(float.class)) {
						float[] fArray = (float[]) a.copyTo1DJavaArray();
						System.out.println("\t\t"+fArray[0] +"\t"+ fArray[1]);
					} else if(v.getDataType().equals(double.class)) {
						double[] dArray = (double[]) a.copyTo1DJavaArray();
						System.out.println("\t\t"+dArray[0] +"\t"+ dArray[1]);
					} else if(v.getDataType().equals(int.class)) {
//				***** GMA 1.6.4

						int[] dArray = (int[]) a.copyTo1DJavaArray();
						System.out.println("\t\t"+dArray[0] +"\t"+ dArray[1]);
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
