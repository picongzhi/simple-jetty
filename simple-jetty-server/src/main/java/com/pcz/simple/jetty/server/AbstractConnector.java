package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.ProcessorUtils;
import com.pcz.simple.jetty.core.component.Container;
import com.pcz.simple.jetty.core.component.ContainerLifeCycle;
import com.pcz.simple.jetty.core.component.Graceful;
import com.pcz.simple.jetty.core.io.ByteBufferPool;
import com.pcz.simple.jetty.core.io.EndPoint;
import com.pcz.simple.jetty.core.io.LogarithmicArrayByteBufferPool;
import com.pcz.simple.jetty.core.thread.AutoLock;
import com.pcz.simple.jetty.core.thread.ScheduledExecutorScheduler;
import com.pcz.simple.jetty.core.thread.Scheduler;
import com.pcz.simple.jetty.core.thread.ThreadPoolBudget;
import com.pcz.simple.jetty.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.stream.Collectors;

/**
 * 抽象的连接器
 *
 * @author picongzhi
 */
public abstract class AbstractConnector extends ContainerLifeCycle implements Connector {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractConnector.class);

    /**
     * 锁
     */
    private final AutoLock lock = new AutoLock();

    /**
     * setAccepting {@link Condition}
     */
    private final Condition setAccepting = lock.newCondition();

    /**
     * 连接工厂，有序
     */
    private final Map<String, ConnectionFactory> connectionFactories = new LinkedHashMap<>();

    /**
     * 服务器
     */
    private final Server server;

    /**
     * 执行器
     */
    private final Executor executor;

    /**
     * 调度器
     */
    private final Scheduler scheduler;

    /**
     * 缓存池
     */
    private final ByteBufferPool byteBufferPool;

    /**
     * 连接线程
     */
    private final Thread[] acceptors;

    /**
     * 端点
     */
    private final Set<EndPoint> endPoints = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 不可变的端点
     */
    private final Set<EndPoint> immutableEndPoints = Collections.unmodifiableSet(endPoints);

    /**
     * 优雅关闭
     */
    private Graceful.Shutdown shutdown;

    /**
     * {@link HttpChannel} 监听器，默认为 {@link HttpChannel#NOOP_LISTENER}
     */
    private HttpChannel.Listener httpChannelListener = HttpChannel.NOOP_LISTENER;

    /**
     * 连接空闲超时时间
     */
    private long idleTimeout = 30000;

    /**
     * 关闭空闲超时时间
     */
    private long shutdownIdleTimeout = 1000L;

    /**
     * 默认的协议
     */
    private String defaultProtocol;

    /**
     * 默认的连接工厂
     */
    protected ConnectionFactory defaultConnectionFactory;

    /**
     * 连接器名称
     */
    private String name;

    /**
     * 接收器优先级增量
     */
    private int acceptorPriorityDelta = -2;

    /**
     * 是否接收请求
     */
    private boolean accepting = true;

    /**
     * 契约
     */
    private ThreadPoolBudget.Lease lease;

    public AbstractConnector(Server server,
                             Executor executor,
                             Scheduler scheduler,
                             ByteBufferPool byteBufferPool,
                             int acceptors,
                             ConnectionFactory... connectionFactories) {
        // 设置 Server
        this.server = server;

        // 处理 Executor
        this.executor = executor != null
                ? executor
                : this.server.getThreadPool();
        addBean(this.executor);
        if (executor == null) {
            unmanage(this.executor);
        }

        // 处理 Scheduler
        if (scheduler == null) {
            scheduler = this.server.getBean(Scheduler.class);
        }
        this.scheduler = scheduler != null
                ? scheduler
                : new ScheduledExecutorScheduler(
                String.format("Connector-Scheduler-%x", hashCode()), false);
        addBean(this.scheduler);

        // 处理 ByteBufferPool
        synchronized (server) {
            if (byteBufferPool == null) {
                byteBufferPool = server.getBean(ByteBufferPool.class);
                if (byteBufferPool == null) {
                    byteBufferPool = new LogarithmicArrayByteBufferPool();
                    server.addBean(byteBufferPool, true);
                }

                addBean(byteBufferPool, false);
            } else {
                addBean(byteBufferPool, true);
            }
        }

        this.byteBufferPool = byteBufferPool;
        addBean(byteBufferPool.asRetainableByteBufferPool());

        // 注册容器监听器
        addEventListener(new Container.Listener() {
            @Override
            public void beanAdded(Container parent, Object child) {
                if (child instanceof HttpChannel.Listener) {
                    AbstractConnector.this.httpChannelListener =
                            new HttpChannelListeners(getBeans(HttpChannel.Listener.class));
                }
            }

            @Override
            public void beanRemoved(Container parent, Object child) {
                if (child instanceof HttpChannel.Listener) {
                    AbstractConnector.this.httpChannelListener =
                            new HttpChannelListeners(getBeans(HttpChannel.Listener.class));
                }
            }
        });

        // 添加连接工厂
        for (ConnectionFactory connectionFactory : connectionFactories) {
            addConnectionFactory(connectionFactory);
        }

        // 初始化接收器线程
        int cores = ProcessorUtils.availableProcessors();
        if (acceptors < 0) {
            acceptors = Math.max(1, Math.max(4, cores / 8));
        }

        if (acceptors > cores) {
            LOG.warn("Acceptors should be <= availableProcessors: {}", this);
        }

        this.acceptors = new Thread[acceptors];
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ByteBufferPool getByteBufferPool() {
        return this.byteBufferPool;
    }

    @Override
    public ConnectionFactory getConnectionFactory(String nextProtocol) {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.connectionFactories.get(StringUtils.asciiToLowerCase(nextProtocol));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getConnectionFactory(Class<T> factoryType) {
        try (AutoLock autoLock = this.lock.lock()) {
            for (ConnectionFactory connectionFactory : this.connectionFactories.values()) {
                if (factoryType.isAssignableFrom(connectionFactory.getClass())) {
                    return (T) connectionFactory;
                }
            }

            return null;
        }
    }

    /**
     * 添加连接工厂
     *
     * @param connectionFactory 连接工厂
     */
    public void addConnectionFactory(ConnectionFactory connectionFactory) {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        // 获取所有待移除的连接工厂
        Set<ConnectionFactory> toRemoveConnectionFactories = new HashSet<>();
        for (String key : connectionFactory.getProtocols()) {
            key = StringUtils.asciiToLowerCase(key);

            ConnectionFactory oldConnectionFactory = this.connectionFactories.remove(key);
            if (oldConnectionFactory != null) {
                if (oldConnectionFactory.getProtocol().equals(this.defaultProtocol)) {
                    this.defaultProtocol = null;
                }

                toRemoveConnectionFactories.add(oldConnectionFactory);
            }

            this.connectionFactories.put(key, connectionFactory);
        }

        // 保留连接工厂引用
        for (ConnectionFactory factory : this.connectionFactories.values()) {
            toRemoveConnectionFactories.remove(factory);
        }

        // 移除 bean
        for (ConnectionFactory factory : toRemoveConnectionFactories) {
            removeBean(factory);
            if (LOG.isDebugEnabled()) {
                LOG.debug("{} removed {}", this, factory);
            }
        }

        // 添加 bean
        addBean(connectionFactory);
        if (this.defaultProtocol == null) {
            this.defaultProtocol = connectionFactory.getProtocol();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} added {}", this, connectionFactory);
        }
    }

    /**
     * 添加第一个连接工厂
     *
     * @param connectionFactory 连接工厂
     */
    public void addFirstConnectionFactory(ConnectionFactory connectionFactory) {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        // 获取现存的所有连接工厂
        List<ConnectionFactory> existingConnectionFactories =
                new ArrayList<>(this.connectionFactories.values());

        // 移除所有连接工厂
        clearConnectionFactories();

        // 添加连接工厂
        addConnectionFactory(connectionFactory);
        for (ConnectionFactory factory : existingConnectionFactories) {
            addConnectionFactory(factory);
        }
    }

    /**
     * 移除所有连接工厂
     */
    public void clearConnectionFactories() {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        this.connectionFactories.clear();
        this.defaultProtocol = null;
    }

    /**
     * 添加连接工厂（如果不存在）
     *
     * @param connectionFactory 连接工厂
     */
    public void addIfAbsentConnectionFactory(ConnectionFactory connectionFactory) {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        String key = StringUtils.asciiToLowerCase(connectionFactory.getProtocol());
        if (!this.connectionFactories.containsKey(key)) {
            addConnectionFactory(connectionFactory);
        }
    }

    /**
     * 移除连接工厂
     *
     * @param protocol 协议
     * @return 被移除的连接工厂
     */
    public ConnectionFactory removeConnectionFactory(String protocol) {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        // 移除连接工厂
        String key = StringUtils.asciiToLowerCase(protocol);
        ConnectionFactory connectionFactory = this.connectionFactories.remove(key);
        if (this.connectionFactories.isEmpty()) {
            this.defaultProtocol = null;
        }

        // 移除 bean
        removeBean(connectionFactory);

        return connectionFactory;
    }

    /**
     * 批量设置连接工厂
     *
     * @param connectionFactories 连接工厂
     */
    public void setConnectionFactories(Collection<ConnectionFactory> connectionFactories) {
        if (isRunning()) {
            throw new IllegalStateException(getState());
        }

        // 移除现存的连接工厂
        List<ConnectionFactory> existingConnectionFactories =
                new ArrayList<>(this.connectionFactories.values());
        for (ConnectionFactory connectionFactory : existingConnectionFactories) {
            removeConnectionFactory(connectionFactory.getProtocol());
        }

        // 添加连接工厂
        for (ConnectionFactory connectionFactory : connectionFactories) {
            if (connectionFactory != null) {
                addConnectionFactory(connectionFactory);
            }
        }
    }

    @Override
    public ConnectionFactory getDefaultConnectionFactory() {
        if (isStarted()) {
            return this.defaultConnectionFactory;
        }

        return getConnectionFactory(this.defaultProtocol);
    }

    @Override
    public Collection<ConnectionFactory> getConnectionFactories() {
        return this.connectionFactories.values();
    }

    /**
     * 获取接收器优先级增量
     *
     * @return 接收器优先级增量
     */
    public int getAcceptorPriorityDelta() {
        return this.acceptorPriorityDelta;
    }

    /**
     * 设置接收器线程优先级增量
     *
     * @param acceptorPriorityDelta 接收器线程优先级增量
     */
    public void setAcceptorPriorityDelta(int acceptorPriorityDelta) {
        int old = this.acceptorPriorityDelta;
        this.acceptorPriorityDelta = acceptorPriorityDelta;
        if (old != acceptorPriorityDelta && isStarted()) {
            for (Thread acceptor : this.acceptors) {
                acceptor.setPriority(Math.max(Thread.MIN_PRIORITY,
                        Math.min(Thread.MAX_PRIORITY,
                                acceptor.getPriority() - old + acceptorPriorityDelta)));
            }
        }
    }

    @Override
    public List<String> getProtocols() {
        return new ArrayList<>(this.connectionFactories.keySet());
    }

    /**
     * 获取默认的协议
     *
     * @return 默认的协议
     */
    public String getDefaultProtocol() {
        return this.defaultProtocol;
    }

    /**
     * 设置默认的协议
     *
     * @param defaultProtocol 默认的协议
     */
    public void setDefaultProtocol(String defaultProtocol) {
        this.defaultProtocol = StringUtils.asciiToLowerCase(defaultProtocol);
        if (isRunning()) {
            this.defaultConnectionFactory = getConnectionFactory(this.defaultProtocol);
        }
    }

    @Override
    public long getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * 设置连接空闲超时时间
     *
     * @param idleTimeout 连接空闲超时时间
     */
    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;

        if (this.idleTimeout == 0) {
            this.shutdownIdleTimeout = 0;
        } else if (this.idleTimeout < this.shutdownIdleTimeout) {
            this.shutdownIdleTimeout = Math.min(1000L, this.idleTimeout);
        }
    }

    /**
     * 获取关闭空闲超时时间
     *
     * @return 关闭空闲超时时间
     */
    public long getShutdownIdleTimeout() {
        return this.shutdownIdleTimeout;
    }

    /**
     * 设置关闭空闲超时时间
     *
     * @param shutdownIdleTimeout 关闭空闲超时时间
     */
    public void setShutdownIdleTimeout(long shutdownIdleTimeout) {
        this.shutdownIdleTimeout = shutdownIdleTimeout;
    }

    @Override
    public Collection<EndPoint> getConnectedEndPoints() {
        return this.immutableEndPoints;
    }

    /**
     * 端点打开时调用
     *
     * @param endPoint 打开的端点
     */
    protected void onEndPointOpened(EndPoint endPoint) {
        this.endPoints.add(endPoint);
    }

    /**
     * 端点关闭时调用
     *
     * @param endPoint 端点
     */
    protected void onEndPointClosed(EndPoint endPoint) {
        this.endPoints.remove(endPoint);

        // 检查关闭
        Shutdown shutdown = this.shutdown;
        if (shutdown != null) {
            shutdown.check();
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 设置名称
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取 HTTP 通道监听器
     *
     * @return HTTP 通道监听器
     */
    public HttpChannel.Listener getHttpChannelListener() {
        return this.httpChannelListener;
    }

    /**
     * 获取接收器数量
     *
     * @return 接收器数量
     */
    public int getAcceptors() {
        return this.acceptors.length;
    }

    @Override
    protected void doStart() throws Exception {
        // 通过配置连接工厂配置连接器
        getConnectionFactories().stream()
                .filter(ConnectionFactory.Configuring.class::isInstance)
                .map(ConnectionFactory.Configuring.class::cast)
                .forEach(configuring -> configuring.configure(this));

        // 配置优雅关闭
        this.shutdown = new Graceful.Shutdown(this) {
            @Override
            public boolean isShutdownDone() {
                if (!AbstractConnector.this.endPoints.isEmpty()) {
                    return false;
                }

                for (Thread acceptor : AbstractConnector.this.acceptors) {
                    if (acceptor != null) {
                        return false;
                    }
                }

                return true;
            }
        };

        // 校验默认协议
        if (this.defaultProtocol == null) {
            throw new IllegalStateException("No default protocol for " + this);
        }

        // 获取并校验默认连接工厂
        this.defaultConnectionFactory = getConnectionFactory(this.defaultProtocol);
        if (this.defaultConnectionFactory == null) {
            throw new IllegalStateException("No protocol factory for default protocol '" +
                    this.defaultProtocol + "' in " + this);
        }

        // 校验 SSL 连接工厂
        SslConnectionFactory sslConnectionFactory = getConnectionFactory(SslConnectionFactory.class);
        if (sslConnectionFactory != null) {
            String nextProtocol = sslConnectionFactory.getNextProtocol();
            ConnectionFactory connectionFactory = getConnectionFactory(nextProtocol);
            if (connectionFactory == null) {
                throw new IllegalStateException("No protocol factory for SSL next protocol: '" +
                        nextProtocol + "' in " + this);
            }
        }

        // 初始化契约
        this.lease = ThreadPoolBudget.leaseFrom(getExecutor(), this, this.acceptors.length);

        super.doStart();

        for (int i = 0; i < this.acceptors.length; i++) {
            Acceptor acceptor = new Acceptor(i);
            addBean(acceptor);
            getExecutor().execute(acceptor);
        }

        LOG.info("Started {}", this);
    }

    @Override
    protected void doStop() throws Exception {
        if (this.lease != null) {
            this.lease.close();
        }

        // 中断接收器
        interruptAcceptors();

        super.doStop();

        // 移除接收器
        for (Acceptor acceptor : getBeans(Acceptor.class)) {
            removeBean(acceptor);
        }

        this.shutdown = null;

        LOG.info("Stopped {}", this);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        Shutdown shutdown = this.shutdown;
        if (shutdown == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> done = shutdown.shutdown();

        // 中断接收器
        interruptAcceptors();

        // 减少端点的空闲超时时间
        for (EndPoint endPoint : this.endPoints) {
            endPoint.setIdleTimeout(getShutdownIdleTimeout());
        }

        // 等待接收器和连接关闭
        return done;
    }

    @Override
    public boolean isShutdown() {
        Shutdown shutdown = this.shutdown;
        return shutdown == null || shutdown.isShutdown();
    }

    /**
     * 中断接收器
     */
    protected void interruptAcceptors() {
        try (AutoLock autoLock = this.lock.lock()) {
            for (Thread acceptor : this.acceptors) {
                if (acceptor != null) {
                    acceptor.interrupt();
                }
            }
        }
    }

    /**
     * 等待接收器线程结束
     *
     * @throws InterruptedException 中断异常
     */
    public void join() throws InterruptedException {
        join(0);
    }

    /**
     * 超时等待接收器线程结束
     *
     * @param timeout 超时时间，单位：ms
     * @throws InterruptedException 中断异常
     */
    public void join(long timeout) throws InterruptedException {
        try (AutoLock autoLock = this.lock.lock()) {
            for (Thread acceptor : this.acceptors) {
                if (acceptor != null) {
                    acceptor.join(timeout);
                }
            }
        }
    }

    /**
     * 接收
     *
     * @param acceptorId 接收器 id
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    protected abstract void accept(int acceptorId) throws IOException, InterruptedException;

    /**
     * 判断是否接收请求
     *
     * @return 是否接收请求
     */
    public boolean isAccepting() {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.accepting;
        }
    }

    /**
     * 设置是否接收请求
     *
     * @param accepting 是否接收请求
     */
    public void setAccepting(boolean accepting) {
        try (AutoLock autoLock = this.lock.lock()) {
            this.accepting = accepting;
            this.setAccepting.signalAll();
        }
    }

    /**
     * 处理接收异常
     *
     * @param t 异常
     * @return 是否成功
     */
    protected boolean handleAcceptFailure(Throwable t) {
        if (!isRunning()) {
            LOG.trace("IGNORED", t);
            return false;
        }

        if (t instanceof InterruptedException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Accept Interrupted", t);
            }
            return true;
        }

        if (t instanceof ClosedByInterruptException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Accept Closed By Interrupt", t);
            }
            return false;
        }

        LOG.warn("Accept Failure", t);

        try {
            // 避免空转循环
            Thread.sleep(1000);
            return true;
        } catch (Throwable th) {
            LOG.trace("IGNORED", th);
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("%s@%x{%s, %s}",
                this.name == null
                        ? getClass().getSimpleName()
                        : this.name,
                hashCode(),
                getDefaultProtocol(),
                getProtocols().stream()
                        .collect(Collectors.joining(", ", "(", ")")));
    }

    /**
     * 接收器
     */
    private class Acceptor implements Runnable {
        /**
         * id
         */
        private final int id;

        /**
         * 名称
         */
        private String name;

        private Acceptor(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            final Thread thread = Thread.currentThread();
            String name = thread.getName();

            // 设置线程名
            this.name = String.format("%s-acceptor-%s@%x-%s",
                    thread.getName(), this.id, hashCode(), AbstractConnector.this.toString());
            thread.setName(this.name);

            // 设置线程优先级
            int priority = thread.getPriority();
            if (AbstractConnector.this.acceptorPriorityDelta != 0) {
                thread.setPriority(Math.max(Thread.MIN_PRIORITY,
                        Math.min(Thread.MAX_PRIORITY, priority + AbstractConnector.this.acceptorPriorityDelta)));
            }

            try {
                while (isRunning() && !shutdown.isShutdown()) {
                    try (AutoLock autoLock = lock.lock()) {
                        if (!accepting && isRunning()) {
                            setAccepting.await();
                        }
                    } catch (InterruptedException e) {
                        continue;
                    }

                    try {
                        accept(this.id);
                    } catch (Throwable t) {
                        if (!handleAcceptFailure(t)) {
                            break;
                        }
                    }
                }
            } finally {
                // 重置线程名
                thread.setName(name);

                // 重置线程优先级
                if (AbstractConnector.this.acceptorPriorityDelta != 0) {
                    thread.setPriority(priority);
                }

                // 将接收器线程置空
                try (AutoLock autoLock = lock.lock()) {
                    acceptors[id] = null;
                }

                // 检查关闭
                Shutdown shutdown = AbstractConnector.this.shutdown;
                if (shutdown != null) {
                    shutdown.check();
                }
            }
        }

        @Override
        public String toString() {
            String name = this.name;
            if (name == null) {
                return String.format("acceptor-%d@%x", this.id, hashCode());
            }

            return name;
        }
    }
}
