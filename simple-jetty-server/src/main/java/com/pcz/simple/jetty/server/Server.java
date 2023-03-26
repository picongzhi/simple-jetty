package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.http.HttpGenerator;
import com.pcz.simple.jetty.core.Jetty;
import com.pcz.simple.jetty.core.MultiException;
import com.pcz.simple.jetty.core.Uptime;
import com.pcz.simple.jetty.core.component.AttributeContainerMap;
import com.pcz.simple.jetty.core.component.LifeCycle;
import com.pcz.simple.jetty.core.thread.QueuedThreadPool;
import com.pcz.simple.jetty.core.thread.ShutdownThread;
import com.pcz.simple.jetty.core.thread.ThreadPool;
import com.pcz.simple.jetty.server.handler.ErrorHandler;
import com.pcz.simple.jetty.server.handler.HandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HTTP Servlet 服务器
 *
 * @author picongzhi
 */
public class Server extends HandlerWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    /**
     * 连接器
     */
    private final List<Connector> connectors = new CopyOnWriteArrayList<>();

    /**
     * 线程池
     */
    private final ThreadPool threadPool;

    /**
     * 属性
     */
    private final AttributeContainerMap attributes = new AttributeContainerMap();

    /**
     * 是否在 shutdown 时停止
     */
    private boolean stopAtShutdown;

    /**
     * 错误处理器
     */
    private ErrorHandler errorHandler;

    /**
     * 是否空运行
     */
    private boolean dryRun;

    public Server(int port) {
        this((ThreadPool) null);

        ServerConnector serverConnector = new ServerConnector(this);
        serverConnector.setPort(port);

        setConnectors(new Connector[]{serverConnector});
    }

    public Server(ThreadPool threadPool) {
        // 初始化线程池
        this.threadPool = threadPool != null
                ? threadPool
                : new QueuedThreadPool();

        // 新增 bean
        addBean(this.threadPool);
        addBean(this.attributes);

        // 设置服务器
        setServer(this);
    }

    /**
     * 获取连接器
     *
     * @return 连接器
     */
    public Connector[] getConnectors() {
        List<Connector> connectors = new ArrayList<>(this.connectors);
        return connectors.toArray(new Connector[0]);
    }

    /**
     * 设置连接器
     *
     * @param connectors 连接器
     */
    public void setConnectors(Connector[] connectors) {
        // 连接器关联服务校验
        if (connectors != null) {
            for (Connector connector : connectors) {
                if (connector.getServer() != this) {
                    throw new IllegalArgumentException("Connector " + connector +
                            " cannot be shared among server " + connector.getServer() + " and server " + this);
                }
            }
        }

        // 更新 beans
        Connector[] oldConnectors = getConnectors();
        updateBeans(oldConnectors, connectors);

        // 移除老的连接器
        this.connectors.removeAll(Arrays.asList(oldConnectors));

        // 添加新的连接器
        if (connectors != null) {
            this.connectors.addAll(Arrays.asList(connectors));
        }
    }

    /**
     * 获取线程池
     *
     * @return 线程池
     */
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * 是否在 shutdown 时停止
     *
     * @return 是否在 shutdown 时停止
     */
    public boolean getStopAtShutdown() {
        return this.stopAtShutdown;
    }

    /**
     * 设置错误处理器
     *
     * @param errorHandler 错误处理器
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler instanceof ErrorHandler.ErrorPageMapper) {
            throw new IllegalArgumentException("ErrorPageMapper is applicable only to ContextHandler");
        }

        // 更新 bean
        updateBean(this.errorHandler, errorHandler);

        // 初始化
        this.errorHandler = errorHandler;
        if (errorHandler != null) {
            errorHandler.setServer(this);
        }
    }

    @Override
    protected void doStart() throws Exception {
        try {
            // 创建错误处理器
            if (this.errorHandler == null) {
                this.errorHandler = getBean(ErrorHandler.class);
            }

            if (this.errorHandler == null) {
                setErrorHandler(new ErrorHandler());
            }

            this.errorHandler.setServer(this);

            // 注册 shutdown hook
            if (getStopAtShutdown()) {
                ShutdownThread.register(this);
            }

            // 注册 shutdown monitor
            ShutdownMonitor.register(this);

            // 开启 shutdown monitor 线程，等待接收 stop 命令
            ShutdownMonitor.getInstance().start();

            String gitHash = Jetty.GIT_HASH;
            String timestamp = Jetty.BUILD_TIMESTAMP;

            LOG.info("jetty-{}; built: {}; git: {}; jvm {}", getVersion(), timestamp, gitHash,
                    System.getProperty("java.runtime.version", System.getProperty("java.version")));
            if (!Jetty.STABLE) {
                LOG.warn("THIS IS NOT A STABLE RELEASE! DO NOT USE IN PRODUCTION!");
                LOG.warn("Download a stable release from https://download.eclipse.org/jetty/");
            }

            HttpGenerator.setJettyVersion(HttpConfiguration.SERVER_VERSION);

            MultiException multiException = new MultiException();

            // 打开网络连接器
            if (!this.dryRun) {
                this.connectors.stream()
                        .filter(NetworkConnector.class::isInstance)
                        .map(NetworkConnector.class::cast)
                        .forEach(connector -> {
                            try {
                                connector.open();
                            } catch (Throwable t) {
                                multiException.add(t);
                            }
                        });
                // 如果有异常，抛出
                multiException.ifExceptionThrow();
            }

            super.doStart();

            if (this.dryRun) {
                LOG.info(String.format("Started(dry run) %s @%dms", this, Uptime.getUptime()));
                throw new StopException();
            }

            // 开启连接器
            for (Connector connector : this.connectors) {
                try {
                    connector.start();
                } catch (Throwable t) {
                    multiException.add(t);
                    this.connectors.stream()
                            .filter(LifeCycle::isRunning)
                            .map(Object.class::cast)
                            .forEach(LifeCycle::stop);
                }
            }

            // 如果有异常，抛出
            multiException.ifExceptionThrow();
            LOG.info(String.format("Started %s @%dms", this, Uptime.getUptime()));
        } catch (Throwable t) {
            // 关闭网络连接器
            this.connectors.stream()
                    .filter(NetworkConnector.class::isInstance)
                    .map(NetworkConnector.class::cast)
                    .forEach(networkConnector -> {
                        try {
                            networkConnector.close();
                        } catch (Throwable t2) {
                            if (t != t2) {
                                t.addSuppressed(t2);
                            }
                        }
                    });

            throw t;
        } finally {

        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    /**
     * 获取版本
     *
     * @return 版本
     */
    public static String getVersion() {
        return Jetty.VERSION;
    }
}
