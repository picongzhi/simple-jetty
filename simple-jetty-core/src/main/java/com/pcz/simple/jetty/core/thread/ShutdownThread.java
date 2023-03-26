package com.pcz.simple.jetty.core.thread;

import com.pcz.simple.jetty.core.component.Destroyable;
import com.pcz.simple.jetty.core.component.LifeCycle;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * shutdown 线程
 *
 * @author picongzhi
 */
public class ShutdownThread extends Thread {
    /**
     * 单例
     */
    private static final ShutdownThread INSTANCE =
            PrivilegedThreadFactory.newThread(ShutdownThread::new);

    /**
     * 锁
     */
    private final AutoLock lock = new AutoLock();

    /**
     * 是否注册了 hook
     */
    private boolean hooked;

    /**
     * 生命周期对象
     */
    private final List<LifeCycle> lifeCycles = new CopyOnWriteArrayList<>();

    private ShutdownThread() {
        super("JettyShutdownThread");
    }

    @Override
    public void run() {
        for (LifeCycle lifeCycle : INSTANCE.lifeCycles) {
            try {
                // 关闭
                if (lifeCycle.isStarted()) {
                    lifeCycle.stop();
                }

                // 销毁
                if (lifeCycle instanceof Destroyable) {
                    ((Destroyable) lifeCycle).destroy();
                }
            } catch (Exception e) {
                // TODO: 打印日志
            }
        }
    }

    /**
     * 注册 hook
     */
    private void hook() {
        try (AutoLock autoLock = this.lock.lock()) {
            if (!this.hooked) {
                Runtime.getRuntime().addShutdownHook(this);
            }
            this.hooked = true;
        } catch (Exception e) {
            // TODO: 打印日志
        }
    }

    /**
     * 移除注册的 hook
     */
    private void unhook() {
        try (AutoLock autoLock = this.lock.lock()) {
            this.hooked = false;
            Runtime.getRuntime().removeShutdownHook(this);
        } catch (Exception e) {
            // TODO: 打印日志
        }
    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static ShutdownThread getInstance() {
        return INSTANCE;
    }

    /**
     * 注册生命周期对象
     *
     * @param lifeCycles 生命周期对象
     */
    public static void register(LifeCycle... lifeCycles) {
        try (AutoLock autoLock = INSTANCE.lock.lock()) {
            INSTANCE.lifeCycles.addAll(Arrays.asList(lifeCycles));
            if (INSTANCE.lifeCycles.size() > 0) {
                INSTANCE.hook();
            }
        }
    }

    /**
     * 注册生命周期对象，并指定索引
     *
     * @param index      索引
     * @param lifeCycles 生命周期对象
     */
    public static void register(int index, LifeCycle... lifeCycles) {
        try (AutoLock autoLock = INSTANCE.lock.lock()) {
            INSTANCE.lifeCycles.addAll(index, Arrays.asList(lifeCycles));
            if (INSTANCE.lifeCycles.size() > 0) {
                INSTANCE.hook();
            }
        }
    }

    /**
     * 注销生命周期对象
     *
     * @param lifeCycle 生命周期对象
     */
    public static void deregister(LifeCycle lifeCycle) {
        try (AutoLock autoLock = INSTANCE.lock.lock()) {
            INSTANCE.lifeCycles.remove(lifeCycle);
            if (INSTANCE.lifeCycles.size() == 0) {
                INSTANCE.unhook();
            }
        }
    }

    /**
     * 判断生命周期对象是否已注册
     *
     * @param lifeCycle 生命周期对象
     * @return 是否已注册
     */
    public static boolean isRegistered(LifeCycle lifeCycle) {
        try (AutoLock autoLock = INSTANCE.lock.lock()) {
            return INSTANCE.lifeCycles.contains(lifeCycle);
        }
    }
}
