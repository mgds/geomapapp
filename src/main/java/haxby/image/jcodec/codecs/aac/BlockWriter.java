package haxby.image.jcodec.codecs.aac;

import static haxby.image.jcodec.codecs.aac.BlockType.TYPE_END;

import haxby.image.jcodec.codecs.aac.blocks.Block;
import haxby.image.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Writes blocks to form AAC frame
 * 
 * @author The JCodec project
 * 
 */
public class BlockWriter {
    
    public void nextBlock(BitWriter bits, Block block) {
        bits.writeNBit(block.getType().ordinal(), 3);
        
        if (block.getType() == TYPE_END)
            return;
        
    }

}
