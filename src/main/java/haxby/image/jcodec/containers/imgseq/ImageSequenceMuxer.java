package haxby.image.jcodec.containers.imgseq;

import static haxby.image.jcodec.common.tools.MainUtils.tildeExpand;
import static java.lang.String.format;

import java.io.IOException;

import haxby.image.jcodec.common.AudioCodecMeta;
import haxby.image.jcodec.common.Codec;
import haxby.image.jcodec.common.Muxer;
import haxby.image.jcodec.common.MuxerTrack;
import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.common.model.Packet;

/**
 * A muxer and muxer track that simply saves each buffer as a file.
 * @author Stanislav Vitvitskiy
 */
public class ImageSequenceMuxer implements Muxer, MuxerTrack {
    private String fileNamePattern;
    private int frameNo;
    
    public ImageSequenceMuxer(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    @Override
    public void addFrame(Packet packet) throws IOException {
        NIOUtils.writeTo(packet.getData(), tildeExpand(format(fileNamePattern, frameNo++)));
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        return this;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        Logger.warn("Audio is not supported for image sequence muxer.");
        return null;
    }

    @Override
    public void finish() throws IOException {
        // NOP
    }
}
