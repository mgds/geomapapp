package haxby.image.jcodec.api.specific;

import haxby.image.jcodec.api.MediaInfo;
import haxby.image.jcodec.common.model.Packet;
import haxby.image.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface ContainerAdaptor {

    Picture decodeFrame(Packet packet, byte[][] data);

    boolean canSeek(Packet data);

    byte[][] allocatePicture();

    MediaInfo getMediaInfo();
}
