package haxby.image.jcodec.containers.raw;

import java.io.IOException;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Muxer;
import haxby.image.jcodec.common.MuxerTrack;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;

public class RawMuxer implements Muxer, MuxerTrack {

    private SeekableByteChannel ch;
    private boolean hasVideo;
    private boolean hasAudio;

    public RawMuxer(SeekableByteChannel destStream) {
        this.ch = destStream;
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        if (hasAudio)
            throw new RuntimeException("Raw muxer supports either video or audio track but not both.");
        hasVideo = true;
        return this;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        if (hasVideo)
            throw new RuntimeException("Raw muxer supports either video or audio track but not both.");
        hasAudio = true;
        return this;
    }

    @Override
    public void finish() throws IOException {
    }

    @Override
    public void addFrame(Packet outPacket) throws IOException {
        ch.write(outPacket.getData().duplicate());
    }
}