package haxby.image.jcodec.containers.mp4.muxer;

import static haxby.image.jcodec.common.Preconditions.checkState;
import static haxby.image.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import haxby.image.jcodec.codecs.aac.ADTSParser;
import haxby.image.jcodec.codecs.h264.H264Utils;
import haxby.image.jcodec.codecs.h264.io.model.SeqParameterSet;
import haxby.image.jcodec.codecs.mpeg4.mp4.EsdsBox;
import haxby.image.jcodec.common.AudioFormat;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.common.model.Packet.FrameType;
import haxby.image.jcodec.containers.mp4.MP4TrackType;
import haxby.image.jcodec.containers.mp4.boxes.AudioSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.Header;
import haxby.image.jcodec.containers.mp4.boxes.MovieHeaderBox;
import haxby.image.jcodec.containers.mp4.boxes.PixelAspectExt;
import haxby.image.jcodec.containers.mp4.boxes.SampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.VideoSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.Box.LeafBox;
import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodecMP4MuxerTrack extends MP4MuxerTrack {

    private static Map<Codec, String> codec2fourcc = new HashMap<Codec, String>();

    static {
        codec2fourcc.put(Codec.MP1, ".mp1");
        codec2fourcc.put(Codec.MP2, ".mp2");
        codec2fourcc.put(Codec.MP3, ".mp3");
        codec2fourcc.put(Codec.H265, "hev1");
        codec2fourcc.put(Codec.H264, "avc1");
        codec2fourcc.put(Codec.AAC, "mp4a");
        codec2fourcc.put(Codec.PRORES, "apch");
        codec2fourcc.put(Codec.JPEG, "mjpg");
        codec2fourcc.put(Codec.PNG, "png ");
        codec2fourcc.put(Codec.V210, "v210");
    }

    private Codec codec;

    // SPS/PPS lists when h.264 is stored, otherwise these lists are not used.
    private List<ByteBuffer> spsList;
    private List<ByteBuffer> ppsList;

    // ADTS header used to construct audio sample entry for AAC
    private ADTSParser.Header adtsHeader;

    private ByteBuffer codecPrivateOpaque;

    public CodecMP4MuxerTrack(int trackId, MP4TrackType type, Codec codec) {
        super(trackId, type);
        this.codec = codec;
        this.spsList = new ArrayList<ByteBuffer>();
        this.ppsList = new ArrayList<ByteBuffer>();
    }
    
    public void setCodecPrivateOpaque(ByteBuffer codecPrivate) {
        this.codecPrivateOpaque = codecPrivate;
    }

    @Override
    public void addFrame(Packet pkt) throws IOException {
        if (codec == Codec.H264) {
            ByteBuffer result = pkt.getData();
            
            if (pkt.frameType == FrameType.UNKNOWN) {
                pkt.setFrameType(H264Utils.isByteBufferIDRSlice(result) ? FrameType.KEY : FrameType.INTER);
            }
            
            H264Utils.wipePSinplace(result, spsList, ppsList);
            result = H264Utils.encodeMOVPacket(result);
            pkt = Packet.createPacketWithData(pkt, result);
        } else if (codec == Codec.AAC) {
            ByteBuffer result = pkt.getData();
            adtsHeader = ADTSParser.read(result);
//            System.out.println(String.format("crc_absent: %d, num_aac_frames: %d, size: %d, remaining: %d, %d, %d, %d",
//                    adtsHeader.getCrcAbsent(), adtsHeader.getNumAACFrames(), adtsHeader.getSize(), result.remaining(),
//                    adtsHeader.getObjectType(), adtsHeader.getSamplingIndex(), adtsHeader.getChanConfig()));
            pkt = Packet.createPacketWithData(pkt, result);
        }
        super.addFrame(pkt);
    }

    @Override
    public void addFrameInternal(Packet pkt, int entryNo) throws IOException {
        checkState(!finished, "The muxer track has finished muxing");

        if (_timescale == NO_TIMESCALE_SET) {
            if (adtsHeader != null) {
                _timescale = adtsHeader.getSampleRate();
            } else {
                _timescale = pkt.getTimescale();
            }
        }
        
        if (adtsHeader != null && pkt.getDuration() == 0) {
            pkt.setDuration(1024);
        }
        
        if (_timescale != pkt.getTimescale()) {
            pkt.setPts((pkt.getPts() * _timescale) / pkt.getTimescale());
            pkt.setDuration((pkt.getDuration() * _timescale) / pkt.getTimescale());
            pkt.setTimescale(_timescale);
        }

        super.addFrameInternal(pkt, entryNo);
    }

    @Override
    protected Box finish(MovieHeaderBox mvhd) throws IOException {
        checkState(!finished, "The muxer track has finished muxing");
        if (getEntries().isEmpty()) {
            if (codec == Codec.H264 && !spsList.isEmpty()) {
                SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
                Size size = H264Utils.getPicSize(sps);
                VideoCodecMeta meta = createSimpleVideoCodecMeta(size, ColorSpace.YUV420);
                addVideoSampleEntry(meta);
            } else {
                Logger.warn("CodecMP4MuxerTrack: Creating a track without sample entry");
            }
        }
        setCodecPrivateIfNeeded();

        return super.finish(mvhd);
    }

    void addVideoSampleEntry(VideoCodecMeta meta) {
        SampleEntry se = VideoSampleEntry.videoSampleEntry(codec2fourcc.get(codec), meta.getSize(), "JCodec");
        if (meta.getPixelAspectRatio() != null)
            se.add(PixelAspectExt.createPixelAspectExt(meta.getPixelAspectRatio()));
        addSampleEntry(se);
    }

    private static List<ByteBuffer> selectUnique(List<ByteBuffer> bblist) {
        Set<ByteArrayWrapper> all = new HashSet<ByteArrayWrapper>();
        for (ByteBuffer byteBuffer : bblist) {
            all.add(new ByteArrayWrapper(byteBuffer));
        }
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (ByteArrayWrapper bs : all) {
            result.add(bs.get());
        }
        return result;
    }

    public void setCodecPrivateIfNeeded() {
        if (codecPrivateOpaque != null) {
            ByteBuffer dup = codecPrivateOpaque.duplicate();
            Header childAtom = Header.read(dup);
            LeafBox lb = LeafBox.createLeafBox(childAtom, dup);
            getEntries().get(0).add(lb);
        } else {
            if (codec == Codec.H264) {
                List<ByteBuffer> sps = selectUnique(spsList);
                List<ByteBuffer> pps = selectUnique(ppsList);
                if (!sps.isEmpty() && !pps.isEmpty()) {
                    getEntries().get(0).add(H264Utils.createAvcCFromPS(sps, pps, 4));
                } else {
                    Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for h.264 track, missing any SPS/PPS NAL units");
                }
            } else if (codec == Codec.AAC) {
                if (adtsHeader != null) {
                    getEntries().get(0).add(EsdsBox.fromADTS(adtsHeader));
                } else {
                    Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for AAC track, missing any ADTS headers.");
                }
            }
        }
    }

    private static class ByteArrayWrapper {
        private byte[] bytes;

        public ByteArrayWrapper(ByteBuffer bytes) {
            this.bytes = NIOUtils.toArray(bytes);
        }

        public ByteBuffer get() {
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArrayWrapper))
                return false;
            return Platform.arrayEqualsByte(bytes, ((ByteArrayWrapper) obj).bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    void addAudioSampleEntry(AudioFormat format) {
        AudioSampleEntry ase = AudioSampleEntry.compressedAudioSampleEntry(codec2fourcc.get(codec), (short) 1, (short) 16,
                format.getChannels(), format.getSampleRate(), 0, 0, 0);

        addSampleEntry(ase);
    }
}