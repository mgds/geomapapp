package haxby.image.jcodec.containers.y4m;

import static haxby.image.jcodec.common.StringUtils.splitC;
import static haxby.image.jcodec.platform.Platform.stringFromBytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Demuxer;
import haxby.image.jcodec.common.DemuxerTrack;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Rational;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.common.model.Packet.FrameType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Y4MDemuxer implements DemuxerTrack, Demuxer {

    private SeekableByteChannel is;
    private int width;
    private int height;
    private String invalidFormat;
    private Rational fps;
    private int bufSize;
    private int frameNum;
    private int totalFrames;
    private int totalDuration;

    public Y4MDemuxer(SeekableByteChannel _is) throws IOException {
        this.is = _is;
        ByteBuffer buf = NIOUtils.fetchFromChannel(is, 2048);
        String[] header = splitC(readLine(buf), ' ');

        if (!"YUV4MPEG2".equals(header[0])) {
            invalidFormat = "Not yuv4mpeg stream";
            return;
        }
        String chroma = find(header, 'C');
        if (chroma != null && !chroma.startsWith("420")) {
            invalidFormat = "Only yuv420p is supported";
            return;
        }

        width = Integer.parseInt(find(header, 'W'));
        height = Integer.parseInt(find(header, 'H'));

        String fpsStr = find(header, 'F');
        if (fpsStr != null) {
            String[] numden = splitC(fpsStr, ':');
            fps = new Rational(Integer.parseInt(numden[0]), Integer.parseInt(numden[1]));
        }

        is.setPosition(buf.position());
        bufSize = width * height;
        bufSize += bufSize / 2;
        long fileSize = is.size();
        totalFrames = (int) (fileSize / (bufSize + 7));
        totalDuration = (totalFrames * fps.getDen()) / fps.getNum();
    }

    @Override
    public Packet nextFrame() throws IOException {
        if (invalidFormat != null)
            throw new RuntimeException("Invalid input: " + invalidFormat);
        ByteBuffer buf = NIOUtils.fetchFromChannel(is, 2048);
        String frame = readLine(buf);
        if (frame == null || !frame.startsWith("FRAME"))
            return null;

        is.setPosition(is.position() - buf.remaining());
        ByteBuffer pix = NIOUtils.fetchFromChannel(is, bufSize);
        Packet packet = new Packet(pix, frameNum * fps.getDen(), fps.getNum(), fps.getDen(), frameNum, FrameType.KEY, null, frameNum);
        ++frameNum;
        return packet;
    }

    private static String find(String[] header, char c) {
        for (int i = 0; i < header.length; i++) {
            String string = header[i];
            if (string.charAt(0) == c)
                return string.substring(1);
        }
        return null;
    }

    private static String readLine(ByteBuffer y4m) {
        ByteBuffer duplicate = y4m.duplicate();
        while (y4m.hasRemaining() && y4m.get() != '\n')
            ;
        if (y4m.hasRemaining())
            duplicate.limit(y4m.position() - 1);
        return stringFromBytes(NIOUtils.toArray(duplicate));
    }

    public Rational getFps() {
        return fps;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        return new DemuxerTrackMeta(TrackType.VIDEO, Codec.RAW, totalDuration, null, totalFrames, null,
                haxby.image.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(new Size(width, height), ColorSpace.YUV420), null);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        List<DemuxerTrack> list = new ArrayList<DemuxerTrack>();
        list.add(this);
        return list;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        return getTracks();
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        return new ArrayList<DemuxerTrack>();
    }
}