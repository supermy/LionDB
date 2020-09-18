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

package com.linkedin.paldb.impl;

import com.linkedin.paldb.api.Configuration;
import com.linkedin.paldb.utils.FormatVersion;
import com.linkedin.paldb.utils.HashUtils;
import com.linkedin.paldb.utils.LongPacker;
import com.linkedin.paldb.utils.TempUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal write implementation.
 */
public class StorageWriter {

  private final static Logger LOGGER = Logger.getLogger(StorageWriter.class.getName());
  // Configuration
  private final Configuration config;
  private final double loadFactor;
  // Output
  private final File tempFolder;
  //
  private final OutputStream outputStream;
  // Index stream 索引文件;维护key对应的index文件
  private File[] indexFiles;
  // 维护key对应文件流
  private DataOutputStream[] indexStreams;
  // Data stream  数据文件;维护value对应的data文件
  private File[] dataFiles;
  //维护value对应的data文件流
  private DataOutputStream[] dataStreams;

  // Cache last value 缓存最后的数据
  private byte[][] lastValues;
  private int[] lastValuesLength;

  // Data length 数据长度=key 长度+1
  private long[] dataLengths;
  // Index length 索引长度
  private long indexesLength;

  // Max offset length ；维护index指向data的位移的存储字段类型
  private int[] maxOffsetLengths;

  // Number of keys key 的数量=key 的最大长度+1
  private int keyCount;
  private int[] keyCounts;

  // Number of values ；value 的数量
  private int valueCount;

  // Number of collisions
  private int collisions;

  private HashUtils hashUtils;

  StorageWriter(Configuration configuration, OutputStream stream) {
    config = configuration;
    loadFactor = config.getDouble(Configuration.LOAD_FACTOR);
    if (loadFactor <= 0.0 || loadFactor >= 1.0) {
      throw new IllegalArgumentException("Illegal load factor = " + loadFactor + ", should be between 0.0 and 1.0.");
    }

    // Create temp path folder
    tempFolder = TempUtils.createTempDir("paldbtempwriter");
    tempFolder.deleteOnExit();
    LOGGER.log(Level.INFO, "Creating temporary folder at {0}", tempFolder.toString());
    outputStream = stream instanceof BufferedOutputStream ? stream : new BufferedOutputStream(stream);
    indexStreams = new DataOutputStream[0];
    dataStreams = new DataOutputStream[0];
    indexFiles = new File[0];
    dataFiles = new File[0];
    lastValues = new byte[0][];
    lastValuesLength = new int[0];
    dataLengths = new long[0];
    maxOffsetLengths = new int[0];
    keyCounts = new int[0];
    hashUtils = new HashUtils();
  }

  /**
   * 数据写入过程解析
   * @param key
   * @param value
   * @throws IOException
   */
  public void put(byte[] key, byte[] value)
      throws IOException {
    int keyLength = key.length;

    //Get the Output stream for that keyLength, each key length has its own file
    //通过key 的长度获取输出流，每个key 长度维护一个索引文件与流；自动扩容
    DataOutputStream indexStream = getIndexStream(keyLength);

    //1.写入key Write key 字节数组
    indexStream.write(key);

    // Check if the value is identical to the last inserted;
    // 判断本次数据是否和上次写入数据一致
    byte[] lastValue = lastValues[keyLength];
    boolean sameValue = lastValue != null && Arrays.equals(value, lastValue);

    // Get data stream and length
    // 如果值相同，则使用上一个数据的偏移
    long dataLength = dataLengths[keyLength];
    if (sameValue) {
      //如果本次写入数据和上次一致，就把dataLength指向上一个value的起始位置
      dataLength -= lastValuesLength[keyLength];
    }

    // Write offset and record max offset length；
      //2.写入key对应value的位移dataLength
    int offsetLength = LongPacker.packLong(indexStream, dataLength);
    maxOffsetLengths[keyLength] = Math.max(offsetLength, maxOffsetLengths[keyLength]);

    // Write if data is not the same；如果数据不相同；
    if (!sameValue) {
      // Get stream；获取指定长度key对应的value的输出流
      DataOutputStream dataStream = getDataStream(keyLength);

      // Write size and value；
        // 3.写入value的长度和value的值
      int valueSize = LongPacker.packInt(dataStream, value.length);
      dataStream.write(value);

      // Update data length；更新数据偏移量，这里的偏移量是下一个写入value的偏移量(值长度长度+值长度)
      dataLengths[keyLength] += valueSize + value.length;

      // Update last value；更新上一次写入的value值
      lastValues[keyLength] = value;
      //更新keyLength对应的key的保存的data的长度
      lastValuesLength[keyLength] = valueSize + value.length;

      valueCount++; //数据+1
    }

    keyCount++; //总数key+1
    keyCounts[keyLength]++; //对应长度个数key+1
  }

