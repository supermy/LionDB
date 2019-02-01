Change Log
==========

### 阅读理解
序列化 kryo，快且尺寸小；TestLongPacker
Hash 采用murmur3,高性能、低碰撞率且分布均衡；
Hash冲突 开放寻址方法（拉链法、再散列法）
Hash取模 位计算，高性能:tableSizeFor,indexFor
Snappy压缩数据  out.write(SHORT_ARRAY_C); //写入数据类型 ；byte[] b = Snappy.compress(val); //数据压缩
RoaringBitmap 结合kryo序列化
BloomFilter guava 作为运算，可以持久化与恢复

Json精密性



Version 1.2.0 *(June 26th 2016)*
--------------------------

### Bugfixes

- Custom serializers with interfaces as types don't work (#25)
- Fix exception handling (#15)
- Fix concurrent database access with multiple instances (#26)

Version 1.1.0 *(January 8th 2016)*
--------------------------

### New features

- Implement a StoreReader.keys() method (#13)

### Bugfixes

- Fixes the return type of apis (#9)
- A Exception Using reader.iterable() (#16)

### Performance improvements

- Share slot read buffers (#14)

### Compatibility

The prefix bytes have been modified (#21) as they were confusing so exceptionally this version breaks store compatibility with 1.0.0. This shouldn't happen anymore in the future for minor versions.

Version 1.0.0 *(July 1st 2015)*
--------------------------
 *  First public release