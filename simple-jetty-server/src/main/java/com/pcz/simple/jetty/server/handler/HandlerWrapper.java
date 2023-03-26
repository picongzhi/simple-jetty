package com.pcz.simple.jetty.server.handler;

import com.pcz.simple.jetty.server.Handler;
import com.pcz.simple.jetty.server.HandlerContainer;
import com.pcz.simple.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 处理器包装类
 *
 * @author picongzhi
 */
public class HandlerWrapper extends AbstractHandlerContainer {
    /**
     * 处理器
     */
    protected Handler handler;

    public HandlerWrapper() {
    }

    /**
     * 获取处理器
     *
     * @return 处理器
     */
    public Handler getHandler() {
        return this.handler;
    }

    /**
     * 设置处理器
     *
     * @param handler 处理器
     */
    public void setHandler(Handler handler) {
        if (isStarted()) {
            throw new IllegalStateException(getState());
        }

        if (handler == this
                || (handler instanceof HandlerContainer
                && Arrays.asList(((HandlerContainer) handler).getChildHandlers()).contains(this))) {
            throw new IllegalStateException("setHandler loop");
        }

        if (handler != null) {
            handler.setServer(getServer());
        }

        Handler oldHandler = this.handler;
        this.handler = handler;

        updateBean(oldHandler, this.handler, true);
    }

    /**
     * 插入 {@link HandlerWrapper}，将当前处理器设置为最后的处理器
     *
     * @param wrapper {@link HandlerWrapper}
     */
    public void insertHandler(HandlerWrapper wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException();
        }

        HandlerWrapper tail = wrapper;
        while (tail.getHandler() instanceof HandlerWrapper) {
            tail = (HandlerWrapper) tail.getHandler();
        }

        if (tail.getHandler() != null) {
            throw new IllegalArgumentException("bad tail of inserted wrapper chain");
        }

        // 将当前处理器设置为最后的处理器
        Handler next = getHandler();
        setHandler(wrapper);
        tail.setHandler(next);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Handler handler = this.handler;
        if (handler != null) {
            handler.handle(target, baseRequest, request, response);
        }
    }

    @Override
    public Handler[] getHandlers() {
        if (this.handler == null) {
            return new Handler[0];
        }

        return new Handler[]{this.handler};
    }

    @Override
    public void destroy() {
        if (!isStopping()) {
            throw new IllegalStateException("!STOPPED");
        }

        Handler handler = getHandler();
        if (handler != null) {
            setHandler(null);
            handler.destroy();
        }

        super.destroy();
    }

    @Override
    protected void expandChildren(List<Handler> handlers, Class<?> cls) {
        expandHandler(this.handler, handlers, cls);
    }
}
