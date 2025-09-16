package haxby.image.jcodec.javase.codecs.pngawt;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import haxby.image.jcodec.common.VideoCodecMeta;
import haxby.image.jcodec.common.VideoDecoder;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.javase.scale.AWTUtil;

/**
 * Video decoder wrapper to Java SE PNG functionality.
 * 
 * @author Stanislav Vitvitskyy
 */
public class PNGDecoder extends VideoDecoder {

    @Override
    public Picture decodeFrame(ByteBuffer data, byte[][] buffer) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return AWTUtil.fromBufferedImageRGB(rgb);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        try {
            BufferedImage rgb = ImageIO.read(new ByteArrayInputStream(NIOUtils.toArray(data)));
            return VideoCodecMeta.createSimpleVideoCodecMeta(new Size(rgb.getWidth(), rgb.getHeight()), ColorSpace.RGB);
        } catch (IOException e) {
            return null;
        }
    }
}
