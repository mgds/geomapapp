package haxby.util;

public class IBM {
	public static int floatToIBM(float val) {
		int fconv = Float.floatToIntBits(val);
		int fmant = (0x007fffff & fconv) | 0x00800000;
		int t = ((0x7f800000 & fconv) >> 23) - 126;
		while( (t & 0x3) != 0 ) { ++t; fmant >>= 1; }
		return (0x80000000 & fconv) | (((t>>2) + 64) << 24) | fmant;
	}
	public static float IBMToFloat(int IBM_Bits) {
		int fconv = IBM_Bits;
		if( fconv != 0) {
			int fmant = 0x00ffffff & fconv;
			int t = (int) ((0x7f000000 & fconv) >> 22) - 130;
			//System.out.println(fconv + " " + fmant + " " + t);
			//added this check, because otherwise there might be same problem
			if (fmant != 0) {
				while( (fmant & 0x00800000) == 0) { --t; fmant <<= 1; }
			}	
			if (t > 254) fconv = (0x80000000 & fconv) | 0x7f7fffff;
			else if (t <= 0) fconv = 0;
			else fconv = (0x80000000 & fconv) |(t << 23)|(0x007fffff & fmant);
		}
		return Float.intBitsToFloat( fconv );
	}
}

