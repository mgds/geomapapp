package haxby.image.jcodec.containers.mp4;

import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.Header;

public interface IBoxFactory {

    Box newBox(Header header);
}