import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * ThreadLocal，很多地方叫做线程本地变量，也有些地方叫做线程本地存储，其实意思差不多。可能很多朋友都知道ThreadLocal
 * 为变量在每个线程中都创建了一个副本，那么每个线程可以访问自己内部的副本变量。
 *
 * 　　最常见的ThreadLocal使用场景为 用来解决 数据库连接、Session管理等。
 *
 */
public class ThreadLocalTest {
    ThreadLocal<Long> longLocal = new ThreadLocal<Long>();
    ThreadLocal<String> stringLocal = new ThreadLocal<String>();


    public void set() {
        longLocal.set(Thread.currentThread().getId());
        stringLocal.set(Thread.currentThread().getName());
    }

    public long getLong() {
        return longLocal.get();
    }

    public String getString() {
        return stringLocal.get();
    }



//    private static final ThreadLocal threadSession = new ThreadLocal();
//
//    public static Session getSession() throws InfrastructureException {
//        Session s = (Session) threadSession.get();
//        try {
//            if (s == null) {
//                s = getSessionFactory().openSession();
//                threadSession.set(s);
//            }
//        } catch (HibernateException ex) {
//            throw new InfrastructureException(ex);
//        }
//        return s;
//    }


    @Test
    public void thread() throws InterruptedException {
        final ThreadLocalTest test = new ThreadLocalTest();


        test.set();
        final long aLong = test.getLong();
        System.out.println(aLong);
        final String s = test.getString();
        System.out.println(s);


        Thread thread1 = new Thread(){
            public void run() {
                test.set();
                Assert.assertNotEquals(aLong,test.getLong());
                Assert.assertNotEquals(s,test.getString());

                System.out.println(test.getLong());
                System.out.println(test.getString());
            };
        };
        thread1.start();
        thread1.join();//作用是一直等待该线程死亡

        Assert.assertEquals(aLong,test.getLong());
        Assert.assertEquals(s,test.getString());

        System.out.println(test.getLong());
        System.out.println(test.getString());
    }



}
