PalDB
==========

[![Build Status](https://travis-ci.org/linkedin/PalDB.svg?branch=master)](https://travis-ci.org/linkedin/PalDB)
[![Coverage Status](https://coveralls.io/repos/linkedin/PalDB/badge.svg?branch=master&service=github)](https://coveralls.io/github/linkedin/PalDB?branch=master)

PalDB is an embeddable write-once key-value store written in Java.

What is PalDB?
-------------------
PalDB是一个快速的只读嵌入式key-value DB。
PalDB 是一次写多读的二进制DB。
PalDB 本身100k 依赖snappy。

PalDB is an embeddable persistent key-value store with very fast read performance and compact store size. PalDB stores are single binary files written once and ready to be used in applications.
PalDB是一个可嵌入的持久键值存储，具有非常快的读取性能和紧凑的存储大小。 PalDB存储是一次编写的单个二进制文件，可以在应用程序中使用。

PalDB's JAR is only 110K and has a single dependency (snappy, which isn't mandatory). It's also very easy to use with just a few configuration parameters.
PalDB的JAR只有110K，并且只有一个依赖（snappy，这不是强制性的）。 只需几个配置参数，它也非常容易使用。


Performance
-----------

Because PalDB is read-only and only focuses on data which can be held in memory it is significantly less complex than other embeddable key-value stores and therefore allows a compact storage format and very high throughput. PalDB is specifically optimized for fast read performance and compact store sizes. Performances can be compared to in-memory data structures such as Java collections (e.g. HashMap, HashSet) or other key-values stores (e.g. LevelDB, RocksDB).
由于PalDB是只读的，并且只关注可以保存在内存中的数据，因此它比其他可嵌入的键值存储要简单得多，因此可以实现紧凑的存储格式和非常高的吞吐量。
 PalDB专门针对快速读取性能和紧凑的存储大小进行了优化。 可以将性能与内存数据结构（例如Java集合（例如HashMap，HashSet）或其他键值存储（例如LevelDB，RocksDB））进行比较。

Current benchmark on a 3.1Ghz Macbook Pro with 10M integer keys index shows an average performance of ~1.6M reads/s for a memory usage 6X less than using a traditional HashSet. That is 5X faster throughput compared to LevelDB (1.8) or RocksDB (4.0).
具有10M整数键索引的3.1Ghz Macbook Pro的当前基准测试显示，与使用传统HashSet相比，内存使用量的平均性能为~1.6M读数/秒。 与LevelDB（1.8）或RocksDB（4.0）相比，吞吐量提高了5倍。

Results of a throughput benchmark between PalDB, LevelDB and RocksDB (higher is better):
PalDB，LevelDB和RocksDB之间的吞吐量基准测试结果（越高越好）：


![throughput](http://linkedin.github.io/PalDB/doc/throughput.png)

Memory usage benchmark between PalDB and a Java HashSet (lower is better):

![memory](http://linkedin.github.com/PalDB/doc/memory.png)

What is it suitable for?
------------------------

Side data can be defined as the extra read-only data needed by a process to do its job. For instance, a list of stopwords used by a natural language processing algorithm is side data. Machine learning models used in machine translation, content classification or spam detection are also side data. When this side data becomes large it can rapidly be a bottleneck for applications depending on them. PalDB aims to fill this gap.
边数据可以定义为进程完成其工作所需的额外只读数据。 例如，自然语言处理算法使用的停用词列表是辅助数据。 
机器翻译，内容分类或垃圾邮件检测中使用的机器学习模型也是辅助数据。 当这种辅助数据变大时，它可能迅速成为应用程序的瓶颈，取决于它们。 PalDB旨在填补这一空白。

PalDB can replace the usage of in-memory data structures to store this side data with comparable query performances and by using an order of magnitude less memory. It also greatly simplifies the code needed to operate this side data as PalDB stores are single binary files, manipulated with a very simple API (see below for examples).
PalDB可以取代内存数据结构的使用，以便通过可比较的查询性能和使用一个数量级更少的内存来存储此侧数据。 
它还大大简化了操作此侧数据所需的代码，因为PalDB存储是单个二进制文件，使用非常简单的API进行操作（请参阅下面的示例）。

Code samples
------------

API documentation can be found [here](http://linkedin.github.com/PalDB/doc/javadoc/index.html).

How to write a store
```java
StoreWriter writer = PalDB.createWriter(new File("store.paldb"));
writer.put("foo", "bar");
writer.put(1213, new int[] {1, 2, 3});
writer.close();
```

How to read a store
```java
StoreReader reader = PalDB.createReader(new File("store.paldb"));
String val1 = reader.get("foo");
int[] val2 = reader.get(1213);
reader.close();
```

How to iterate on a store
```java
StoreReader reader = PalDB.createReader(new File("store.paldb"));
Iterable<Map.Entry<String, String>> iterable = reader.iterable();
for (Map.Entry<String, String> entry : iterable) {
  String key = entry.getKey();
  String value = entry.getValue();
}
reader.close();
```

For Scala examples, see [here](https://gist.github.com/mbastian/9b9b49a4b96333da33ec) and [here](https://gist.github.com/mbastian/440a706f5e863bb65622).

Use it
------

PalDB is available on Maven Central, hence just add the following dependency:
```
<dependency>
    <groupId>com.linkedin.paldb</groupId>
    <artifactId>paldb</artifactId>
    <version>1.2.0</version>
</dependency>
```
Scala SBT
```
libraryDependencies += "com.linkedin.paldb" % "paldb" % "1.2.0"
```


Frequently asked questions
--------------------------

**Can you open a store for writing subsequent times?**

No, the final binary file is created when `StoreWriter.close()` is called.
.closed（）开始进行二进制文件的创建。

**Are duplicate keys allowed?**

No, duplicate keys aren't allowed and an exception will be thrown.
重复的key 会抛出异常；

**Do keys have an order when iterating?**

No, like a hashtable PalDB stores have no order.
key 是无序的；

Build
-----

PalDB requires Java 6+ and gradle. The target Java version is 6.

```bash
gradle build
```

Performance tests are run separately from the build
```bash
gradle perfTest
```

Test
----

We use the TestNG framework for our unit tests. You can run them via the `gradle clean test` command.

Coverage
--------

Coverage is run using JaCoCo. You can run a report via `gradle jacocoTestReport`. The report will be generated in `paldb/build/reports/jacoco/test/html/`.

Advanced configuration
----------------------

Write parameters:

+ `load.factor`,  index load factor (double) [default: 0.75]
+ `compression.enabled`, 允许压缩，默认是关闭的；enable compression (boolean) [default: false]

Read parameters:

+ `mmap.data.enabled`, enable memory mapping for data (boolean) [default: true]
+ `mmap.segment.size`, memory map segment size (bytes) [default: 1GB]
+ `cache.enabled`, LRU 缓存默认关闭；LRU cache enabled (boolean) [default: false]
+ `cache.bytes`, 缓存大小限制；cache limit (bytes) [default: Xmx - 100MB]
+ `cache.initial.capacity`, 缓存初始化容量；cache initial capacity (int) [default: 1000]
+ `cache.load.factor`, cache load factor (double) [default: 0.75]

Configuration values are passed at init time. Example:
初始化配置；
```java
Configuration config = PalDB.newConfiguration();
config.set(Configuration.CACHE_ENABLED, "true");
StoreReader reader = PalDB.createReader(new File("store.paldb"), config);
```

A few tips on how configuration can affect performance:
有关配置如何影响性能的一些提示：

+ Disabling memory mapping will significantly reduce performance as disk seeks will be performed instead.
+ 禁用内存映射会显着降低性能，因为将执行磁盘搜索。

+ Enabling the cache makes sense when the value size is large and there's a significant cost in deserialization. Otherwise, the cache adds an overhead. The cache is also useful when memory mapping is disabled.
+ 当值大小很大并且反序列化的成本很高时，启用缓存是有意义的。 否则，缓存会增加开销。 禁用内存映射时，缓存也很有用。

+ Compression can be enabled when the store size is a concern and the values are large (e.g. a sparse matrix). By default, PalDB already uses a compact serialization. Snappy is used for compression.
+ 当存储大小受到关注且值很大时（例如稀疏矩阵），可以启用压缩。 默认情况下，PalDB已经使用了紧凑的序列化。 Snappy用于压缩。



Custom serializer
-----------------

PalDB is primarily optimized for Java primitives and arrays but supports adding custom serializers so arbitrary Java classes can be supported.
PalDB主要针对Java原语和数组进行了优化，但支持添加自定义序列化程序，因此可以支持任意Java类。

Serializers can be defined by implementing the `Serializer` interface and its methods. Here's an example which supports the `java.awt.Point` class:

```java
public class PointSerializer implements Serializer<Point> {

  @Override
  public Point read(DataInput input) {
    return new Point(input.readInt(), input.readInt());
  }

  @Override
  public void write(DataOutput output, Point point) {
    output.writeInt(point.x);
    output.writeInt(point.y);
  }

  @Override
  public int getWeight(Point instance) {
    return 8;
  }
}
```

The `write` method serializes the instance to the `DataOutput`. The `read` method deserializes from `DataInput` and creates new object instances. The `getWeight` method returns the estimated memory used by an instance in bytes. The latter is used by the cache to evaluate the amount of memory it's currently using.

Serializer implementation should be registered using the `Configuration`:

```java
Configuration configuration = PalDB.newConfiguration();
configuration.registerSerializer(new PointSerializer());
```

Use cases
---------

At LinkedIn, PalDB is used in analytics workflows and machine-learning applications.
在LinkedIn，PalDB用于分析工作流程和机器学习应用程序。

Its usage is especially popular in Hadoop workflows because memory is rare yet critical to speed things up. In this context, PalDB often enables map-side operations (e.g. join) which wouldn't be possible with classic in-memory data structures (e.g Java collections). For instance, a set of 35M member ids would only use ~290M of memory with PalDB versus ~1.8GB with a traditional Java HashSet. Moreover, as PalDB's store files are single binary files it is easy to package and use with Hadoop's distributed cache mechanism.
它的使用在Hadoop工作流程中特别受欢迎，因为内存很少，但对于加快速度至关重要。
在这种情况下，PalDB经常启用映射端操作（例如，连接），这对于经典的内存数据结构（例如Java集合）是不可能的。
例如，一组35M成员ID只能使用PalDB内存大约290M，而使用传统Java HashSet大约1.8GB内存。
此外，由于PalDB的存储文件是单个二进制文件，因此很容易使用Hadoop的分布式缓存机制进行打包和使用。

Machine-learning applications often have complex binary model files created in the `training` phase and used in the `scoring` phase. These two phases always happen at different times and often in different environments. For instance, the training phase happens on Hadoop or Spark and the scoring phase in a real-time service. PalDB makes this process easier and more efficient by reducing the need of large CSV files loaded in memory.
机器学习应用程序通常具有在“训练”阶段创建的复杂二进制模型文件，并用于“评分”阶段。
这两个阶段总是发生在不同的时间，通常发生在不同的环境中。
例如，培训阶段发生在Hadoop或Spark以及实时服务中的评分阶段。 PalDB通过减少内存中加载的大型CSV文件的需要，使这个过程更容易，更有效。

Limitations
-----------
+ PalDB is optimal in replacing the usage of large in-memory data storage but still use memory (off-heap, yet much less) to do its job. Disabling memory mapping and relying on seeks is possible but is not what PalDB has been optimized for.
+ PalDB是替换大型内存数据存储的最佳选择，但仍然使用内存（堆外，但更少）来完成其工作。 禁用内存映射并依赖于搜索是可能的，但不是PalDB针对其进行优化的。
+ The size of the index is limited to 2GB. There's no limitation in the data size however.
+ 索引的大小限制为2GB。 但是，数据大小没有限制。
+ PalDB is not thread-safe at the moment so synchronization should be done externally if multi-threaded.
+ PalDB目前不是线程安全的，因此如果是多线程的，则应在外部进行同步。

Contributions
-----------

Any helpful feedback is more than welcome. This includes feature requests, bug reports, pull requests, constructive feedback, etc.

Copyright & License
-------------------

PalDB © 2015 LinkedIn Corp. Licensed under the terms of the Apache License, Version 2.0.
