package com.pcz.simple.jetty.core.thread;

import java.util.concurrent.Executor;

/**
 * 线程池
 *
 * @author picongzhi
 */
public interface ThreadPool extends Executor {
    /**
     * 有大小限制的线程池
     */
    interface SizedThreadPool extends ThreadPool {
        /**
         * 获取最小线程数
         *
         * @return 最小线程数
         */
        int getMinThreads();

        /**
         * 获取最大线程数
         *
         * @return 最大线程数
         */
        int getMaxThreads();

        /**
         * 设置最小线程数
         *
         * @param threads 最小线程数
         */
        void setMinThreads(int threads);

        /**
         * 设置最大线程数
         *
         * @param threads 最大线程数
         */
        void setMaxThreads(int threads);

        /**
         * 获取线程池预算
         *
         * @return 线程数预算
         */
        default ThreadPoolBudget getThreadPoolBudget() {
            return null;
        }
    }
}
