package com.pcz.simple.jetty.server.handler;

import com.pcz.simple.jetty.core.component.ContainerLifeCycle;
import com.pcz.simple.jetty.server.Handler;
import com.pcz.simple.jetty.server.Request;
import com.pcz.simple.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 抽象的处理器
 *
 * @author picongzhi
 */
public abstract class AbstractHandler extends ContainerLifeCycle implements Handler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHandler.class);

    /**
     * 服务器
     */
    private Server server;

    public AbstractHandler() {
    }

    @Override
    public abstract void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;

    @Override
    protected void doStart() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting {}", this);
        }

        if (this.server == null) {
            LOG.warn("No Server set for {}", this);
        }

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping {}", this);
        }

        super.doStop();
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public void setServer(Server server) {
        if (this.server == server) {
            return;
        }

        if (isStarted()) {
            throw new IllegalStateException(getState());
        }

        this.server = server;
    }

    @Override
    public void destroy() {
        if (!isStopped()) {
            throw new IllegalStateException("!STOPPED");
        }

        super.destroy();
    }
}
