package haxby.image.jcodec.common;

import java.io.IOException;

import haxby.image.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface DemuxerTrack {
    Packet nextFrame() throws IOException;
    
    DemuxerTrackMeta getMeta();
}
