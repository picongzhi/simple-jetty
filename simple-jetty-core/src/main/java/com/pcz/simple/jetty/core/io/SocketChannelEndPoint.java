package com.pcz.simple.jetty.core.io;

import com.pcz.simple.jetty.core.thread.Scheduler;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 基于 {@link java.nio.channels.SocketChannel} 的 {@link EndPoint}
 *
 * @author picongzhi
 */
public class SocketChannelEndPoint extends SelectableChannelEndPoint {
    public SocketChannelEndPoint(SocketChannel socketChannel,
                                 ManagedSelector managedSelector,
                                 SelectionKey selectionKey,
                                 Scheduler scheduler) {

    }
}
