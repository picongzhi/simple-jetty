package com.pcz.simple.jetty.core.thread;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Supplier;

/**
 * 有权限控制的线程工厂
 *
 * @author picongzhi
 */
class PrivilegedThreadFactory {
    /**
     * 创建线程
     *
     * @param supplier 线程 {@link Supplier}
     * @param <T>      线程类型
     * @return 线程
     */
    static <T extends Thread> T newThread(Supplier<T> supplier) {
        return AccessController.doPrivileged((PrivilegedAction<T>) supplier::get);
    }
}
