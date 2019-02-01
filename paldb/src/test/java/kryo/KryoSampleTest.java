package kryo;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.testng.Assert;
import org.testng.annotations.Test;

public class KryoSampleTest {
    Logger LOG=Logger.getLogger(this.getClass().getName());

    /**
     * 序列化kryo 是java 1/5
     * @throws IOException
     */
    @Test
    public  void kryoVsJava() throws IOException {
        Map someObject = new HashMap();
        someObject.put("abc","测试序列化");
//        someObject.put("cde",123456789);
//        someObject.put("efg",Long.parseLong("1234567890"));
//        someObject.put("ghj",Double.parseDouble("1234567890.90"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
//        objectOutputStream.writeObject(someObject);
//        objectOutputStream.writeObject(1234567890);
//        objectOutputStream.writeObject("测试序列化");
        objectOutputStream.writeObject("abcdeabcde");

        objectOutputStream.flush();
        objectOutputStream.close();

        Kryo kryo = new Kryo();
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        Output output = new Output(outputStream2);
//        kryo.writeObject(output, someObject);
//        kryo.writeObject(output,1234567890);
//        kryo.writeObject(output,"测试序列化");
        kryo.writeObject(output,"abcdeabcde");
        output.flush();
        output.close();

        Double round = Double.valueOf(outputStream2.size()) / outputStream.size();
        LOG.log(Level.INFO,String.format("%.2f", round));

        LOG.log(Level.INFO,"{}"+outputStream2.toByteArray().length);
        LOG.log(Level.INFO,"{}"+new String(outputStream2.toByteArray()));
        LOG.log(Level.INFO,"{}"+new String(outputStream.toByteArray()));
        LOG.log(Level.INFO,"{}"+outputStream.toByteArray().length);

    }
    @Test
    public  void s() throws IOException {
        // 序列化
        Kryo kryo = new Kryo();
//        Output output = new Output(new FileOutputStream("file.bin"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);



        Map someObject = new HashMap();
        someObject.put("abc","测试序列化");
        kryo.writeObject(output, someObject);

        kryo.writeObject(output,"字符串");
        kryo.writeObject(output,123);
        kryo.writeObject(output,Long.parseLong("456"));
        kryo.writeObject(output,Double.parseDouble("7.89"));

        output.flush();
        output.close();
        outputStream.flush();
        outputStream.close();


        System.out.println(outputStream.size());
        System.out.println(outputStream.toByteArray().length);


        // 反序列化
//        Input input = new Input(new FileInputStream("file.bin"));
        Input input = new Input(new ByteArrayInputStream(outputStream.toByteArray()));
        Map message = kryo.readObject(input, HashMap.class);
        String message1 = kryo.readObject(input, String.class);
        int message2 = kryo.readObject(input, Integer.class);
        long message3 = kryo.readObject(input, Long.class);
        double message4 = kryo.readObject(input, Double.class);
        System.out.println(message.get("abc"));
        System.out.println(message1);
        System.out.println(message2);
        System.out.println(message3);
        System.out.println(message4);

        Assert.assertEquals(message.get("abc"),"测试序列化");
        Assert.assertEquals(message1,"字符串");
        Assert.assertEquals(message2,123);
        Assert.assertEquals(message3,456);
        Assert.assertEquals(message4,7.89);

        outputStream.flush();
        outputStream.close();

        input.close();
    }
}