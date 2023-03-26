package com.pcz.simple.jetty.core.thread;

import com.pcz.simple.jetty.core.AtomicBiInteger;
import com.pcz.simple.jetty.core.BlockingArrayQueue;
import com.pcz.simple.jetty.core.component.ContainerLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于队列的线程池
 *
 * @author picongzhi
 */
public class QueuedThreadPool extends ContainerLifeCycle implements ThreadFactory, ThreadPool.SizedThreadPool {
    private static final Logger LOG = LoggerFactory.getLogger(QueuedThreadPool.class);

    /**
     * 什么都不做
     */
    private static final Runnable NOOP = () -> {
    };

    /**
     * 编码的线程数
     * Hi：总线程数
     * Lo：网络空闲线程数 = 空闲线程数 - 任务队列长度
     */
    private final AtomicBiInteger counts = new AtomicBiInteger(Integer.MIN_VALUE, 0);

    /**
     * 最近的收缩时间戳
     */
    private final AtomicLong lastShrink = new AtomicLong();

    /**
     * 线程
     */
    private final Set<Thread> threads = ConcurrentHashMap.newKeySet();

    /**
     * join 锁
     */
    private final AutoLock.WithCondition joinLock = new AutoLock.WithCondition();

    /**
     * 任务
     */
    private final BlockingQueue<Runnable> jobs;

    /**
     * 线程组
     */
    private final ThreadGroup threadGroup;

    /**
     * 线程工厂
     */
    private final ThreadFactory threadFactory;

    /**
     * 名称
     */
    private String name = "qtp" + hashCode();

    /**
     * 空闲时间
     */
    private int idleTimeout;

    /**
     * 最大线程数
     */
    private int maxThreads;

    /**
     * 最小线程数
     */
    private int minThreads;

    /**
     * 保留的线程数
     */
    private int reservedThreads = -1;

    /**
     * 尝试执行器
     */
    private TryExecutor tryExecutor = TryExecutor.NO_TRY;

    /**
     * 线程优先级
     */
    private int priority = Thread.NORM_PRIORITY;

    /**
     * 是否 daemon 线程
     */
    private boolean daemon = false;

    /**
     * 是否 dump 详情
     */
    private boolean detailedDump = false;

    /**
     * 低线程阈值
     */
    private int lowThreadsThreshold = 1;

    /**
     * 线程池预算
     */
    private ThreadPoolBudget threadPoolBudget;

    /**
     * 停止超时时间
     */
    private long stopTimeout;

    /**
     * 虚拟线程执行器
     */
    private Executor virtualThreadsExecutor;

    public QueuedThreadPool() {
        this(200);
    }

    public QueuedThreadPool(int maxThreads) {
        this(maxThreads, Math.min(8, maxThreads));
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads) {
        this(maxThreads, maxThreads, 60000);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            int idleTimeout) {
        this(maxThreads, minThreads, idleTimeout, null);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            BlockingQueue<Runnable> queue) {
        this(maxThreads, minThreads, 60000, -1, queue, null);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            int idleTimeout,
                            BlockingQueue<Runnable> queue) {
        this(maxThreads, minThreads, idleTimeout, queue, null);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            int idleTimeout,
                            BlockingQueue<Runnable> queue,
                            ThreadGroup threadGroup) {
        this(maxThreads, minThreads, idleTimeout, -1, queue, threadGroup);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            int idleTimeout,
                            int reservedThreads,
                            BlockingQueue<Runnable> queue,
                            ThreadGroup threadGroup) {
        this(maxThreads, minThreads, idleTimeout, reservedThreads, queue, threadGroup, null);
    }

    public QueuedThreadPool(int maxThreads,
                            int minThreads,
                            int idleTimeout,
                            int reservedThreads,
                            BlockingQueue<Runnable> queue,
                            ThreadGroup threadGroup,
                            ThreadFactory threadFactory) {
        if (maxThreads < minThreads) {
            throw new IllegalArgumentException(
                    "Max threads (" + maxThreads + ") less than min threads (" + minThreads + ")");
        }

        setMinThreads(minThreads);
        setMaxThreads(maxThreads);
        setIdleTimeout(idleTimeout);
        setStopTimeout(5000);
        setReservedThreads(reservedThreads);

        if (queue == null) {
            int capcity = Math.max(this.minThreads, 8) * 1024;
            queue = new BlockingArrayQueue<>(capcity, capcity);
        }
        this.jobs = queue;

        this.threadGroup = threadGroup;
        setThreadPoolBudget(new ThreadPoolBudget(this));
        this.threadFactory = threadFactory == null
                ? this
                : threadFactory;
    }

    @Override
    public int getMinThreads() {
        return 0;
    }

    @Override
    public void setMinThreads(int threads) {

    }

    @Override
    public int getMaxThreads() {
        return 0;
    }

    @Override
    public void setMaxThreads(int threads) {

    }

    public int getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getStopTimeout() {
        return this.stopTimeout;
    }

    public void setStopTimeout(long stopTimeout) {
        this.stopTimeout = stopTimeout;
    }

    public int getReservedThreads() {
        return this.reservedThreads;
    }

    public void setReservedThreads(int reservedThreads) {
        this.reservedThreads = reservedThreads;
    }

    @Override
    public ThreadPoolBudget getThreadPoolBudget() {
        return this.threadPoolBudget;
    }

    public void setThreadPoolBudget(ThreadPoolBudget threadPoolBudget) {
        if (threadPoolBudget != null && threadPoolBudget.getSizedThreadPool() != this) {
            throw new IllegalArgumentException();
        }

        updateBean(this.threadPoolBudget, threadPoolBudget);
        this.threadPoolBudget = threadPoolBudget;
    }

    @Override
    protected void doStart() throws Exception {
        if (this.reservedThreads == 0) {
            this.tryExecutor = TryExecutor.NO_TRY;
        } else {
            ReservedThreadExecutor reservedThreadExecutor =
                    new ReservedThreadExecutor(this, this.reservedThreads);
            reservedThreadExecutor.setIdleTimeout(this.idleTimeout, TimeUnit.MILLISECONDS);

            this.tryExecutor = reservedThreadExecutor;
        }

        addBean(this.tryExecutor);

        super.doStart();

        // 初始化线程
        this.counts.set(0, 0);

        // 确认线程数

        ensureThreads();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    /**
     * 确认线程数
     */
    private void ensureThreads() {
        while (true) {
            long counts = this.counts.get();

            int threads = AtomicBiInteger.getHi(counts);
            if (threads == Integer.MIN_VALUE) {
                break;
            }

            int idle = AtomicBiInteger.getLo(counts);
            if (threads < this.minThreads
                    || (idle < 0 && threads < this.maxThreads)) {
                // 尝试开启线程
                if (this.counts.compareAndSet(counts, threads + 1, idle + 1)) {
                    startThread();
                }

                // 否则继续检查状态
                continue;
            }

            break;
        }
    }

    protected void startThread() {
        
    }

    @Override
    public void execute(Runnable command) {

    }

    @Override
    public Thread newThread(Runnable r) {
        return null;
    }
}
