package com.pcz.simple.jetty.core.io;

import java.nio.ByteBuffer;

/**
 * 抽象的字节缓存池
 *
 * @author picongzhi
 */
abstract class AbstractByteBufferPool implements ByteBufferPool {
    @Override
    public ByteBuffer acquire(int size, boolean direct) {
        return null;
    }

    @Override
    public void release(ByteBuffer buffer) {

    }

    @Override
    public RetainableByteBufferPool asRetainableByteBufferPool() {
        return null;
    }
}
