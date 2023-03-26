package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.component.Container;
import com.pcz.simple.jetty.core.component.Graceful;
import com.pcz.simple.jetty.core.component.LifeCycle;
import com.pcz.simple.jetty.core.io.ByteBufferPool;
import com.pcz.simple.jetty.core.io.EndPoint;
import com.pcz.simple.jetty.core.thread.Scheduler;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 连接器
 *
 * @author picongzhi
 */
public interface Connector extends LifeCycle, Container, Graceful {
    /**
     * 获取服务器
     *
     * @return 服务器
     */
    Server getServer();

    /**
     * 获取执行器
     *
     * @return 执行器
     */
    Executor getExecutor();

    /**
     * 获取调度器
     *
     * @return 调度器
     */
    Scheduler getScheduler();

    /**
     * 获取缓存池
     *
     * @return 缓存池
     */
    ByteBufferPool getByteBufferPool();

    /**
     * 根据协议获取连接工厂
     *
     * @param nextProtocol 下一个协议
     * @return 连接工厂
     */
    ConnectionFactory getConnectionFactory(String nextProtocol);

    /**
     * 根据工厂类型获取连接工厂
     *
     * @param factoryType 工厂类型
     * @param <T>         工厂类型
     * @return 连接工厂
     */
    <T> T getConnectionFactory(Class<T> factoryType);

    /**
     * 获取默认的连接工厂
     *
     * @return 默认的连接工厂
     */
    ConnectionFactory getDefaultConnectionFactory();

    /**
     * 获取所有连接工厂
     *
     * @return 所有连接工厂
     */
    Collection<ConnectionFactory> getConnectionFactories();

    /**
     * 获取所有协议
     *
     * @return 所有协议
     */
    List<String> getProtocols();

    /**
     * 获取连接的最大空闲时间
     *
     * @return 最大空闲时间
     */
    long getIdleTimeout();

    /**
     * 获取底层的通信对象，Socket、Channel等
     *
     * @return 底层的通信对象
     */
    Object getTransport();

    /**
     * 获取所有连接的端点
     *
     * @return 所有连接的端点
     */
    Collection<EndPoint> getConnectedEndPoints();

    /**
     * 获取连接器名称
     *
     * @return 名称
     */
    String getName();
}
