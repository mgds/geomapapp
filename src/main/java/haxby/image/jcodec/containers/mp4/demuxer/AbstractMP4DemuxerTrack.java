package haxby.image.jcodec.containers.mp4.demuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import haxby.image.jcodec.common.DemuxerTrackMeta;
import haxby.image.jcodec.common.SeekableDemuxerTrack;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.common.model.RationalLarge;
import haxby.image.jcodec.containers.mp4.MP4Packet;
import haxby.image.jcodec.containers.mp4.MP4TrackType;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import haxby.image.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import haxby.image.jcodec.containers.mp4.boxes.Edit;
import haxby.image.jcodec.containers.mp4.boxes.EditListBox;
import haxby.image.jcodec.containers.mp4.boxes.NameBox;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleEntry;
import haxby.image.jcodec.containers.mp4.boxes.SampleToChunkBox;
import haxby.image.jcodec.containers.mp4.boxes.TimeToSampleBox;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import haxby.image.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Shared routines between PCM and Frames tracks
 * 
 * @author The JCodec project
 * 
 */
public abstract class AbstractMP4DemuxerTrack implements SeekableDemuxerTrack {
    protected TrakBox box;
    private MP4TrackType type;
    private int no;
    protected SampleEntry[] sampleEntries;

    protected TimeToSampleEntry[] timeToSamples;
    protected SampleToChunkEntry[] sampleToChunks;
    protected long[] chunkOffsets;

    protected long duration;

    protected int sttsInd;
    protected int sttsSubInd;

    protected int stcoInd;

    protected int stscInd;

    protected long pts;
    protected long curFrame;
    protected int timescale;

    public AbstractMP4DemuxerTrack(TrakBox trak) {
        no = trak.getTrackHeader().getNo();
        type = TrakBox.getTrackType(trak);
        sampleEntries = NodeBox.findAllPath(trak, SampleEntry.class, new String[]{"mdia", "minf", "stbl", "stsd", null});

        NodeBox stbl = trak.getMdia().getMinf().getStbl();

        TimeToSampleBox stts = NodeBox.findFirst(stbl, TimeToSampleBox.class, "stts");
        SampleToChunkBox stsc = NodeBox.findFirst(stbl, SampleToChunkBox.class, "stsc");
        ChunkOffsetsBox stco = NodeBox.findFirst(stbl, ChunkOffsetsBox.class, "stco");
        ChunkOffsets64Box co64 = NodeBox.findFirst(stbl, ChunkOffsets64Box.class, "co64");

        timeToSamples = stts.getEntries();
        sampleToChunks = stsc.getSampleToChunk();
        chunkOffsets = stco != null ? stco.getChunkOffsets() : co64.getChunkOffsets();

        for (int i = 0; i < timeToSamples.length; i++) {
            TimeToSampleEntry ttse = timeToSamples[i];
            duration += ttse.getSampleCount() * ttse.getSampleDuration();
        }
        box = trak;

        timescale = trak.getTimescale();
    }

    public int pts2Sample(long _tv, int _timescale) {
        long tv = _tv * timescale / _timescale;
        int ttsInd, sample = 0;
        for (ttsInd = 0; ttsInd < timeToSamples.length - 1; ttsInd++) {
            int a = timeToSamples[ttsInd].getSampleCount() * timeToSamples[ttsInd].getSampleDuration();
            if (tv < a)
                break;
            tv -= a;
            sample += timeToSamples[ttsInd].getSampleCount();
        }
        return sample + (int) (tv / timeToSamples[ttsInd].getSampleDuration());
    }

    public MP4TrackType getType() {
        return type;
    }

    public int getNo() {
        return no;
    }

    public SampleEntry[] getSampleEntries() {
        return sampleEntries;
    }

    public TrakBox getBox() {
        return box;
    }

    public long getTimescale() {
        return timescale;
    }

    protected abstract void seekPointer(long frameNo);

    public boolean canSeek(long pts) {
        return pts >= 0 && pts < duration;
    }

