package com.pcz.simple.jetty.core.thread;

import java.io.Closeable;
import java.util.concurrent.Executor;

/**
 * 线程池预算
 *
 * @author picongzhi
 */
public class ThreadPoolBudget {
    public ThreadPoolBudget(ThreadPool.SizedThreadPool threadPool) {

    }

    public static Lease leaseFrom(Executor executor, Object lease, int threads) {
        return null;
    }

    public ThreadPool.SizedThreadPool getSizedThreadPool() {
        return null;
    }

    /**
     * 契约
     */
    public interface Lease extends Closeable {
        /**
         * 获取线程数
         *
         * @return 线程数
         */
        int getThreads();
    }
}