  /**
   * 通过close动作来完成PalDB文件的生成
   * @throws IOException
   */
  public void close()
      throws IOException {
    // Close the data and index streams
    // 关闭及数据与索引文件流
    for (DataOutputStream dos : dataStreams) {
      if (dos != null) {
        dos.close();
      }
    }
    for (DataOutputStream dos : indexStreams) {
      if (dos != null) {
        dos.close();
      }
    }

    // Stats
    LOGGER.log(Level.INFO, "Number of keys: {0}", keyCount);
    LOGGER.log(Level.INFO, "Number of values: {0}", valueCount);

    // Prepare files to merge
    // 准备合并文件
    List<File> filesToMerge = new ArrayList<File>();

    try {

      //Write metadata file
      //写元数据文件
      File metadataFile = new File(tempFolder, "metadata.dat");
      metadataFile.deleteOnExit();
      FileOutputStream metadataOututStream = new FileOutputStream(metadataFile);
      DataOutputStream metadataDataOutputStream = new DataOutputStream(metadataOututStream);
      writeMetadata(metadataDataOutputStream);
      metadataDataOutputStream.close();
      metadataOututStream.close();
      filesToMerge.add(metadataFile);

      // Build index file
      for (int i = 0; i < indexFiles.length; i++) {
        if (indexFiles[i] != null) {
          filesToMerge.add(buildIndex(i));
        }
      }

      // Stats collisions
      LOGGER.log(Level.INFO, "Number of collisions: {0}", collisions);

      // Add data files
      for (File dataFile : dataFiles) {
        if (dataFile != null) {
          filesToMerge.add(dataFile);
        }
      }

      // Merge and write to output
      checkFreeDiskSpace(filesToMerge);
      mergeFiles(filesToMerge, outputStream);
    } finally {
      outputStream.close();
      cleanup(filesToMerge);
    }
  }

  private void writeMetadata(DataOutputStream dataOutputStream)
      throws IOException {
    //Write format version ；版本号
    dataOutputStream.writeUTF(FormatVersion.getLatestVersion().name());

    //Write time ；写入时间戳
    dataOutputStream.writeLong(System.currentTimeMillis());

    //Prepare
    int keyLengthCount = getNumKeyCount();
    int maxKeyLength = keyCounts.length - 1;

    //Write size (number of keys) ；key总数量
    dataOutputStream.writeInt(keyCount);

    //Write the number of different key length；keyLength数量,去掉为 0 的
    dataOutputStream.writeInt(keyLengthCount);

    //Write the max value for keyLength;key最大长度
    dataOutputStream.writeInt(maxKeyLength);

    // For each keyLength
    long datasLength = 0l;
    for (int i = 0; i < keyCounts.length; i++) {
      if (keyCounts[i] > 0) {
        // Write the key length;key 长度
        dataOutputStream.writeInt(i);

        // Write key count ；key数量
        dataOutputStream.writeInt(keyCounts[i]);

        // 构建开放式寻址
        // Write slot count ;桶数量 哈希表 slot 数量 = 该 key 长度下的 key 数量 / loadFactor（默认0.75，可手动指定）
        int slots = (int) Math.round(keyCounts[i] / loadFactor);
        dataOutputStream.writeInt(slots);

        // Write slot size ;slot_size=i+最大偏移量长度
        int offsetLength = maxOffsetLengths[i];
        dataOutputStream.writeInt(i + offsetLength);       //每个 slot 的大小是固定的，等于 key 长度 +  value 位置的最大长度（因此，slot 里的数据其实是有部分空闲的）。

          // Write index offset ；该长度的键在数据文件中的索引偏移
        dataOutputStream.writeInt((int) indexesLength);

        // Increment index length ；该长度的键所有桶的长度+索引累计长度
        indexesLength += (i + offsetLength) * slots;

        // Write data length ；该长度的数据在数据文件中的数据偏移
        dataOutputStream.writeLong(datasLength);

        // Increment data length ；数据累计长度(值长度长度+值长度) 下一个长度 value的开始位置；
        datasLength += dataLengths[i];
      }
    }

    //Write serializers
    try {
      Serializers.serialize(dataOutputStream, config.getSerializers());
    } catch (Exception e) {
      throw new RuntimeException();
    }

    //Write the position of the index and the data
    // 元数据文件+key文件+value文件的顺序，先写入key索引文件起始位移，再写入value的data文件的起始位移。
    int indexOffset = dataOutputStream.size() + (Integer.SIZE / Byte.SIZE) + (Long.SIZE / Byte.SIZE);
    //索引整体偏移量
    dataOutputStream.writeInt(indexOffset);
    //value 整体偏移量=索引偏移量+索引长度
    dataOutputStream.writeLong(indexOffset + indexesLength);
  }

