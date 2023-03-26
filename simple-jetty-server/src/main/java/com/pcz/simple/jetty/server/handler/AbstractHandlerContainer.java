package com.pcz.simple.jetty.server.handler;

import com.pcz.simple.jetty.server.Handler;
import com.pcz.simple.jetty.server.HandlerContainer;
import com.pcz.simple.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 抽象的处理器容器
 *
 * @author picongzhi
 */
public abstract class AbstractHandlerContainer extends AbstractHandler implements HandlerContainer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHandlerContainer.class);

    public AbstractHandlerContainer() {
    }

    @Override
    public Handler[] getChildHandlers() {
        List<Handler> handlers = new ArrayList<>();
        expandChildren(handlers, null);

        return handlers.toArray(new Handler[0]);
    }

    @Override
    public Handler[] getChildHandlersByClass(Class<?> cls) {
        List<Handler> handlers = new ArrayList<>();
        expandChildren(handlers, cls);

        return handlers.toArray(new Handler[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Handler> T getChildHandlerByClass(Class<T> cls) {
        List<Handler> handlers = new ArrayList<>();
        expandChildren(handlers, cls);

        return handlers.isEmpty()
                ? null
                : (T) handlers.get(0);
    }

    /**
     * 扩展子处理器
     *
     * @param handlers 子处理器
     * @param cls      子处理器类型
     */
    protected void expandChildren(List<Handler> handlers, Class<?> cls) {
    }

    /**
     * 扩展处理器
     * 添加 {@code handler} 并遍历 {@code handler} 的子处理器
     *
     * @param handler  处理器
     * @param handlers 所有处理器
     * @param cls      处理器类型
     */
    protected void expandHandler(Handler handler, List<Handler> handlers, Class<?> cls) {
        if (handler == null) {
            return;
        }

        if (cls == null || cls.isAssignableFrom(handler.getClass())) {
            handlers.add(handler);
        }

        // 遍历子处理器
        if (handler instanceof AbstractHandlerContainer) {
            ((AbstractHandlerContainer) handler).expandChildren(handlers, cls);
        } else if (handler instanceof HandlerContainer) {
            HandlerContainer handlerContainer = (HandlerContainer) handler;
            Handler[] childrenHandler = cls == null
                    ? handlerContainer.getChildHandlers()
                    : handlerContainer.getChildHandlersByClass(cls);
            handlers.addAll(Arrays.asList(childrenHandler));
        }
    }

    @Override
    public void setServer(Server server) {
        if (server == getServer()) {
            return;
        }

        if (isStarted()) {
            throw new IllegalStateException(getState());
        }

        super.setServer(server);

        Handler[] handlers = getHandlers();
        if (handlers != null) {
            for (Handler handler : handlers) {
                handler.setServer(server);
            }
        }
    }
}
