/*
* Copyright 2015 LinkedIn Corp. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/

package com.linkedin.paldb.api;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestConfiguration {

  @Test
  public void testConfiguration() {
    Configuration c = new Configuration();
    c.set("foo", "bar");
    Assert.assertEquals(c.get("foo", null), "bar");
    Assert.assertEquals(c.get("bar", "foo"), "foo");
  }

  @Test
  public void testConfigurationCopy() {
    Configuration c = new Configuration();
    c.set("foo", "bar");

    Configuration r = new Configuration(c);
    Assert.assertEquals(r.get("foo", null), "bar");

    c.set("foo", "");
    Assert.assertEquals(r.get("foo", null), "bar");
  }

  //触发异常测试
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testConfigurationReadOnly() {
    Configuration c = new Configuration();
    c.set("foo", "bar");

    Configuration r = new Configuration(c);//初始化完成之后，不允许修改；
    r.set("foo", "bar");
  }

  @Test
  public void testEqualsEmpty() {
    Assert.assertEquals(new Configuration(), new Configuration());
  }

  @Test
  public void testEquals() {
    Configuration c1 = new Configuration();
    c1.set("foo", "bar");

    Configuration c2 = new Configuration();
    c2.set("foo", "bar");

    Configuration c3 = new Configuration();
    c3.set("foo", "notbar");

    Assert.assertEquals(c1, c2);
    Assert.assertNotEquals(c1, c3);
  }

  @Test
  public void testGetBoolean() {
    Configuration c = new Configuration();
    c.set("foo", "true");
    c.set("bar", "false");

    Assert.assertTrue(c.getBoolean("foo"));
    Assert.assertFalse(c.getBoolean("bar"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetBooleanMissing() {
    new Configuration().getBoolean("foo");
  }

  @Test
  public void testGetBooleanDefault() {
    Configuration c = new Configuration();
    c.set("foo", "true");

    Assert.assertTrue(c.getBoolean("foo", false));
    Assert.assertTrue(c.getBoolean("bar", true));
  }

  @Test
  public void testGetDouble() {
    Configuration c = new Configuration();
    c.set("foo", "1.0");

    Assert.assertEquals(c.getDouble("foo"), 1.0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetDoubleMissing() {
    new Configuration().getDouble("foo");
  }

  @Test
  public void testGetDoubleDefault() {
    Configuration c = new Configuration();
    c.set("foo", "1.0");

    Assert.assertEquals(c.getDouble("foo", 2.0), 1.0);
    Assert.assertEquals(c.getDouble("bar", 2.0), 2.0);
  }

  @Test
  public void testGetFloat() {
    Configuration c = new Configuration();
    c.set("foo", "1.0");

    Assert.assertEquals(c.getFloat("foo"), 1f);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetFloatMissing() {
    new Configuration().getFloat("foo");
  }

  @Test
  public void testGetFloatDefault() {
    Configuration c = new Configuration();
    c.set("foo", "1.0");

    Assert.assertEquals(c.getFloat("foo", 2f), 1f);
    Assert.assertEquals(c.getFloat("bar", 2f), 2f);
  }

  @Test
  public void testGetInt() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getInt("foo"), 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetIntMissing() {
    new Configuration().getInt("foo");
  }

  @Test
  public void testGetIntDefault() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getInt("foo", 2), 1);
    Assert.assertEquals(c.getInt("bar", 2), 2);
  }

  @Test
  public void testGetShort() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getShort("foo"), (short) 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetShortMissing() {
    new Configuration().getShort("foo");
  }

  @Test
  public void testGetShortDefault() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getShort("foo", (short) 2), (short) 1);
    Assert.assertEquals(c.getShort("bar", (short) 2), (short) 2);
  }

  @Test
  public void testGetLong() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getLong("foo"), 1l);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetLongMissing() {
    new Configuration().getLong("foo");
  }

  @Test
  public void testGetLongDefault() {
    Configuration c = new Configuration();
    c.set("foo", "1");

    Assert.assertEquals(c.getLong("foo", 2l), 1l);
    Assert.assertEquals(c.getLong("bar", 2l), 2l);
  }

  @Test
  public void testGetClass() //测试类是否一致
      throws ClassNotFoundException {
    Configuration c = new Configuration();
    c.set("foo", Integer.class.getName());

    Assert.assertEquals(c.getClass("foo"), Integer.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetClassMissing()
      throws ClassNotFoundException {
    new Configuration().getClass("foo");
  }

  @Test
  public void testGetList() {
    Configuration c = new Configuration();
    c.set("foo", "foo,bar");

    Assert.assertEquals(c.getList("foo"), Arrays.asList("foo", "bar"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetListMissing() {
    new Configuration().getList("foo");
  }

  @Test
  public void testGetListDefault() {
    Configuration c = new Configuration();
    c.set("foo", "foo,bar");

    Assert.assertEquals(c.getList("foo", Arrays.asList("that")), Arrays.asList("foo", "bar"));
    Assert.assertEquals(c.getList("bar", Arrays.asList("that")), Arrays.asList("that"));
  }

  @Test
  public void testSerialization() //测试序列化配置文件
      throws Throwable {
    Configuration c = new Configuration();
    c.set("foo", "bar");
    c.registerSerializer(new PointSerializer());

    //字节数组输出流在内存中创建一个字节数组缓冲区，所有发送到输出流的数据保存在该字节数组缓冲区中。下面创建一个32字节（默认大小）的缓冲区。
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //也可以输出到文件里面；FileOutputStream fos = new FileOutputStream("t.tmp");
        //对象的输出流将指定的对象写入到文件的过程，就是将对象序列化的过程;对象输出流；
    ObjectOutputStream out = new ObjectOutputStream(bos);
    out.writeObject(c);

    out.close();
    bos.close();

    //创建一个新分配的字节数组。数组的大小和当前输出流的大小，内容是当前输出流的拷贝。
    byte[] bytes = bos.toByteArray();
    //字节数组输入流在内存中创建一个字节数组缓冲区，从输入流读取的数据保存在该字节数组缓冲区中。
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    //ObjectOutputStream会在文件中的开头和结尾进行一个标识AC/DC，ObjectInputStream会严格的根据开头和结尾来进行读取，当读取不到的时候就会进行报错
    ObjectInputStream in = new ObjectInputStream(bis);
    Configuration sc = (Configuration) in.readObject();

    in.close();
    bis.close();

    Assert.assertEquals(sc, c);
  }

  // UTILITY

  public static class PointSerializer implements Serializer<Point> {

    @Override
    public Point read(DataInput input) {
      return null;
    }

    @Override
    public void write(DataOutput output, Point input) {

    }

    @Override
    public int getWeight(Point instance) {
      return 0;
    }
  }
}