  private File buildIndex(int keyLength)  //会有多个索引文件
      throws IOException {

      // 根据该长度下key的数目/负载因子计算存储的slot的格式
      long count = keyCounts[keyLength];
      int slots = (int) Math.round(count / loadFactor);
    int offsetLength = maxOffsetLengths[keyLength];

      //注意slotSize的计算方式，slot里面保存的内容包括key的长度以及指向data的偏移量占用的字节数
      int slotSize = keyLength + offsetLength;

    // Init index
    File indexFile = new File(tempFolder, "index" + keyLength + ".dat");
    RandomAccessFile indexAccessFile = new RandomAccessFile(indexFile, "rw");
    try {

      indexAccessFile.setLength(slots * slotSize);// 设置重建key的文件的长度 mmap 根据桶的长度；
      FileChannel indexChannel = indexAccessFile.getChannel();
      MappedByteBuffer byteBuffer = indexChannel.map(FileChannel.MapMode.READ_WRITE, 0, indexAccessFile.length());  //映射内存的起始位置与空间大小

      // Init reading stream
        // 初始化输入流
      File tempIndexFile = indexFiles[keyLength];
      DataInputStream tempIndexStream = new DataInputStream(new BufferedInputStream(new FileInputStream(tempIndexFile)));
      try {

        byte[] keyBuffer = new byte[keyLength];  //one key byte数组
        byte[] slotBuffer = new byte[slotSize];  //one solt byte数组
        byte[] offsetBuffer = new byte[offsetLength]; //one offset byte数组

        // Read all keys
          // 遍历key的数量重新写入到新建的索引文件当中
          for (int i = 0; i < count; i++) {
          // Read key
          tempIndexStream.readFully(keyBuffer);   //读取 key by keyBuffer 长度；

          // Read offset
          long offset = LongPacker.unpackLong(tempIndexStream);  //读取 data偏移地址

          // Hash
          long hash = (long) hashUtils.hash(keyBuffer);  // Hash，根据key进行重hash后确定放置到具体的slot位置

          //开放寻址法，随机数探测
          boolean collision = false;
          for (int probe = 0; probe < count; probe++) {
            int slot = (int) ((hash + probe) % slots);

            byteBuffer.position(slot * slotSize);  //内存块指定地址；
            byteBuffer.get(slotBuffer); //获取桶长度数据；byteBuffer获取完 key 数据之后，就会移动指针到 slotBuffer 的位置；

            long found = LongPacker.unpackLong(slotBuffer, keyLength); //是否存在 key ，偏移地址是否为 0
            if (found == 0) {
              // The spot is empty use it； 桶为空则使用
              byteBuffer.position(slot * slotSize); // 根据hash值写入key以及key对应value在data文件的偏移量
              byteBuffer.put(keyBuffer); //保存 key
              int pos = LongPacker.packLong(offsetBuffer, offset);
              byteBuffer.put(offsetBuffer, 0, pos); //保存data偏移量 ,pos = offsetBuffer长度
              break;
            } else {
              collision = true;
              // Check for duplicates
                // PalDB不支持存在相同的key
                if (Arrays.equals(keyBuffer, Arrays.copyOf(slotBuffer, keyLength))) {
                throw new RuntimeException(
                        String.format("A duplicate key has been found for for key bytes %s", Arrays.toString(keyBuffer)));
              }
            }
          }

          if (collision) {
            collisions++;
          }
        }

        String msg = "  Max offset length: " + offsetLength + " bytes" +
                "\n  Slot size: " + slotSize + " bytes";

        LOGGER.log(Level.INFO, "Built index file {0}\n" + msg, indexFile.getName());
      } finally {
        // Close input
        tempIndexStream.close();

        // Close index and make sure resources are liberated
        indexChannel.close();
        indexChannel = null;
        byteBuffer = null;

        // Delete temp index file
        if (tempIndexFile.delete()) {
          LOGGER.log(Level.INFO, "Temporary index file {0} has been deleted", tempIndexFile.getName());
        }
      }
    } finally{
      indexAccessFile.close();
      indexAccessFile = null;
      System.gc();
    }

    return indexFile;
  }

