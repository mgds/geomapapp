package haxby.image.jcodec.containers.mps;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import haxby.image.jcodec.common.Demuxer;
import haxby.image.jcodec.common.DemuxerTrack;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public interface MPEGDemuxer extends Demuxer {
    List<? extends MPEGDemuxerTrack> getTracks();
    List<? extends MPEGDemuxerTrack> getVideoTracks();
    List<? extends MPEGDemuxerTrack> getAudioTracks();
    
    public static interface MPEGDemuxerTrack extends DemuxerTrack {
        Packet nextFrameWithBuffer(ByteBuffer buf) throws IOException;
        DemuxerTrackMeta getMeta();

        int getSid();

        List<PESPacket> getPending();

        void ignore();
    }
}