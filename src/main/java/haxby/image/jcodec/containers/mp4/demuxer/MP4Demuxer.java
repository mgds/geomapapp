package haxby.image.jcodec.containers.mp4.demuxer;

import static haxby.image.jcodec.common.Fourcc.free;
import static haxby.image.jcodec.common.Fourcc.ftyp;
import static haxby.image.jcodec.common.Fourcc.mdat;
import static haxby.image.jcodec.common.Fourcc.moov;
import static haxby.image.jcodec.common.Fourcc.wide;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Demuxer;
import haxby.image.jcodec.common.DemuxerTrack;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.SeekableDemuxerTrack;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.UsedViaReflection;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.containers.mp4.MP4TrackType;
import haxby.image.jcodec.containers.mp4.MP4Util;
import haxby.image.jcodec.containers.mp4.MP4Util.Movie;
import haxby.image.jcodec.containers.mp4.boxes.AudioSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.HandlerBox;
import haxby.image.jcodec.containers.mp4.boxes.MovieBox;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.SampleSizesBox;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;
import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for MP4
 * 
 * @author The JCodec project
 * 
 */
public class MP4Demuxer implements Demuxer {

    private List<SeekableDemuxerTrack> tracks;
    private TimecodeMP4DemuxerTrack timecodeTrack;
    MovieBox movie;
    protected SeekableByteChannel input;

    // modifies h264 to conform to annexb and aac to contain adts header
    public static MP4Demuxer createMP4Demuxer(SeekableByteChannel input) throws IOException {
        return new MP4Demuxer(input);
    }

    // does not modify packets
    public static MP4Demuxer createRawMP4Demuxer(SeekableByteChannel input) throws IOException {
        return new MP4Demuxer(input) {
            @Override
            protected SeekableDemuxerTrack newTrack(TrakBox trak) {
                return new MP4DemuxerTrack(movie, trak, this.input);
            }
        };
    }

    private SeekableDemuxerTrack fromTrakBox(TrakBox trak) {
        SampleSizesBox stsz = NodeBox.findFirstPath(trak, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
        if (stsz == null)
            return null;

        SampleEntry[] sampleEntries = NodeBox.findAllPath(trak, SampleEntry.class,
                new String[] { "mdia", "minf", "stbl", "stsd", null });
        boolean isPCM = sampleEntries.length != 0 ? (sampleEntries[0] instanceof AudioSampleEntry)
                && isPCMCodec(Codec.codecByFourcc(sampleEntries[0].getFourcc())) : false;

        if (stsz.getDefaultSize() != 0 && isPCM)
            return new PCMMP4DemuxerTrack(movie, trak, input);
        return newTrack(trak);
    }

    private boolean isPCMCodec(Codec codec) {
        return codec != null && codec.isPcm();
    }

    protected SeekableDemuxerTrack newTrack(TrakBox trak) {
        return new CodecMP4DemuxerTrack(new MP4DemuxerTrack(movie, trak, input));
    }

    MP4Demuxer(SeekableByteChannel input) throws IOException {
        this.input = input;
        tracks = new LinkedList<SeekableDemuxerTrack>();
        findMovieBox(input);
    }

    private void findMovieBox(SeekableByteChannel input) throws IOException {
        Movie mv = MP4Util.parseFullMovieChannel(input);
        if (mv == null || mv.getMoov() == null)
            throw new IOException("Could not find movie meta information box");
        movie = mv.getMoov();

        processHeader(movie);
    }

    private void processHeader(NodeBox moov) throws IOException {
        TrakBox tt = null;
        TrakBox[] trakBoxs = NodeBox.findAll(moov, TrakBox.class, "trak");
        for (int i = 0; i < trakBoxs.length; i++) {
            TrakBox trak = trakBoxs[i];
            SampleEntry se = NodeBox.findFirstPath(trak, SampleEntry.class,
                    new String[] { "mdia", "minf", "stbl", "stsd", null });
            if (se != null && "tmcd".equals(se.getFourcc())) {
                tt = trak;
            } else {
                SeekableDemuxerTrack trakBox = fromTrakBox(trak);
                if (trakBox != null) {
                    tracks.add(trakBox);
                }
            }
        }
        if (tt != null) {
            DemuxerTrack video = getVideoTrack();
            if (video != null)
                timecodeTrack = new TimecodeMP4DemuxerTrack(movie, tt, input);
        }
    }

    public static MP4TrackType getTrackType(TrakBox trak) {
        HandlerBox handler = NodeBox.findFirstPath(trak, HandlerBox.class, Box.path("mdia.hdlr"));
        return MP4TrackType.fromHandler(handler.getComponentSubType());
    }

    public DemuxerTrack getVideoTrack() {
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                return demuxerTrack;
        }
        return null;
    }

    public MovieBox getMovie() {
        return movie;
    }

    @Override
    public List<SeekableDemuxerTrack> getTracks() {
        return new ArrayList<SeekableDemuxerTrack>(tracks);
    }

    @Override
    public List<DemuxerTrack> getVideoTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                result.add(demuxerTrack);
        }
        return result;
    }

    @Override
    public List<DemuxerTrack> getAudioTracks() {
        ArrayList<DemuxerTrack> result = new ArrayList<DemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : tracks) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.AUDIO)
                result.add(demuxerTrack);
        }
        return result;
    }

    public TimecodeMP4DemuxerTrack getTimecodeTrack() {
        return timecodeTrack;
    }

    @UsedViaReflection
    public static int probe(final ByteBuffer b) {
        ByteBuffer fork = b.duplicate();
        int success = 0;
        int total = 0;
        while (fork.remaining() >= 8) {
            long len = Platform.unsignedInt(fork.getInt());
            int fcc = fork.getInt();
            int hdrLen = 8;
            if (len == 1) {
                len = fork.getLong();
                hdrLen = 16;
            } else if (len < 8)
                break;
            if (fcc == ftyp && len < 64 || fcc == moov && len < 100 * 1024 * 1024 || fcc == free || fcc == mdat
                    || fcc == wide)
                success++;
            total++;
            if (len >= Integer.MAX_VALUE)
                break;
            NIOUtils.skip(fork, (int) (len - hdrLen));
        }

        return total == 0 ? 0 : success * 100 / total;
    }

    @Override
    public void close() throws IOException {
        input.close();
    }
}