  //Fail if the size of the expected store file exceed 2/3rd of the free disk space
  private void checkFreeDiskSpace(List<File> inputFiles) {
    //Check for free space
    long usableSpace = 0;
    long totalSize = 0;
    for (File f : inputFiles) {
      if (f.exists()) {
        totalSize += f.length();
        usableSpace = f.getUsableSpace();
      }
    }
    LOGGER.log(Level.INFO, "Total expected store size is {0} Mb",
        new DecimalFormat("#,##0.0").format(totalSize / (1024 * 1024)));
    LOGGER.log(Level.INFO, "Usable free space on the system is {0} Mb",
        new DecimalFormat("#,##0.0").format(usableSpace / (1024 * 1024)));
    if (totalSize / (double) usableSpace >= 0.66) {
      throw new RuntimeException("Aborting because there isn' enough free disk space");
    }
  }

  //Merge files to the provided fileChannel
  private void mergeFiles(List<File> inputFiles, OutputStream outputStream)
      throws IOException {
    long startTime = System.nanoTime();

    //Merge files
    for (File f : inputFiles) {
      if (f.exists()) {
        FileInputStream fileInputStream = new FileInputStream(f);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        try {
          LOGGER.log(Level.INFO, "Merging {0} size={1}", new Object[]{f.getName(), f.length()});

          byte[] buffer = new byte[8192];
          int length;
          while ((length = bufferedInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
          }
        } finally {
          bufferedInputStream.close();
          fileInputStream.close();
        }
      } else {
        LOGGER.log(Level.INFO, "Skip merging file {0} because it doesn't exist", f.getName());
      }
    }

    LOGGER.log(Level.INFO, "Time to merge {0} s", ((System.nanoTime() - startTime) / 1000000000.0));
  }

  //Cleanup files
  private void cleanup(List<File> inputFiles) {
    for (File f : inputFiles) {
      if (f.exists()) {
        if (f.delete()) {
          LOGGER.log(Level.INFO, "Deleted temporary file {0}", f.getName());
        }
      }
    }
    if (tempFolder.delete()) {
      LOGGER.log(Level.INFO, "Deleted temporary folder at {0}", tempFolder.getAbsolutePath());
    }
  }

  //Get the data stream for the specified keyLength, create it if needed
  private DataOutputStream getDataStream(int keyLength)
      throws IOException {
    // Resize array if necessary
    if (dataStreams.length <= keyLength) {
      dataStreams = Arrays.copyOf(dataStreams, keyLength + 1);
      dataFiles = Arrays.copyOf(dataFiles, keyLength + 1);
    }

    DataOutputStream dos = dataStreams[keyLength];
    if (dos == null) {
      File file = new File(tempFolder, "data" + keyLength + ".dat");
      file.deleteOnExit();
      dataFiles[keyLength] = file;

      dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      dataStreams[keyLength] = dos;

      // Write one byte so the zero offset is reserved
      // 写一个字节，以便保留零偏移
      dos.writeByte(0);
    }
    return dos;
  }

  //Get the index stream for the specified keyLength, create it if needed
  private DataOutputStream getIndexStream(int keyLength)
      throws IOException {
    // Resize array if necessary;每次按照keyLength进行扩容
    if (indexStreams.length <= keyLength) {
      indexStreams = Arrays.copyOf(indexStreams, keyLength + 1);
      indexFiles = Arrays.copyOf(indexFiles, keyLength + 1);

      keyCounts = Arrays.copyOf(keyCounts, keyLength + 1);
      maxOffsetLengths = Arrays.copyOf(maxOffsetLengths, keyLength + 1);

      lastValues = Arrays.copyOf(lastValues, keyLength + 1);
      lastValuesLength = Arrays.copyOf(lastValuesLength, keyLength + 1);

      dataLengths = Arrays.copyOf(dataLengths, keyLength + 1);
    }

    // Get or create stream
    DataOutputStream dos = indexStreams[keyLength];
    if (dos == null) {
      File file = new File(tempFolder, "temp_index" + keyLength + ".dat");
      file.deleteOnExit();
      indexFiles[keyLength] = file;

      dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      indexStreams[keyLength] = dos;

      //写一个字节，以便保留零偏移 dos.writeByte(0);
      dataLengths[keyLength]++;
    }
    return dos;
  }

  private int getNumKeyCount() {
    int res = 0;
    for (int i = 0; i < keyCounts.length; i++) {
      if (keyCounts[i] != 0) {
        res++;
      }
    }
    return res;
  }


}
