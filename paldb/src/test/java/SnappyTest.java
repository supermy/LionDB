import org.testng.Assert;
import org.testng.annotations.Test;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

public class SnappyTest {
    Logger LOG=Logger.getLogger(this.getClass().getName());

    @Test
    public void testString() throws IOException {
        String input = "Hello snappy-java! Snappy-java is a JNI-based wrapper of Snappy, a fast compresser/decompresser.";
        {
            byte[] compressed = Snappy.compress(input.getBytes("UTF-8"));
            byte[] uncompressed = Snappy.uncompress(compressed);
            String result = new String(uncompressed, "UTF-8");
            System.out.println(result);
            Assert.assertEquals(input, result);
        }

        {
            byte[] compressed = Snappy.compress(input);
            System.out.println(Snappy.uncompressString(compressed));
            Assert.assertEquals(input, Snappy.uncompressString(compressed));
        }

        {
            double[] arr = new double[]{123.456, 234.567, 345.678};
            byte[] compressed = Snappy.compress(arr);
            double[] unarr = Snappy.uncompressDoubleArray(compressed);
            System.out.println(Arrays.toString(unarr));
            Assert.assertEquals(arr, unarr);
        }


    }

    @Test
    public void testStream() throws IOException {

        {
            File file = new File(basePath("paldb")+"CHANGELOG.md"); //待压缩文件
            File out = new File("./", file.getName() + ".snappy"); //压缩结果文件

            byte[] buffer = new byte[1024 * 1024 * 8];
            FileInputStream fi = null;
            FileOutputStream fo = null;
            SnappyOutputStream sout = null;
            try {
                fi = new FileInputStream(file);
                fo = new FileOutputStream(out);
                sout = new SnappyOutputStream(fo);
                while (true) {
                    int count = fi.read(buffer, 0, buffer.length);
                    if (count == -1) {
                        break;
                    }
                    sout.write(buffer, 0, count);
                }
                sout.flush();
            } catch (Throwable ex) {
                ex.printStackTrace();
            } finally {
                if (sout != null) {
                    try {
                        sout.close();
                    } catch (Exception e) {
                    }
                }
                if (fi != null) {
                    try {
                        fi.close();
                    } catch (Exception x) {
                    }
                }
                if (fo != null) {
                    try {
                        fo.close();
                    } catch (Exception x) {
                    }
                }
            }

        }

        {
            File file = new File(basePath("paldb")+"CHANGELOG.md.snappy"); //待解压文件
            File out = new File(basePath("paldb")+"CHANGELOG.snappy.md");  //解压后文件

            byte[] buffer = new byte[1024 * 1024 * 8];
            FileInputStream  fi = null;
            FileOutputStream fo = null;
            SnappyInputStream sin = null;
            try
            {
                fo = new FileOutputStream(out);
                fi = new FileInputStream(file.getPath());
                sin = new SnappyInputStream(fi);

                while(true)
                {
                    int count = sin.read(buffer, 0, buffer.length);
                    if(count == -1) { break; }
                    fo.write(buffer, 0, count);
                }
                fo.flush();
            }
            catch(Throwable ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                if(sin != null) { try { sin.close(); } catch(Exception x) {} }
                if(fi != null) { try { fi.close(); } catch(Exception x) {} }
                if(fo != null) { try { fo.close(); } catch(Exception x) {} }
            }

        }

    }

    /**
     * 项目路径
     *
     * @param url
     * @return
     */
    public  String basePath(String projectName) {
        URL url = this.getClass().getResource("/");

        String path = url.getPath();

        LOG.info(path);

        String basepath = path.substring(0,path.indexOf(projectName));

        LOG.info(basepath);
        return basepath;
//        return path;
    }
}
