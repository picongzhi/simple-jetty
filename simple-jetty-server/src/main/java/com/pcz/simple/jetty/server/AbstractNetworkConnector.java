package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.io.ByteBufferPool;
import com.pcz.simple.jetty.core.thread.Scheduler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 抽象的网络连接器
 *
 * @author picongzhi
 */
public abstract class AbstractNetworkConnector extends AbstractConnector implements NetworkConnector {
    /**
     * 主机
     */
    private volatile String host;

    /**
     * 端口
     */
    private volatile int port = 0;

    public AbstractNetworkConnector(Server server,
                                    Executor executor,
                                    Scheduler scheduler,
                                    ByteBufferPool byteBufferPool,
                                    int acceptors,
                                    ConnectionFactory... connectionFactories) {
        super(server, executor, scheduler, byteBufferPool, acceptors, connectionFactories);
    }

    @Override
    protected void doStart() throws Exception {
        open();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        close();
        super.doStop();
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        close();
        return super.shutdown();
    }

    @Override
    protected boolean handleAcceptFailure(Throwable t) {
        if (isOpen()) {
            return super.handleAcceptFailure(t);
        }

        LOG.trace("IGNORED", t);

        return false;
    }

    @Override
    public String getHost() {
        return host;
    }

    /**
     * 设置主机
     *
     * @param host 主机
     */
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    /**
     * 设置端口
     *
     * @param port 端口
     */
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int getLocalPort() {
        return -1;
    }

    @Override
    public String toString() {
        return String.format("%s{%s:%d}",
                super.toString(),
                getHost() == null
                        ? "0.0.0.0"
                        : getHost(),
                getLocalPort() <= 0
                        ? getPort()
                        : getLocalPort());
    }
}
