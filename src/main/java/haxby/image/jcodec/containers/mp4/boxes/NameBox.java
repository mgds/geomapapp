package haxby.image.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import haxby.image.jcodec.common.JCodecUtil2;
import haxby.image.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class NameBox extends Box {
    private String name;

    public static String fourcc() {
        return "name";
    }

    public static NameBox createNameBox(String name) {
        NameBox box = new NameBox(new Header(fourcc()));
        box.name = name;
        return box;
    }

    public NameBox(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        name = NIOUtils.readNullTermString(input);
    }

    protected void doWrite(ByteBuffer out) {
        out.put(JCodecUtil2.asciiString(name));
        out.putInt(0);
    }
    
    @Override
    public int estimateSize() {
        return 12 + JCodecUtil2.asciiString(name).length;
    }

    public String getName() {
        return name;
    }
}
