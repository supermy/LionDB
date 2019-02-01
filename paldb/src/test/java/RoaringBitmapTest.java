import com.esotericsoftware.kryo.Kryo;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;
import org.roaringbitmap.longlong.LongBitmapDataProvider;
import org.roaringbitmap.longlong.LongIterator;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RoaringBitmapTest {

    @Test
    public  void basic() {
        {
            RoaringBitmap rr = RoaringBitmap.bitmapOf(1, 2, 3, 1000);

//            rr.deserialize();
//            rr.serialize();


            RoaringBitmap rr2 = new RoaringBitmap();
            rr2.add(4000L, 4255L);

//            System.out.println(rr.select(3)); // would return the third value or 1000
            assertEquals(rr.select(3),1000);
//            System.out.println(rr.rank(2)); // would return the rank of 2, which is index 1
            assertEquals(rr.rank(2),2);

            assertTrue(rr.contains(1000)); // will return true
            assertFalse(rr.contains(7)); // will return false

            RoaringBitmap rror = RoaringBitmap.or(rr, rr2);// new bitmap
            rr.or(rr2); //in-place computation
            boolean equals = rror.equals(rr);// true
            assertTrue(equals);
            assertEquals(rror,rr);
            if (!equals) throw new RuntimeException("bug");

            // number of values stored?
            long cardinality = rr.getLongCardinality();
            System.out.println(cardinality);
            assertEquals(cardinality,259);
            // a "forEach" is faster than this loop, but a loop is possible:
            for (int i : rr) {
                System.out.println(i);
            }
        }


    }


    @Test
    public  void mutable() throws IOException {

        {
            MutableRoaringBitmap rr1 = MutableRoaringBitmap.bitmapOf(1, 2, 3, 1000);
            MutableRoaringBitmap rr2 = MutableRoaringBitmap.bitmapOf( 2, 3, 1010);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            // If there were runs of consecutive values, you could
            // call rr1.runOptimize(); or rr2.runOptimize(); to improve compression
            rr1.serialize(dos);
            rr2.serialize(dos);
            dos.close();

//            RoaringSerializer rs=new RoaringSerializer();
//            rs.write(new Kryo(),dos,rr1);

            System.out.println(bos.toByteArray().length);
            System.out.println(Snappy.compress(bos.toByteArray()).length);

            ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray());
            ImmutableRoaringBitmap rrback1 = new ImmutableRoaringBitmap(bb);

            bb.position(bb.position() + rrback1.serializedSizeInBytes());
            ImmutableRoaringBitmap rrback2 = new ImmutableRoaringBitmap(bb);


            Assert.assertEquals(rr1,rrback1);
            Assert.assertEquals(rr2,rrback2);
            System.out.println(rr1);
            System.out.println(rrback1);
            System.out.println(rr2);
            System.out.println(rrback2);

        }


    }

    @Test
    public  void longbit() {

        {
            LongBitmapDataProvider r = Roaring64NavigableMap.bitmapOf(1,2,100,1000);
            r.addLong(1234);
            assertTrue(r.contains(1)); // true
            assertFalse(r.contains(3)); // false
            LongIterator i = r.getLongIterator();
            while(i.hasNext()) System.out.println(i.next());
        }
    }
}
