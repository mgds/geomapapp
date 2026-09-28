package haxby.image.jcodec.containers.mp4;

import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.ChannelBox;
import haxby.image.jcodec.containers.mp4.boxes.WaveExtension;

public class AudioBoxes extends Boxes {

    public AudioBoxes() {
        super();
        mappings.put(WaveExtension.fourcc(), WaveExtension.class);
        mappings.put(ChannelBox.fourcc(), ChannelBox.class);
        mappings.put("esds", Box.LeafBox.class);
    }
}