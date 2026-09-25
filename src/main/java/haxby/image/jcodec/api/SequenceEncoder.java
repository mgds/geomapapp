package haxby.image.jcodec.api;

import static haxby.image.jcodec.common.Codec.H264;
import static haxby.image.jcodec.common.Format.MOV;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import haxby.image.jcodec.api.transcode.PixelStore;
import haxby.image.jcodec.api.transcode.PixelStoreImpl;
import haxby.image.jcodec.api.transcode.Sink;
import haxby.image.jcodec.api.transcode.SinkImpl;
import haxby.image.jcodec.api.transcode.VideoFrameWithPacket;
import haxby.image.jcodec.api.transcode.PixelStore.LoanerPicture;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Format;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Rational;
import haxby.image.jcodec.common.model.Packet.FrameType;
import haxby.image.jcodec.scale.ColorUtil;
import haxby.image.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Encodes a sequence of images as a video.
 * 
 * @author The JCodec project
 */
public class SequenceEncoder {

    private Transform transform;
    private int frameNo;
    private int timestamp;
    private Rational fps;
    private Sink sink;
    private PixelStore pixelStore;

    public static SequenceEncoder createSequenceEncoder(File out, int fps) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(fps, 1), MOV, H264, null);
    }

    public static SequenceEncoder create25Fps(File out) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(25, 1), MOV, H264, null);
    }

    public static SequenceEncoder create30Fps(File out) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(30, 1), MOV, H264, null);
    }

    public static SequenceEncoder create2997Fps(File out) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(30000, 1001), MOV, H264, null);
    }

    public static SequenceEncoder create24Fps(File out) throws IOException {
        return new SequenceEncoder(NIOUtils.writableChannel(out), Rational.R(24, 1), MOV, H264, null);
    }

    public static SequenceEncoder createWithFps(SeekableByteChannel out, Rational fps) throws IOException {
        return new SequenceEncoder(out, fps, MOV, H264, null);
    }

    public SequenceEncoder(SeekableByteChannel out, Rational fps, Format outputFormat, Codec outputVideoCodec,
            Codec outputAudioCodec) throws IOException {
        this.fps = fps;

        sink = SinkImpl.createWithStream(out, outputFormat, outputVideoCodec, outputAudioCodec);

        pixelStore = new PixelStoreImpl();
    }

    /**
     * Allows passing configuration to the codec. Must be called before the first
     * frame is encoded.
     * 
     * @param opts
     */
    public void configureCodec(Map<String, String> opts) {
        if (sink.isInitialised()) {
            throw new RuntimeException(
                    "Sink was already used to encode frames, we cannot allow any codec configuration changes");
        }
        sink.setCodecOpts(opts);
    }

    /**
     * Encodes a frame into a movie.
     * 
     * @param pic
     * @throws IOException
     */
    public void encodeNativeFrame(Picture pic) throws IOException {
        if (pic.getColor() != ColorSpace.RGB)
            throw new IllegalArgumentException("The input images is expected in RGB color.");
        if (!sink.isInitialised()) {
            sink.init(false, false);
            if (sink.getInputColor() != null)
                transform = ColorUtil.getTransform(ColorSpace.RGB, sink.getInputColor());
        }

        ColorSpace sinkColor = sink.getInputColor();
        LoanerPicture toEncode;
        if (sinkColor != null) {
            toEncode = pixelStore.getPicture(pic.getWidth(), pic.getHeight(), sinkColor);
            transform.transform(pic, toEncode.getPicture());
        } else {
            toEncode = new LoanerPicture(pic, 0);
        }

        Packet pkt = Packet.createPacket(null, timestamp, fps.getNum(), fps.getDen(), frameNo, FrameType.KEY, null);
        sink.outputVideoFrame(new VideoFrameWithPacket(pkt, toEncode));

        if (sinkColor != null)
            pixelStore.putBack(toEncode);

        timestamp += fps.getDen();
        frameNo++;
    }

    public void finish() throws IOException {
        sink.finish();
    }
}