    public synchronized boolean seekPts(long pts) {
        if (pts < 0)
            throw new IllegalArgumentException("Seeking to negative pts");
        if (pts >= duration)
            return false;

        long prevDur = 0;
        int frameNo = 0;
        for (sttsInd = 0; pts > prevDur + timeToSamples[sttsInd].getSampleCount()
                * timeToSamples[sttsInd].getSampleDuration()
                && sttsInd < timeToSamples.length - 1; sttsInd++) {
            prevDur += timeToSamples[sttsInd].getSampleCount() * timeToSamples[sttsInd].getSampleDuration();
            frameNo += timeToSamples[sttsInd].getSampleCount();
        }
        sttsSubInd = (int) ((pts - prevDur) / timeToSamples[sttsInd].getSampleDuration());
        frameNo += sttsSubInd;
        this.pts = prevDur + timeToSamples[sttsInd].getSampleDuration() * sttsSubInd;

        seekPointer(frameNo);

        return true;
    }

    protected void shiftPts(long frames) {
        pts -= sttsSubInd * timeToSamples[sttsInd].getSampleDuration();
        sttsSubInd += frames;
        while (sttsInd < timeToSamples.length - 1 && sttsSubInd >= timeToSamples[sttsInd].getSampleCount()) {
            pts += timeToSamples[sttsInd].getSegmentDuration();
            sttsSubInd -= timeToSamples[sttsInd].getSampleCount();
            sttsInd++;
        }
        pts += sttsSubInd * timeToSamples[sttsInd].getSampleDuration();
    }

    protected void nextChunk() {
        if (stcoInd >= chunkOffsets.length)
            return;
        stcoInd++;

        if ((stscInd + 1 < sampleToChunks.length) && stcoInd + 1 == sampleToChunks[stscInd + 1].getFirst()) {
            stscInd++;
        }
    }
    
    @Override
    public synchronized boolean gotoFrame(long frameNo) {
        if (frameNo < 0)
            throw new IllegalArgumentException("negative frame number");
        if (frameNo >= getFrameCount())
            return false;
        if (frameNo == curFrame)
            return true;

        seekPointer(frameNo);
        seekFrame(frameNo);

        return true;
    }
    
    @Override
    public void seek(double second) {
        seekPts((long) (second * timescale));
    }

    private void seekFrame(long frameNo) {
        pts = sttsInd = sttsSubInd = 0;
        shiftPts(frameNo);
    }

    public RationalLarge getDuration() {
        return new RationalLarge(box.getMediaDuration(), box.getTimescale());
    }

    public abstract long getFrameCount();

    @Override
    public long getCurFrame() {
        return curFrame;
    }

    public List<Edit> getEdits() {
        EditListBox editListBox = NodeBox.findFirstPath(box, EditListBox.class, Box.path("edts.elst"));
        if (editListBox != null)
            return editListBox.getEdits();
        return null;
    }

    public String getName() {
        NameBox nameBox = NodeBox.findFirstPath(box, NameBox.class, Box.path("udta.name"));
        return nameBox != null ? nameBox.getName() : null;
    }

    public String getFourcc() {
        SampleEntry[] entries = getSampleEntries();
        SampleEntry se = entries == null || entries.length == 0 ? null : entries[0];
        String fourcc = se == null ? null : se.getHeader().getFourcc();
        return fourcc;
    }

    protected ByteBuffer readPacketData(SeekableByteChannel input, ByteBuffer buffer, long offset, int size)
            throws IOException {
        ByteBuffer result = buffer.duplicate();
        synchronized (input) {
            input.setPosition(offset);
            NIOUtils.readL(input, result, size);
        }
        ((java.nio.Buffer)result).flip();
        return result;
    }

    public abstract MP4Packet getNextFrame(ByteBuffer storage) throws IOException;
    
    public ByteBuffer convertPacket(ByteBuffer _in) {
        return _in;
    }
    
    @Override
    public DemuxerTrackMeta getMeta() {
        return MP4DemuxerTrackMeta.fromTrack(this);
    }
}