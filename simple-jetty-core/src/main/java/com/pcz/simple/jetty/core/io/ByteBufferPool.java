package com.pcz.simple.jetty.core.io;

import java.nio.ByteBuffer;

/**
 * {@link ByteBuffer} 池
 *
 * @author picongzhi
 */
public interface ByteBufferPool {
    /**
     * 获取固定大小的 {@link ByteBuffer}
     *
     * @param size   缓存大小
     * @param direct 是否需要直接内存
     * @return {@link ByteBuffer}
     */
    ByteBuffer acquire(int size, boolean direct);

    /**
     * 释放 {@link ByteBuffer}，释放之后可重复使用
     *
     * @param buffer {@link ByteBuffer}
     */
    void release(ByteBuffer buffer);

    /**
     * 移除 {@link ByteBuffer}，移除之后不可重复使用
     *
     * @param buffer {@link ByteBuffer}
     */
    default void remove(ByteBuffer buffer) {
    }

    /**
     * 转 {@link RetainableByteBufferPool}
     *
     * @return {@link RetainableByteBufferPool}
     */
    RetainableByteBufferPool asRetainableByteBufferPool();
}
