import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.KryoDataInput;
import com.esotericsoftware.kryo.io.KryoDataOutput;
import com.esotericsoftware.kryo.io.Output;
import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;

/**
 * kryo 结合 roaringbitmap 序列化
 */
public class RoaringSerializer extends Serializer<RoaringBitmap> {

    @Override
    public void write(Kryo kryo, Output output, RoaringBitmap bitmap) {
        try {
            bitmap.serialize(new KryoDataOutput(output));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     *
     * roaringbitmap序列化
     *
     * @param kryo
     * @param input
     * @param type
     * @return
     */
    @Override
    public RoaringBitmap read(Kryo kryo, Input input, Class<RoaringBitmap> type) {
        RoaringBitmap bitmap = new RoaringBitmap();
        try {
            bitmap.deserialize(new KryoDataInput(input));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return bitmap;
    }

}