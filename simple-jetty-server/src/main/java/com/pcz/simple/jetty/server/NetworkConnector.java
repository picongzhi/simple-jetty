package com.pcz.simple.jetty.server;

import java.io.Closeable;
import java.io.IOException;

/**
 * 网络连接器
 *
 * @author picongzhi
 */
public interface NetworkConnector extends Connector, Closeable {
    /**
     * 打开连接
     *
     * @throws IOException IO 异常
     */
    void open() throws IOException;

    /**
     * 关闭连接
     *
     * @throws
     */
    @Override
    void close();

    /**
     * 判断连接是否已经打开
     *
     * @return 链接是否已经打开
     */
    boolean isOpen();

    /**
     * 获取主机名
     *
     * @return 主机名
     */
    String getHost();

    /**
     * 获取端口
     *
     * @return 端口
     */
    int getPort();

    /**
     * 获取本地端口
     *
     * @return 本地端口
     */
    int getLocalPort();
}
