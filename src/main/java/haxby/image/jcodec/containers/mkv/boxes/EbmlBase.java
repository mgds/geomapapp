package haxby.image.jcodec.containers.mkv.boxes;
import java.io.IOException;
import java.nio.ByteBuffer;

import haxby.image.jcodec.common.UsedViaReflection;
import haxby.image.jcodec.common.io.SeekableByteChannel;
import haxby.image.jcodec.containers.mkv.MKVType;
import haxby.image.jcodec.containers.mkv.util.EbmlUtil;
import haxby.image.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public abstract class EbmlBase {

    protected EbmlMaster parent;
    public MKVType type;
    public byte[] id;
    public int dataLen = 0;
    public long offset;
    public long dataOffset;
    public int typeSizeLength;
    
    @UsedViaReflection
    public EbmlBase(byte[] id) {
        this.id = id;
    }
    
    public boolean equalId(byte[] typeId) {
        return Platform.arrayEqualsByte(this.id, typeId);
    }
    
    public abstract ByteBuffer getData();
    
    public long size() {
        return this.dataLen + EbmlUtil.ebmlLength(dataLen) + id.length;
    }

    public long mux(SeekableByteChannel os) throws IOException {
        ByteBuffer bb = getData();
        return os.write(bb);
    }
    

}
