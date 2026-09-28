package haxby.image.jcodec.api.transcode;

import java.io.IOException;

import haxby.image.jcodec.common.model.Packet;


/**
 * A source for compressed video/audio frames.
 * 
 * @author Stanislav Vitvitskiy
 */
public interface PacketSource {

    Packet inputVideoPacket() throws IOException;

    Packet inputAudioPacket() throws IOException;
    
}
