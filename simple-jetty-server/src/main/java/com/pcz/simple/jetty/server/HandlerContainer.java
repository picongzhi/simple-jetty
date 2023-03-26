package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.component.LifeCycle;

/**
 * 处理器容器
 *
 * @author picongzhi
 */
public interface HandlerContainer extends LifeCycle {
    /**
     * 获取所有处理器
     *
     * @return 所有处理器
     */
    Handler[] getHandlers();

    /**
     * 获取所有子处理器
     *
     * @return 所有子处理器
     */
    Handler[] getChildHandlers();

    /**
     * 获取指定类型的子处理器
     *
     * @param cls 子处理器类型
     * @return 子处理器
     */
    Handler[] getChildHandlersByClass(Class<?> cls);

    /**
     * 获取指定类型的子处理器
     *
     * @param cls 子处理器类型
     * @param <T> 子处理器类型
     * @return 子处理器
     */
    <T extends Handler> T getChildHandlerByClass(Class<T> cls);
}
