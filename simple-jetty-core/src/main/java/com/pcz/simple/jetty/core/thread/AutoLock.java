package com.pcz.simple.jetty.core.thread;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自动释放的锁
 *
 * @author picongzhi
 */
public class AutoLock implements AutoCloseable, Serializable {
    private static final long serialVersionUID = -3155935961686229999L;

    /**
     * 锁
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 获取锁
     *
     * @return 当前锁
     */
    public AutoLock lock() {
        this.lock.lock();
        return this;
    }

    /**
     * 判断锁是否被当前线程持有
     *
     * @return 锁是否被当前线程持有
     */
    public boolean isHeldByCurrentThread() {
        return this.lock.isHeldByCurrentThread();
    }

    /**
     * 获取当前锁关联的 {@link Condition}
     *
     * @return 当前锁关联的 {@link Condition}
     */
    public Condition newCondition() {
        return this.lock.newCondition();
    }

    /**
     * 判断是否已加锁
     *
     * @return 是否已加锁
     */
    boolean isLocked() {
        return this.lock.isLocked();
    }

    @Override
    public void close() {
        this.lock.unlock();
    }

    public static class WithCondition extends AutoLock {
        /**
         * condition
         */
        private final Condition condition = newCondition();

        @Override
        public AutoLock.WithCondition lock() {
            return (WithCondition) super.lock();
        }

        /**
         * 唤醒一个等待的线程
         */
        public void signal() {
            this.condition.signal();
        }

        /**
         * 唤醒所有等待的线程
         */
        public void signalAll() {
            this.condition.signalAll();
        }

        /**
         * 当前线程等待
         *
         * @throws InterruptedException 中断异常
         */
        public void await() throws InterruptedException {
            this.condition.await();
        }

        /**
         * 当前线程超时等待
         *
         * @param time 等待时间
         * @param unit 时间单位
         * @return 是否在超时前返回
         * @throws InterruptedException 中断异常
         */
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return this.condition.await(time, unit);
        }
    }
}
