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

package com.linkedin.paldb.utils;

import com.google.common.hash.Hashing;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestHashUtils {
  HashUtils hashUtils = new HashUtils();

  @Test
  public void testHashEquals() {
    Assert.assertEquals(hashUtils.hash("foo".getBytes()), hashUtils.hash("foo".getBytes()));
    System.out.println(hashUtils.hash("foobar".getBytes()));
  }

  @Test
  public void testEmpty() {
    Assert.assertTrue(hashUtils.hash(new byte[0]) > 0);
  }

  @Test
  public void test() {
    String input = "hello, world";
    // 计算MD5
    System.out.println(Hashing.md5().hashBytes(input.getBytes()).toString());
    // 计算sha256
    System.out.println(Hashing.sha256().hashBytes(input.getBytes()).toString());
    // 计算sha512
    System.out.println(Hashing.sha512().hashBytes(input.getBytes()).toString());
    // 计算crc32
    System.out.println(Hashing.crc32().hashBytes(input.getBytes()).toString());

    System.out.println(Hashing.md5().hashUnencodedChars(input).toString());

    System.out.println("murmur:begin");
    System.out.println(Hashing.murmur3_128().hashBytes(input.getBytes()).toString());
    int i = Hashing.murmur3_128().hashBytes(input.getBytes()).hashCode();
    System.out.println(i);
    System.out.println(i %16);
//    位计算取余的两种方式，计算更快 长度最好要是2的n次方 用于奇偶判断 (!(n&(n-1)) )&& n
    //因为16是2的整数次幂的原因，在小数据量的情况下16比15和20更能减少key之间的碰撞，而加快查询的效率。
    //那么hashmap什么时候进行扩容呢？当hashmap中的元素个数超过数组大小*loadFactor时，就会进行数组扩容，
    // loadFactor的默认值为0.75，也就是说，默认情况下，数组大小为16，那么当hashmap中元素个数超过16*0.75=12的时候，
    // 就把数组的大小扩展为2*16=32，即扩大一倍，然后重新计算每个元素在数组中的位置，而这是一个非常消耗性能的操作，
    // 所以如果我们已经预知hashmap中元素的个数，那么预设元素的个数能够有效的提高hashmap的性能。比如说，
    // 我们有1000个元素new HashMap(1000), 但是理论上来讲new HashMap(1024)更合适，不过上面annegu已经说过，即使是1000，
    // hashmap也自动会将其设置为1024。 但是new HashMap(1024)还不是更合适的，因为0.75*1000 < 1000,
    // 也就是说为了让0.75 * size > 1000, 我们必须这样new HashMap(2048)才最合适，既考虑了&的问题，也避免了resize的问题。

    System.out.println(indexFor(i,16));
    System.out.println(tableSizeFor(15));
    System.out.println(tableSizeFor(18));
    System.out.println(tableSizeFor(1000));
    System.out.println(tableSizeFor(2000));

    System.out.println("murmur:end");


    System.out.println(Hashing.sipHash24().hashBytes(input.getBytes()).toString());

    System.out.println(Hashing.goodFastHash(64).hashBytes(input.getBytes()).toString());
    System.out.println(Hashing.goodFastHash(64).hashBytes(input.getBytes()).hashCode());

  }

  //位计算取模
  static int indexFor(int h, int length) {
    return h & (length-1);
  }

  // 计算出大于 initialCapacity 的最小的 2 的 n 次方值。
  static final int tableSizeFor(int cap) {
    int n = cap - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >=Integer.MAX_VALUE) ?     Integer.MAX_VALUE : n + 1;
  }


}
