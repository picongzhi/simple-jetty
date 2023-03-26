package com.pcz.simple.jetty.core.io;

import com.pcz.simple.jetty.core.component.ContainerLifeCycle;

import java.io.Closeable;
import java.nio.channels.SelectableChannel;
import java.util.EventListener;

/**
 * NIO Selector 管理器
 *
 * @author picongzhi
 */
public abstract class SelectorManager extends ContainerLifeCycle {
    /**
     * 给连接操作注册服务端通道
     *
     * @param selectableChannel 服务端通道
     * @return {@link Closeable}
     */
    public Closeable acceptor(SelectableChannel selectableChannel) {
        return null;
    }

    /**
     * 注册通道
     *
     * @param selectableChannel 通道
     */
    public void accept(SelectableChannel selectableChannel) {

    }

    /**
     * {@link SelectorManager} 监听器
     */
    public interface SelectorManagerListener extends EventListener {
    }
}
