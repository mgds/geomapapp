package haxby.image.jcodec.containers.mp4.demuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.TrackType;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Packet.FrameType;
import haxby.image.jcodec.containers.mp4.MP4Packet;
import haxby.image.jcodec.containers.mp4.QTTimeUtil;
import haxby.image.jcodec.containers.mp4.boxes.AudioSampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.MovieBox;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.SampleSizesBox;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Specialized demuxer track for PCM audio samples
 * 
 * Always reads one chunk of frames at a time, except for after seek. After seek
 * the beginning of chunk before the seek point is not read effectivaly reading
 * PCM frame from exactly the frame seek was performed to.
 * 
 * Packet size depends on underlying container PCM chunk sizes.
 * 
 * @author The JCodec project
 * 
 */
public class PCMMP4DemuxerTrack extends AbstractMP4DemuxerTrack {

    private int defaultSampleSize;

    private int posShift;

    protected int totalFrames;

    private SeekableByteChannel input;

    private MovieBox movie;

    public PCMMP4DemuxerTrack(MovieBox movie, TrakBox trak, SeekableByteChannel input) {
        super(trak);

        this.movie = movie;
        this.input = input;
        SampleSizesBox stsz = NodeBox.findFirstPath(trak, SampleSizesBox.class, Box.path("mdia.minf.stbl.stsz"));
        defaultSampleSize = stsz.getDefaultSize();

        int chunks = 0;
        for (int i = 1; i < sampleToChunks.length; i++) {
            int ch = (int) (sampleToChunks[i].getFirst() - sampleToChunks[i - 1].getFirst());
            totalFrames += ch * sampleToChunks[i - 1].getCount();
            chunks += ch;
        }
        totalFrames += sampleToChunks[sampleToChunks.length - 1].getCount() * (chunkOffsets.length - chunks);
    }

    @Override
    public Packet nextFrame() throws IOException {
        int frameSize = getFrameSize();
        int chSize = sampleToChunks[stscInd].getCount() * frameSize - posShift;

        return getNextFrame(ByteBuffer.allocate(chSize));
    }

    @Override
    public synchronized MP4Packet getNextFrame(ByteBuffer buffer) throws IOException {
        if (stcoInd >= chunkOffsets.length)
            return null;
        int frameSize = getFrameSize();

        int se = sampleToChunks[stscInd].getEntry();
        int chSize = sampleToChunks[stscInd].getCount() * frameSize;

        long pktOff = chunkOffsets[stcoInd] + posShift;
        int pktSize = chSize - posShift;
        ByteBuffer result = readPacketData(input, buffer, pktOff, pktSize);

        long ptsRem = pts;
        int doneFrames = pktSize / frameSize;
        shiftPts(doneFrames);

        MP4Packet pkt = new MP4Packet(result, QTTimeUtil.mediaToEdited(box, ptsRem, movie.getTimescale()), timescale,
                (int) (pts - ptsRem), curFrame, FrameType.KEY, null, 0, ptsRem, se - 1, pktOff, pktSize, true);

        curFrame += doneFrames;

        posShift = 0;

        ++stcoInd;
        if (stscInd < sampleToChunks.length - 1 && (stcoInd + 1) == sampleToChunks[stscInd + 1].getFirst())
            stscInd++;

        return pkt;
    }

    @Override
    public boolean gotoSyncFrame(long frameNo) {
        return gotoFrame(frameNo);
    }

    public int getFrameSize() {
        SampleEntry entry = sampleEntries[sampleToChunks[stscInd].getEntry() - 1];
        if (entry instanceof AudioSampleEntry && defaultSampleSize == 0) {
            return ((AudioSampleEntry) entry).calcFrameSize();
        } else {
            return defaultSampleSize;
        }
    }

    @Override
    protected void seekPointer(long frameNo) {
        for (stcoInd = 0, stscInd = 0, curFrame = 0;;) {
            long nextFrame = curFrame + sampleToChunks[stscInd].getCount();
            if (nextFrame > frameNo)
                break;
            curFrame = nextFrame;
            nextChunk();
        }
        posShift = (int) ((frameNo - curFrame) * getFrameSize());
        curFrame = frameNo;
    }

    @Override
    public long getFrameCount() {
        return totalFrames;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        if (getSampleEntries()[0] instanceof AudioSampleEntry) {
            AudioSampleEntry ase = (AudioSampleEntry) getSampleEntries()[0];
            AudioCodecMeta audioCodecMeta = haxby.image.jcodec.common.AudioCodecMeta.fromAudioFormat(ase.getFormat());
            return new DemuxerTrackMeta(TrackType.AUDIO, Codec.codecByFourcc(getFourcc()), (double) duration / timescale, null, totalFrames,
                null, null, audioCodecMeta);
        } else {
            System.out.println("stan");
            return null;
        }
    }
}