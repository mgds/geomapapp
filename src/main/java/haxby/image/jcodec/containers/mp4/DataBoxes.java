package haxby.image.jcodec.containers.mp4;

import haxby.image.jcodec.containers.mp4.boxes.AliasBox;
import haxby.image.jcodec.containers.mp4.boxes.UrlBox;

public class DataBoxes extends Boxes {
    public DataBoxes() {
        mappings.put(UrlBox.fourcc(), UrlBox.class);
        mappings.put(AliasBox.fourcc(), AliasBox.class);
        mappings.put("cios", AliasBox.class);
    }
}