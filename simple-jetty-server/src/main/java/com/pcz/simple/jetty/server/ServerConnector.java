package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.IO;
import com.pcz.simple.jetty.core.io.ByteBufferPool;
import com.pcz.simple.jetty.core.io.ManagedSelector;
import com.pcz.simple.jetty.core.io.SelectorManager;
import com.pcz.simple.jetty.core.io.SocketChannelEndPoint;
import com.pcz.simple.jetty.core.thread.Scheduler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EventListener;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务器连接器
 *
 * @author picongzhi
 */
public class ServerConnector extends AbstractNetworkConnector {
    /**
     * Selector 管理器
     */
    private final SelectorManager selectorManager;

    /**
     * 接收器引用
     */
    private final AtomicReference<Closeable> acceptor = new AtomicReference<>();

    /**
     * 接收通道
     */
    private volatile ServerSocketChannel acceptChannel;

    /**
     * 是否继承通道
     */
    private volatile boolean inheritChannel = false;

    /**
     * 本地端口
     */
    private volatile int localPort = -1;

    /**
     * 接收队列大小
     */
    private volatile int acceptQueueSize = 0;

    /**
     * 是否可重用地址
     */
    private volatile boolean reuseAddress = true;

    /**
     * 是否可重用端口
     */
    private volatile boolean reusePort = false;

    /**
     * 是否开启 Nagle 算法
     */
    private volatile boolean acceptedTcpNoDelay = true;

    /**
     * 接收缓存大小
     */
    private volatile int acceptedReceiveBufferSize = -1;

    /**
     * 发送缓存大小
     */
    private volatile int acceptedSendBufferSize = -1;

    public ServerConnector(Server server) {
        this(server, null, null, null, -1, -1, new HttpConnectionFactory());
    }


    public ServerConnector(Server server,
                           int acceptors,
                           int selectors) {
        this(server, null, null, null, acceptors, selectors, new HttpConnectionFactory());
    }

    public ServerConnector(Server server,
                           int acceptors,
                           int selectors,
                           ConnectionFactory... connectionFactories) {
        this(server, null, null, null, acceptors, selectors, connectionFactories);
    }

    public ServerConnector(Server server,
                           ConnectionFactory... connectionFactories) {
        this(server, null, null, null, -1, -1, connectionFactories);
    }

    public ServerConnector(Server server,
                           SslConnectionFactory.Server sslConnectionFactory) {
        this(server, null, null, null, -1, -1,
                AbstractConnectionFactory.getFactories(sslConnectionFactory, new HttpConnectionFactory()));
    }

    public ServerConnector(Server server,
                           int acceptors,
                           int selectors,
                           SslConnectionFactory.Server sslConnectionFactory) {
        this(server, null, null, null, acceptors, selectors,
                AbstractConnectionFactory.getFactories(sslConnectionFactory, new HttpConnectionFactory()));
    }

    public ServerConnector(Server server,
                           SslConnectionFactory.Server sslConnectionFactory,
                           ConnectionFactory... connectionFactories) {
        this(server, null, null, null, -1, -1,
                AbstractConnectionFactory.getFactories(sslConnectionFactory, connectionFactories));
    }

    public ServerConnector(Server server,
                           Executor executor,
                           Scheduler scheduler,
                           ByteBufferPool byteBufferPool,
                           int acceptors,
                           int selectors,
                           ConnectionFactory... connectionFactories) {
        super(server, executor, scheduler, byteBufferPool, acceptors, connectionFactories);

        this.selectorManager = newSelectorManager(getExecutor(), getScheduler(), selectors);
        addBean(this.selectorManager, true);

        setAcceptorPriorityDelta(-2);
    }

    /**
     * 实例化 {@link SelectorManager}
     *
     * @param executor  执行器
     * @param scheduler 调度器
     * @param selectors 选择器数量
     * @return {@link SelectorManager}
     */
    protected SelectorManager newSelectorManager(Executor executor, Scheduler scheduler, int selectors) {
        return new ServerConnectorManager(executor, scheduler, selectors);
    }

