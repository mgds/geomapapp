package haxby.image.jcodec.api.transcode.filters;

import haxby.image.jcodec.api.transcode.Filter;
import haxby.image.jcodec.api.transcode.PixelStore;
import haxby.image.jcodec.api.transcode.PixelStore.LoanerPicture;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

public class CropFilter implements Filter {

    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        return null;
    }

    @Override
    public ColorSpace getInputColor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ColorSpace getOutputColor() {
        // TODO Auto-generated method stub
        return null;
    }
}
