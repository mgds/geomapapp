package haxby.image.jcodec.api.specific;

import haxby.image.jcodec.api.MediaInfo;
import haxby.image.jcodec.common.VideoDecoder;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * High level frame grabber helper.
 * 
 * @author The JCodec project
 * 
 */
public class GenericAdaptor implements ContainerAdaptor {

    private VideoDecoder decoder;

    public GenericAdaptor(VideoDecoder detect) {
        this.decoder = detect;
    }

    @Override
    public Picture decodeFrame(Packet packet, byte[][] data) {
        return decoder.decodeFrame(packet.getData(), data);
    }

    @Override
    public boolean canSeek(Packet data) {
        return true;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return new MediaInfo(new Size(0, 0));
    }

    @Override
    public byte[][] allocatePicture() {
        return Picture.create(1920, 1088, ColorSpace.YUV444).getData();
    }
}
