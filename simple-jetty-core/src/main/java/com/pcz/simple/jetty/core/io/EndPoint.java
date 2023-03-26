package com.pcz.simple.jetty.core.io;

import java.io.Closeable;

/**
 * 端点
 *
 * @author picongzhi
 */
public interface EndPoint extends Closeable {
    /**
     * 设置空闲超时时间
     *
     * @param timeout 空闲超时时间
     */
    void setIdleTimeout(long timeout);
}