    @Override
    protected void doStart() throws Exception {
        // 注册监听器
        for (EventListener eventListener : getBeans(SelectorManager.SelectorManagerListener.class)) {
            this.selectorManager.addEventListener(eventListener);
        }

        super.doStart();

        if (getAcceptors() == 0) {
            this.acceptChannel.configureBlocking(false);
            this.acceptor.set(this.selectorManager.acceptor(this.acceptChannel));
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        // 移除监听器
        for (EventListener eventListener : getBeans(EventListener.class)) {
            this.selectorManager.removeEventListener(eventListener);
        }
    }

    @Override
    public boolean isOpen() {
        ServerSocketChannel serverSocketChannel = this.acceptChannel;
        return serverSocketChannel != null && serverSocketChannel.isOpen();
    }

    /**
     * 判断是否是从 JVM 继承的通道
     *
     * @return 是否是从 JVM 继承的通道
     */
    public boolean isInheritChannel() {
        return this.inheritChannel;
    }

    /**
     * 设置是否是从 JVM 继承的通道
     *
     * @param inheritChannel 是否是从 JVM 继承的通道
     */
    public void setInheritChannel(boolean inheritChannel) {
        this.inheritChannel = inheritChannel;
    }

    /**
     * 通过给定的服务端通道打开连接器
     *
     * @param acceptChannel 服务端通道
     * @throws IOException IO 异常
     */
    public void open(ServerSocketChannel acceptChannel) throws IOException {
        if (isStarted()) {
            throw new IllegalStateException(getState());
        }

        // 更新 bean
        updateBean(this.acceptChannel, acceptChannel);

        // 设置服务端通道
        this.acceptChannel = acceptChannel;

        // 设置本地端口
        this.localPort = this.acceptChannel.socket().getLocalPort();
        if (this.localPort <= 0) {
            throw new IOException("Server channel not bound");
        }
    }

    @Override
    public void open() throws IOException {
        if (this.acceptChannel == null) {
            this.acceptChannel = openAcceptChannel();
            this.acceptChannel.configureBlocking(true);

            this.localPort = this.acceptChannel.socket().getLocalPort();
            if (this.localPort <= 0) {
                throw new IOException("Server channel not bound");
            }

            addBean(this.acceptChannel);
        }
    }

    /**
     * 打开服务端通道
     *
     * @return 服务端通道
     * @throws IOException IO 异常
     */
    protected ServerSocketChannel openAcceptChannel() throws IOException {
        ServerSocketChannel serverSocketChannel = null;

        if (isInheritChannel()) {
            // 继承 JVM 的通道
            Channel channel = System.inheritedChannel();
            if (channel instanceof ServerSocketChannel) {
                serverSocketChannel = (ServerSocketChannel) channel;
            } else {
                LOG.warn("Unable to use System.inheritedChannel() [{}]. Trying a new ServerSocketChannel at {}:{}",
                        channel, getHost(), getPort());
            }
        }

        if (serverSocketChannel == null) {
            // 打开通道
            InetSocketAddress inetSocketAddress = getHost() == null
                    ? new InetSocketAddress(getPort())
                    : new InetSocketAddress(getHost(), getPort());

            serverSocketChannel = ServerSocketChannel.open();
            setSocketOption(serverSocketChannel, StandardSocketOptions.SO_REUSEADDR, isReuseAddress());
            setSocketOption(serverSocketChannel, StandardSocketOptions.SO_REUSEPORT, isReusePort());

            // 绑定地址
            try {
                serverSocketChannel.bind(inetSocketAddress, getAcceptQueueSize());
            } catch (Throwable t) {
                IO.close(serverSocketChannel);
                throw new IOException("Failed to bind to " + inetSocketAddress, t);
            }
        }

        return serverSocketChannel;
    }

    /**
     * 设置 Socket 选项
     *
     * @param serverSocketChannel 服务端通道
     * @param socketOption        Socket 选项
     * @param value               选项值
     * @param <T>                 值类型
     */
    private <T> void setSocketOption(ServerSocketChannel serverSocketChannel, SocketOption<T> socketOption, T value) {
        try {
            serverSocketChannel.setOption(socketOption, value);
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not configure {} to {} on {}",
                        socketOption, value, serverSocketChannel, t);
            }
        }
    }

    /**
     * 设置 Socket 选项
     *
     * @param socketChannel 通道
     * @param socketOption  Socket 选项
     * @param value         选项值
     * @param <T>           值类型
     */
    private <T> void setSocketOption(SocketChannel socketChannel, SocketOption<T> socketOption, T value) {
        try {
            socketChannel.setOption(socketOption, value);
        } catch (Throwable t) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Could not configure {} to {} on {}",
                        socketOption, value, socketChannel, t);
            }
        }
    }

    @Override
    public void close() {
        super.close();

        ServerSocketChannel acceptChannel = this.acceptChannel;
        this.acceptChannel = null;

        if (acceptChannel != null) {
            // 移除 bean
            removeBean(acceptChannel);

            // 关闭通道
            if (acceptChannel.isOpen()) {
                try {
                    acceptChannel.close();
                } catch (IOException e) {
                    LOG.warn("Unable to close: {}", acceptChannel, e);
                }
            }
        }

        this.localPort = -2;
    }

    @Override
    protected void accept(int acceptorId) throws IOException, InterruptedException {
        ServerSocketChannel acceptChannel = this.acceptChannel;
        if (acceptChannel != null && acceptChannel.isOpen()) {
            SocketChannel channel = acceptChannel.accept();
            accepted(channel);
        }
    }

    /**
     * 接收连接
     *
     * @param socketChannel 通道
     * @throws IOException IO 异常
     */
    private void accepted(SocketChannel socketChannel) throws IOException {
        // 非阻塞
        socketChannel.configureBlocking(false);

        // 配置 Nagel 算法
        setSocketOption(socketChannel, StandardSocketOptions.TCP_NODELAY, this.acceptedTcpNoDelay);

        // 配置接收缓存
        if (this.acceptedReceiveBufferSize > -1) {
            setSocketOption(socketChannel, StandardSocketOptions.SO_RCVBUF, this.acceptedReceiveBufferSize);
        }

        // 配置发送缓存
        if (this.acceptedSendBufferSize > -1) {
            setSocketOption(socketChannel, StandardSocketOptions.SO_SNDBUF, this.acceptedSendBufferSize);
        }

        // 注册通道
        this.selectorManager.accept(socketChannel);
    }

    /**
     * 获取 Selector 管理器
     *
     * @return Selector 管理器
     */
    public SelectorManager getSelectorManager() {
        return this.selectorManager;
    }

    @Override
    public Object getTransport() {
        return this.acceptChannel;
    }

    @Override
    public int getLocalPort() {
        return this.localPort;
    }

    /**
     * 创建一个新的端点
     *
     * @param socketChannel   {@link SocketChannel}
     * @param managedSelector {@link ManagedSelector}
     * @param selectionKey    {@link SelectionKey}
     * @return {@link SocketChannelEndPoint}
     * @throws IOException IO 异常
     */
    protected SocketChannelEndPoint newEndPoint(SocketChannel socketChannel,
                                                ManagedSelector managedSelector,
                                                SelectionKey selectionKey)
            throws IOException {
        SocketChannelEndPoint endPoint = new SocketChannelEndPoint(
                socketChannel, managedSelector, selectionKey, getScheduler());
        endPoint.setIdleTimeout(getIdleTimeout());

        return endPoint;
    }

    @Override
    public void setAccepting(boolean accepting) {
        super.setAccepting(accepting);

        if (getAcceptors() > 0) {
            return;
        }

        try {
            if (accepting) {
                // 接收连接
                if (this.acceptor.get() == null) {
                    Closeable acceptor = this.selectorManager.acceptor(this.acceptChannel);
                    if (!this.acceptor.compareAndSet(null, acceptor)) {
                        acceptor.close();
                    }
                }
            } else {
                // 关闭连接
                Closeable acceptor = this.acceptor.get();
                if (acceptor != null && this.acceptor.compareAndSet(acceptor, null)) {
                    acceptor.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取接收队列大小
     *
     * @return 接收队列大小
     */
    public int getAcceptQueueSize() {
        return this.acceptQueueSize;
    }

    /**
     * 设置接收队列大小
     *
     * @param acceptQueueSize 接收队列大小
     */
    public void setAcceptQueueSize(int acceptQueueSize) {
        this.acceptQueueSize = acceptQueueSize;
    }

    /**
     * 获取是否可重用地址
     *
     * @return 是否可重用地址
     */
    public boolean isReuseAddress() {
        return this.reuseAddress;
    }

    /**
     * 设置是否可重用地址
     *
     * @param reuseAddress 是否可重用地址
     */
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * 获取是否可重用端口
     *
     * @return 是否可重用端口
     */
    public boolean isReusePort() {
        return this.reusePort;
    }

    /**
     * 设置是否可重用端口
     *
     * @param reusePort 是否可重用端口
     */
    public void setReusePort(boolean reusePort) {
        this.reusePort = reusePort;
    }

    /**
     * 判断是否开启 Nagel 算法
     *
     * @return 是否开启 Nagel 算法
     */
    public boolean getAcceptedTcpNoDelay() {
        return this.acceptedTcpNoDelay;
    }

    /**
     * 设置是否开启 Nagel 算法
     *
     * @param acceptedTcpNoDelay 是否开启 Nagel 算法
     */
    public void setAcceptedTcpNoDelay(boolean acceptedTcpNoDelay) {
        this.acceptedTcpNoDelay = acceptedTcpNoDelay;
    }

    /**
     * 获取接收缓存大小
     *
     * @return 接收缓存大小
     */
    public int getAcceptedReceiveBufferSize() {
        return this.acceptedReceiveBufferSize;
    }

    /**
     * 设置接收缓存大小
     *
     * @param acceptedReceiveBufferSize 接收缓存大小
     */
    public void setAcceptedReceiveBufferSize(int acceptedReceiveBufferSize) {
        this.acceptedReceiveBufferSize = acceptedReceiveBufferSize;
    }

    /**
     * 获取发送缓存大小
     *
     * @return 发送缓存大小
     */
    public int getAcceptedSendBufferSize() {
        return acceptedSendBufferSize;
    }

    /**
     * 设置发送缓存大小
     *
     * @param acceptedSendBufferSize 发送缓存大小
     */
    public void setAcceptedSendBufferSize(int acceptedSendBufferSize) {
        this.acceptedSendBufferSize = acceptedSendBufferSize;
    }

    /**
     * {@link ServerConnector} 的 {@link SelectorManager}
     */
    protected class ServerConnectorManager extends SelectorManager {
        public ServerConnectorManager(Executor executor, Scheduler scheduler, int selectors) {
        }
    }
}
