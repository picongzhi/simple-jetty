package com.pcz.simple.jetty.core.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 可以确认线程是否立即可用的执行器
 *
 * @author picongzhi
 */
public interface TryExecutor extends Executor {
    /**
     * 不尝试的执行器
     */
    TryExecutor NO_TRY = new TryExecutor() {
        @Override
        public boolean tryExecute(Runnable task) {
            return false;
        }

        @Override
        public String toString() {
            return "NO_TRY";
        }
    };

    /**
     * 尝试执行任务
     *
     * @param task 任务
     * @return 执行结果
     */
    boolean tryExecute(Runnable task);

    @Override
    default void execute(Runnable task) {
        if (!tryExecute(task)) {
            throw new RejectedExecutionException();
        }
    }

    /**
     * 将 {@link Executor } 转 {@link TryExecutor}
     *
     * @param executor {@link Executor}
     * @return {@link TryExecutor}
     */
    static TryExecutor asTryExecutor(Executor executor) {
        if (executor instanceof TryExecutor) {
            return (TryExecutor) executor;
        }

        return new NoTryExecutor(executor);
    }

    /**
     * 不尝试执行的执行器
     */
    class NoTryExecutor implements TryExecutor {
        /**
         * 执行器
         */
        private final Executor executor;

        public NoTryExecutor(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(Runnable task) {
            this.executor.execute(task);
        }

        @Override
        public boolean tryExecute(Runnable task) {
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s@%x[%s]",
                    getClass().getSimpleName(), hashCode(), this.executor);
        }
    }
}
