package haxby.image.jcodec.codecs.wav;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Demuxer;
import haxby.image.jcodec.common.DemuxerTrack;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Packet.FrameType;

/**
 * A demuxer for a wav file.
 * 
 * 
 * @author Stan Vitvitskiy
 */
public class WavDemuxer implements Demuxer, DemuxerTrack {
    // How many audio frames will be read per packet. One frame is all samples
    // for all channels, i.e. for the 5.1 16bit format one frame will be 12
    // bytes.
    private static final int FRAMES_PER_PKT = 1024;

    private SeekableByteChannel ch;
    private WavHeader header;
    private long dataSize;
    private short frameSize;

    private int frameNo;

    private long pts;

    public WavDemuxer(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        header = WavHeader.readChannel(ch);
        dataSize = ch.size() - ch.position();
        frameSize = header.getFormat().getFrameSize();
    }

    @Override
    public void close() throws IOException {
        ch.close();
    }

    @Override
    public Packet nextFrame() throws IOException {
        ByteBuffer data = NIOUtils.fetchFromChannel(ch, frameSize * FRAMES_PER_PKT);
        if (!data.hasRemaining())
            return null;
        long oldPts = pts;
        int duration = data.remaining() / frameSize;
        pts += duration;
        return Packet.createPacket(data, oldPts, header.getFormat().getFrameRate(), data.remaining() / frameSize,
                frameNo++, FrameType.KEY, null);
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        AudioFormat format = header.getFormat();
        AudioCodecMeta audioCodecMeta = haxby.image.jcodec.common.AudioCodecMeta.fromAudioFormat(format);
        long totalFrames = dataSize / format.getFrameSize();
        return new DemuxerTrackMeta(TrackType.AUDIO, Codec.PCM, (double) totalFrames / format.getFrameRate(), null,
                (int) totalFrames, null, null, audioCodecMeta);
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        List<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        result.add(this);
        return result;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        List<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        return result;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return getTracks();
    }
}