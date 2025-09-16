package haxby.image.jcodec.api.transcode.filters;

import haxby.image.jcodec.api.transcode.Filter;
import haxby.image.jcodec.api.transcode.PixelStore;
import haxby.image.jcodec.api.transcode.PixelStore.LoanerPicture;
import haxby.image.jcodec.common.logging.Logger;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;
import haxby.image.jcodec.scale.ColorUtil;
import haxby.image.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Color transform filter.
 * 
 * @author Stanislav Vitvitskyy
 */
public class ColorTransformFilter implements Filter {
    private Transform transform;
    private ColorSpace outputColor;

    public ColorTransformFilter(ColorSpace outputColor) {
        this.outputColor = outputColor;
    }

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        if (transform == null) {
            transform = ColorUtil.getTransform(picture.getColor(), outputColor);
            Logger.debug("Creating transform: " + transform);
        }
        LoanerPicture outFrame = store.getPicture(picture.getWidth(), picture.getHeight(), outputColor);
        outFrame.getPicture().setCrop(picture.getCrop());
        transform.transform(picture, outFrame.getPicture());
        return outFrame;
    }

    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.ANY_PLANAR;
    }

    @Override
    public ColorSpace getOutputColor() {
        return outputColor;
    }
}