package haxby.image.jcodec.codecs.mpeg4.mp4;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import haxby.image.jcodec.codecs.aac.ADTSParser;
import haxby.image.jcodec.codecs.mpeg4.es.DecoderConfig;
import haxby.image.jcodec.codecs.mpeg4.es.DecoderSpecific;
import haxby.image.jcodec.codecs.mpeg4.es.Descriptor;
import haxby.image.jcodec.codecs.mpeg4.es.DescriptorParser;
import haxby.image.jcodec.codecs.mpeg4.es.ES;
import haxby.image.jcodec.codecs.mpeg4.es.NodeDescriptor;
import haxby.image.jcodec.codecs.mpeg4.es.SL;
import haxby.image.jcodec.containers.mp4.boxes.FullBox;
import haxby.image.jcodec.containers.mp4.boxes.Header;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MPEG 4 elementary stream descriptor
 * 
 * @author The JCodec project
 * 
 */
public class EsdsBox extends FullBox {

    private ByteBuffer streamInfo;
    private int objectType;
    private int bufSize;
    private int maxBitrate;
    private int avgBitrate;
    private int trackId;

    public static String fourcc() {
        return "esds";
    }

    public EsdsBox(Header atom) {
        super(atom);
    }

    @Override
    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        if (streamInfo != null && streamInfo.remaining() > 0) {
            ArrayList<Descriptor> l = new ArrayList<Descriptor>();
            ArrayList<Descriptor> l1 = new ArrayList<Descriptor>();
            l1.add(new DecoderSpecific(streamInfo));
            l.add(new DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, l1));
            l.add(new SL());
            new ES(trackId, l).write(out);
        } else {
            ArrayList<Descriptor> l = new ArrayList<Descriptor>();
            l.add(new DecoderConfig(objectType, bufSize, maxBitrate, avgBitrate, new ArrayList<Descriptor>()));
            l.add(new SL());
            new ES(trackId, l).write(out);
        }
    }
    
    @Override
    public int estimateSize() {
        return 64;
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        ES es = (ES) DescriptorParser.read(input);

        trackId = es.getTrackId();
        DecoderConfig decoderConfig = NodeDescriptor.findByTag(es, DecoderConfig.tag());
        objectType = decoderConfig.getObjectType();
        bufSize = decoderConfig.getBufSize();
        maxBitrate = decoderConfig.getMaxBitrate();
        avgBitrate = decoderConfig.getAvgBitrate();
        DecoderSpecific decoderSpecific = NodeDescriptor.findByTag(decoderConfig, DecoderSpecific.tag());
        streamInfo = decoderSpecific == null ? null : decoderSpecific.getData();
    }

    public ByteBuffer getStreamInfo() {
        return streamInfo;
    }

    public int getObjectType() {
        return objectType;
    }

    public int getBufSize() {
        return bufSize;
    }

    public int getMaxBitrate() {
        return maxBitrate;
    }

    public int getAvgBitrate() {
        return avgBitrate;
    }

    public int getTrackId() {
        return trackId;
    }

    public static EsdsBox fromADTS(haxby.image.jcodec.codecs.aac.ADTSParser.Header hdr) {
        return createEsdsBox(ADTSParser.adtsToStreamInfo(hdr), hdr.getObjectType() << 5, 0, 210750, 133350, 2);
    }

    public static EsdsBox createEsdsBox(ByteBuffer streamInfo, int objectType, int bufSize, int maxBitrate,
            int avgBitrate, int trackId) {
        EsdsBox esds = new EsdsBox(new Header(fourcc()));
        esds.objectType = objectType;
        esds.bufSize = bufSize;
        esds.maxBitrate = maxBitrate;
        esds.avgBitrate = avgBitrate;
        esds.trackId = trackId;
        esds.streamInfo = streamInfo;
        return esds;
    }

    public static EsdsBox newEsdsBox() {
        return new EsdsBox(new Header(fourcc()));
    }
}