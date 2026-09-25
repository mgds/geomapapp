package haxby.image.jcodec.containers.mp4.boxes;

import haxby.image.jcodec.containers.mp4.Boxes;
import haxby.image.jcodec.containers.mp4.IBoxFactory;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.Header;
import haxby.image.jcodec.platform.Platform;

public class SimpleBoxFactory implements IBoxFactory {
    private Boxes boxes;

    public SimpleBoxFactory(Boxes boxes) {
        this.boxes = boxes;
    }

    @Override
    public Box newBox(Header header) {
        Class<? extends Box> claz = boxes.toClass(header.getFourcc());
        if (claz == null)
            return new Box.LeafBox(header);
        Box box = Platform.newInstance(claz, new Object[] { header });
        return box;
    }

}
