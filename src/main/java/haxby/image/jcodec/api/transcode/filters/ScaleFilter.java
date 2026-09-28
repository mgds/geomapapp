package haxby.image.jcodec.api.transcode.filters;

import haxby.image.jcodec.api.transcode.Filter;
import haxby.image.jcodec.api.transcode.PixelStore;
import haxby.image.jcodec.api.transcode.PixelStore.LoanerPicture;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.scale.BaseResampler;
import haxby.image.jcodec.scale.LanczosResampler;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Scales image to a different size.
 * 
 * @author The JCodec project
 * 
 */
public class ScaleFilter implements Filter {
    private BaseResampler resampler;
    private ColorSpace currentColor;
    private Size currentSize;
    private Size targetSize;
    private int width;
    private int height;

    public ScaleFilter(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size getTarget() {
        return new Size(width, height);
    }

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        Size pictureSize = picture.getSize();
        if (resampler == null || currentColor != picture.getColor() || !pictureSize.equals(currentSize)) {
            currentColor = picture.getColor();
            currentSize = picture.getSize();
            targetSize = new Size(width & currentColor.getWidthMask(), height & currentColor.getHeightMask());
            resampler = new LanczosResampler(currentSize, targetSize);
        }

        LoanerPicture dest = store.getPicture(targetSize.getWidth(), targetSize.getHeight(), currentColor);

        resampler.resample(picture, dest.getPicture());

        return dest;
    }

    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.ANY_PLANAR;
    }

    @Override
    public ColorSpace getOutputColor() {
        return ColorSpace.SAME;
    }
}
