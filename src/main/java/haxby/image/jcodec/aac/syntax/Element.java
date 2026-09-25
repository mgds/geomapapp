package haxby.image.jcodec.aac.syntax;

import haxby.image.jcodec.aac.AACException;
import haxby.image.jcodec.aac.SampleFrequency;
import haxby.image.jcodec.aac.sbr.SBR;
import haxby.image.jcodec.common.io.BitReader;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 *
 * @author in-somnia
 */
public abstract class Element implements SyntaxConstants {

    private int elementInstanceTag;
    private SBR sbr;

    protected void readElementInstanceTag(BitReader _in) throws AACException {
        elementInstanceTag = _in.readNBit(4);
    }

    public int getElementInstanceTag() {
        return elementInstanceTag;
    }

    void decodeSBR(BitReader _in, SampleFrequency sf, int count, boolean stereo, boolean crc, boolean downSampled,
            boolean smallFrames) throws AACException {
        if (sbr == null)
            sbr = new SBR(smallFrames, elementInstanceTag == ELEMENT_CPE, sf, downSampled);
        sbr.decode(_in, count);
    }

    boolean isSBRPresent() {
        return sbr != null;
    }

    SBR getSBR() {
        return sbr;
    }
}
