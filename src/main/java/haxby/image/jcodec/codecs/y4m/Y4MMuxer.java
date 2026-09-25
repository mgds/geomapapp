package haxby.image.jcodec.codecs.y4m;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Muxer;
import haxby.image.jcodec.common.MuxerTrack;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Size;

/**
 * Stores frames into Y4M file.
 * 
 * @author Stanislav Vitvitskiy
 * 
 */
public class Y4MMuxer implements Muxer, MuxerTrack {
    private WritableByteChannel ch;
    private boolean headerWritten;
    private VideoCodecMeta meta;
    public static final byte[] frameTag = "FRAME\n".getBytes();

    public Y4MMuxer(WritableByteChannel ch) {
        this.ch = ch;
    }

    protected void writeHeader() throws IOException {
        Size size = meta.getSize();
        byte[] bytes = String
                .format("YUV4MPEG2 W%d H%d F25:1 Ip A0:0 C420jpeg XYSCSS=420JPEG\n", size.getWidth(), size.getHeight())
                .getBytes();
        ch.write(ByteBuffer.wrap(bytes));
    }

    @Override
    public void addFrame(Packet outPacket) throws IOException {
        if (!headerWritten) {
            writeHeader();
            headerWritten = true;
        }
        ch.write(ByteBuffer.wrap(frameTag));
        ch.write(outPacket.data.duplicate());
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        this.meta = meta;
        return this;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        throw new RuntimeException("Y4M doesn't support audio");
    }

    @Override
    public void finish() throws IOException {
    }
}
