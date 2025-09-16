package haxby.image.jcodec.codecs.wav;

import java.io.IOException;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Muxer;
import haxby.image.jcodec.common.MuxerTrack;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs integer samples into wav file
 * 
 * @author The JCodec project
 */
public class WavMuxer implements Muxer, MuxerTrack {

    protected SeekableByteChannel out;
    protected WavHeader header;
    protected int written;
    private AudioFormat format;

    public WavMuxer(SeekableByteChannel out) throws IOException {
        this.out = out;
    }
    
    @Override
    public void addFrame(Packet outPacket) throws IOException {
        written += out.write(outPacket.getData());
    }

    public void close() throws IOException {
        out.setPosition(0);
        WavHeader.createWavHeader(format, format.bytesToFrames(written)).write(out);
        NIOUtils.closeQuietly(out);
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        return null;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        header = WavHeader.createWavHeader(meta.getFormat(), 0);
        this.format = meta.getFormat();
        try {
            header.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public void finish() throws IOException {
        // NOP
    }
}