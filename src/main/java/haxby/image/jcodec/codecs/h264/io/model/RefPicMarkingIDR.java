package haxby.image.jcodec.codecs.h264.io.model;

import haxby.image.jcodec.common.tools.ToJSON;
import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reference picture marking used for IDR frames
 * 
 * @author The JCodec project
 * 
 */
public class RefPicMarkingIDR {
    boolean discardDecodedPics;
    boolean useForlongTerm;

    public RefPicMarkingIDR(boolean discardDecodedPics, boolean useForlongTerm) {
        this.discardDecodedPics = discardDecodedPics;
        this.useForlongTerm = useForlongTerm;
    }

    public boolean isDiscardDecodedPics() {
        return discardDecodedPics;
    }

    public boolean isUseForlongTerm() {
        return useForlongTerm;
    }
    
    @Override
    public String toString() {
        return Platform.toJSON(this);
    }
}
