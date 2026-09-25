package haxby.image.jcodec.aac.tools;

import haxby.image.jcodec.aac.huffman.HCB;
import haxby.image.jcodec.aac.syntax.CPE;
import haxby.image.jcodec.aac.syntax.ICSInfo;
import haxby.image.jcodec.aac.syntax.ICStream;
import haxby.image.jcodec.aac.syntax.SyntaxConstants;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Intensity stereo
 * 
 * @author in-somnia
 */
public final class IS implements SyntaxConstants, ISScaleTable, HCB {

    private IS() {
    }

    public static void process(CPE cpe, float[] specL, float[] specR) {
        final ICStream ics = cpe.getRightChannel();
        final ICSInfo info = ics.getInfo();
        final int[] offsets = info.getSWBOffsets();
        final int windowGroups = info.getWindowGroupCount();
        final int maxSFB = info.getMaxSFB();
        final int[] sfbCB = ics.getSfbCB();
        final int[] sectEnd = ics.getSectEnd();
        final float[] scaleFactors = ics.getScaleFactors();

        int w, i, j, c, end, off;
        int idx = 0, groupOff = 0;
        float scale;
        for (int g = 0; g < windowGroups; g++) {
            for (i = 0; i < maxSFB;) {
                if (sfbCB[idx] == INTENSITY_HCB || sfbCB[idx] == INTENSITY_HCB2) {
                    end = sectEnd[idx];
                    for (; i < end; i++, idx++) {
                        c = sfbCB[idx] == INTENSITY_HCB ? 1 : -1;
                        if (cpe.isMSMaskPresent())
                            c *= cpe.isMSUsed(idx) ? -1 : 1;
                        scale = c * scaleFactors[idx];
                        for (w = 0; w < info.getWindowGroupLength(g); w++) {
                            off = groupOff + w * 128 + offsets[i];
                            for (j = 0; j < offsets[i + 1] - offsets[i]; j++) {
                                specR[off + j] = specL[off + j] * scale;
                            }
                        }
                    }
                } else {
                    end = sectEnd[idx];
                    idx += end - i;
                    i = end;
                }
            }
            groupOff += info.getWindowGroupLength(g) * 128;
        }
    }
}
