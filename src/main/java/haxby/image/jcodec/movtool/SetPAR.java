package haxby.image.jcodec.movtool;

import java.io.File;

import haxby.image.jcodec.common.model.Rational;
import haxby.image.jcodec.common.model.Size;
import haxby.image.jcodec.containers.mp4.BoxUtil;
import haxby.image.jcodec.containers.mp4.boxes.Box;
import haxby.image.jcodec.containers.mp4.boxes.MovieBox;
import haxby.image.jcodec.containers.mp4.boxes.MovieFragmentBox;
import haxby.image.jcodec.containers.mp4.boxes.NodeBox;
import haxby.image.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import haxby.image.jcodec.containers.mp4.boxes.TrakBox;
import haxby.image.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SetPAR {
    public static void main1(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: setpasp <movie> <num:den>");
            System.exit(-1);
        }
        final Rational newPAR = Rational.parse(args[1]);

        new InplaceMP4Editor().modify(new File(args[0]), new MP4Edit() {

            @Override
            public void apply(MovieBox mov) {
                TrakBox vt = mov.getVideoTrack();
                vt.setPAR(newPAR);
                Box box = NodeBox.findFirstPath(vt, SampleDescriptionBox.class, Box.path("mdia.minf.stbl.stsd")).getBoxes()
                        .get(0);
                if (box != null && (box instanceof VideoSampleEntry)) {
                    VideoSampleEntry vs = (VideoSampleEntry) box;
                    int codedWidth = (int) vs.getWidth();
                    int codedHeight = (int) vs.getHeight();
                    int displayWidth = codedWidth * newPAR.getNum() / newPAR.getDen();

                    vt.getTrackHeader().setWidth(displayWidth);

                    if (BoxUtil.containsBox(vt, "tapt")) {
                        vt.setAperture(new Size(codedWidth, codedHeight), new Size(displayWidth, codedHeight));
                    }
                }
            }

            @Override
            public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
                throw new RuntimeException("Unsupported");
            }
        });
    }
}
