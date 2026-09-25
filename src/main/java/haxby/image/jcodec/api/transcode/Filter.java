package haxby.image.jcodec.api.transcode;

import haxby.image.jcodec.api.transcode.PixelStore.LoanerPicture;
import haxby.image.jcodec.common.model.ColorSpace;
import haxby.image.jcodec.common.model.Picture;

/**
 * Filters the decoded image before it gets to encoder.
 * 
 * @author stan
 */
public interface Filter {
    LoanerPicture filter(Picture picture, PixelStore store);
    
    /**
     * The color space that this filter supports on the input. null indicates any color space is taken.
     * @return
     */
    ColorSpace getInputColor();
    
    ColorSpace getOutputColor();
}