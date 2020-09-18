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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Packing utility for non-negative
 * 打包使用程序对于long与int可变长存储
 * <code>long</code> and <code>int</code> values.
 * <p>
 * Originally developed for Kryo by Nathan Sweet. Modified for JDBM by Jan Kotek
 * <p>
 * 最初由Nathan Sweet为Kryo开发。Jan Kotek对JDBM的修改
 * <p>
 * 可变长存储
 * kryo对int和long类型都采用了可变长存储的机制，以int为例，一般需要4个字节去存储，而对kryo来说，可以通过1-5个变长字节去存储，从而避免高位都是0的浪费。
 * 最多需要5个字节存储是因为，在变长存储int过程中，一个字节的8位用来存储有效数字的只有7位，最高位用于标记是否还需读取下一个字节，1表示需要，0表示不需要。
 * 在对string的存储中也有变长存储的应用，string序列化的整体结构为length+内容，那么length也会使用变长int写入字符的长度。
 * <p>
 * <p>
 * “>>>”运算符所作的是无符号的位移处理，它不会将所处理的值的最高位视为正负符号，所以作位移处理时，会直接在空出的高位填入0。当我们要作位移的原始值并非代表数值时（例如：表示颜色图素的值，最高位并非正负号），可能就会需要使用此种无符号的位移。比如：
 * －10>>>2=1073741821
 * -10=1111 1111 1111 1111 1111 1111 1111 0110 (不管原来的“符号”位的值(一长串1)，空上的全部直接填0)
 * 0011 1111 1111 1111 1111 1111 1111 1101=1037341821
 * ~ 按位非（NOT）（一元运算）
 * & 按位与（AND）
 * | 按 位或（OR）
 * ^ 按位异或（XOR）
 * >> 右移
 * >>> 右移，左边空出的位以0填 充
 * 运算符 结果
 * << 左移
 * &= 按位与赋值
 * |= 按位或赋值
 * ^= 按 位异或赋值
 * >>= 右移赋值
 * >>>= 右移赋值，左边空出的位以0填充
 * <<= 左 移赋值
 */
public final class LongPacker {

    // Default constructor
    private LongPacker() {

    }

    /**
     * Pack non-negative long into output stream. It will occupy 1-10 bytes
     * depending on value (lower values occupy smaller space)
     *
     * @param os    the data output
     * @param value the long value
     * @return the number of bytes written
     * @throws IOException if an error occurs with the stream
     */
    static public int packLong(DataOutput os, long value)
            throws IOException {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }


//    0x7F是十bai六进制的数值，（如：在微机原理du中一般表示为7FH，zhiH就表示十六进制）dao
//    0x7F = 7*16+15 = 127，

        //1 & ~0x7FL ==0
        //9 & ~0x7FL ==0
        //123456789 & ~0x7FL == 12345678
        int i = 1;
        while ((value & ~0x7FL) != 0) {  //判断是否2位数
//        0x是C语言中16进制数的表示方法。
//        0x80等于十进制的128
            os.write((((int) value & 0x7F) | 0x80)); //temp=temp|0x80;//表示将temp第一个字节的最高位置为1.
            value >>>= 7;  //>>>= 右移赋值，左边空出的位以0填充

//            System.out.println("debug:" + value);

            i++;
        }
        os.write((byte) value);
        return i;
    }

    /**
     * Pack non-negative long into byte array. It will occupy 1-10 bytes
     * depending on value (lower values occupy smaller space)
     *
     * @param ba    the byte array
     * @param value the long value
     * @return the number of bytes written
     * @throws IOException if an error occurs with the stream
     */
    static public int packLong(byte[] ba, long value)
            throws IOException {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }

        /**
         *
         * &是‘与’运算
         * 0x7f换成二进制就是0111 1111
         * p0和0111 1111进行‘与’运算。
         * 0111 1111第七位为0，而0和任何状态‘与’运算结果都为0，自然p0的第七位‘与’运算后就为0了。
         * 0111 1111第0---6位为1,1与1结果为1,1与0结果为0，那么P0的0---6位保持不变。
         *
         */
        int i = 1;
        while ((value & ~0x7FL) != 0) { //0x7f表示的是bai一个十六进du制数7f，换算成十进制数是127。 相当于将value bit7(最高位)清零，而不影响其他低7位。
            ba[i - 1] = (byte) (((int) value & 0x7F) | 0x80);
            value >>>= 7;
            i++;
        }
        ba[i - 1] = (byte) value;
        return i;
    }

    /**
     * Unpack positive long value from the input stream.
     *
     * @param is The input stream.
     * @return the long value
     * @throws IOException if an error occurs with the stream
     */
    static public long unpackLong(DataInput is)
            throws IOException {

        long result = 0;
        for (int offset = 0; offset < 64; offset += 7) {
            long b = is.readUnsignedByte();
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed long.");
    }

    /**
     * Unpack positive long value from the byte array.
     *
     * @param ba byte array
     * @return the long value
     */
    static public long unpackLong(byte[] ba) {
        return unpackLong(ba, 0);
    }

    /**
     * Unpack positive long value from the byte array.
     * 从字节数组中解压Long值
     * <p>
     * The index value indicates the index in the given byte array.
     * 索引值指示关于字节数组的索引
     *
     * @param ba    byte array
     * @param index index in ba
     * @return the long value
     */
    static public long unpackLong(byte[] ba, int index) {
        long result = 0;
        for (int offset = 0; offset < 64; offset += 7) {
            long b = ba[index++];
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed long.");
    }

    /**
     * Pack non-negative int into output stream. It will occupy 1-5 bytes
     * depending on value (lower values occupy smaller space)
     *
     * @param os    the data output
     * @param value the value
     * @return the number of bytes written
     * @throws IOException if an error occurs with the stream
     */
    static public int packInt(DataOutput os, int value)
            throws IOException {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }

        int i = 1;
        while ((value & ~0x7F) != 0) {
            os.write(((value & 0x7F) | 0x80));
            value >>>= 7;
            i++;
        }

        os.write((byte) value);
        return i;
    }

    /**
     * Unpack positive int value from the input stream.
     *
     * @param is The input stream.
     * @return the long value
     * @throws IOException if an error occurs with the stream
     */
    static public int unpackInt(DataInput is)
            throws IOException {
        for (int offset = 0, result = 0; offset < 32; offset += 7) {
            int b = is.readUnsignedByte();
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed integer.");
    }

    /**
     * Unpack positive int value from the input byte buffer.
     *
     * @param bb The byte buffer
     * @return the long value
     * @throws IOException if an error occurs with the stream
     */
    static public int unpackInt(ByteBuffer bb)
            throws IOException {
        for (int offset = 0, result = 0; offset < 32; offset += 7) {
            int b = bb.get() & 0xffff;
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed integer.");
    }
}
