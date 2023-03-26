package com.pcz.simple.jetty.core.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 优雅关闭接口
 *
 * @author picongzhi
 */
public interface Graceful {
    /**
     * 关闭
     *
     * @return {@link CompletableFuture}
     */
    CompletableFuture<Void> shutdown();

    /**
     * 判断是否已关闭
     *
     * @return 是否已关闭
     */
    boolean isShutdown();

    /**
     * 优雅地关闭容器
     *
     * @param container 容器对象
     * @return {@link CompletableFuture}
     */
    static CompletableFuture<Void> shutdown(Container container) {
        Logger log = LoggerFactory.getLogger(container.getClass());
        log.info("Shutdown {}", container);

        List<Graceful> gracefuls = new ArrayList<>();
        if (container instanceof Graceful) {
            gracefuls.add((Graceful) container);
        }
        gracefuls.addAll(container.getContainedBeans(Graceful.class));

        if (log.isDebugEnabled()) {
            gracefuls.forEach(graceful -> log.debug("Graceful {}", graceful));
        }

        return CompletableFuture.allOf(gracefuls.stream()
                .map(Graceful::shutdown)
                .toArray(CompletableFuture[]::new));
    }

    /**
     * 关闭 {@link ThrowingRunnable}
     *
     * @param throwingRunnable {@link ThrowingRunnable} 对象
     * @return {@link CompletableFuture}
     */
    static CompletableFuture<Void> shutdown(ThrowingRunnable throwingRunnable) {
        AtomicReference<Thread> stopThreadReference = new AtomicReference<>();

        // 撤销会中断运行 throwingRunnable 的线程
        CompletableFuture<Void> shutdown = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean canceled = super.cancel(mayInterruptIfRunning);
                if (canceled && mayInterruptIfRunning) {
                    Thread thread = stopThreadReference.get();
                    if (thread != null) {
                        // 中断
                        thread.interrupt();
                    }
                }

                return canceled;
            }
        };

        Thread stopThread = new Thread(() -> {
            try {
                throwingRunnable.run();
                shutdown.complete(null);
            } catch (Throwable t) {
                shutdown.completeExceptionally(t);
            }
        });

        // 设置为 daemon 线程
        stopThread.setDaemon(true);
        stopThreadReference.set(stopThread);

        // 开始执行
        stopThread.start();

        return shutdown;
    }

    /**
     * 优化关闭抽象类
     */
    abstract class Shutdown implements Graceful {
        /**
         * 目标组件
         */
        final Object component;

        /**
         * 是否完成的引用
         */
        final AtomicReference<CompletableFuture<Void>> done = new AtomicReference<>();

        protected Shutdown(Object component) {
            this.component = component;
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            if (this.done.get() == null) {
                this.done.compareAndSet(null, new CompletableFuture<>() {
                    @Override
                    public String toString() {
                        return String.format("Shutdown<%s>@%s", Shutdown.this.component, hashCode());
                    }
                });
            }

            CompletableFuture<Void> done = this.done.get();
            check();

            return done;
        }

        @Override
        public boolean isShutdown() {
            return this.done.get() != null;
        }

        /**
         * 检查是否已经完成关闭
         */
        public void check() {
            CompletableFuture<Void> done = this.done.get();
            if (done != null && isShutdownDone()) {
                done.complete(null);
            }
        }

        /**
         * 取消
         */
        public void cancel() {
            CompletableFuture<Void> done = this.done.get();
            if (done != null && !done.isDone()) {
                done.cancel(true);
            }

            this.done.set(null);
        }

        /**
         * 判断是否已经完成关闭
         *
         * @return 是否已经完成关闭
         */
        public abstract boolean isShutdownDone();
    }

    /**
     * 可能抛异常的任务
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        /**
         * 运行
         *
         * @throws Exception 异常
         */
        void run() throws Exception;
    }
}
