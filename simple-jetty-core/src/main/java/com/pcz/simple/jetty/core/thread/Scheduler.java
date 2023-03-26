package com.pcz.simple.jetty.core.thread;

import com.pcz.simple.jetty.core.component.LifeCycle;

import java.util.concurrent.TimeUnit;

/**
 * 调度器
 *
 * @author picongzhi
 */
public interface Scheduler extends LifeCycle {
    /**
     * 调度任务
     *
     * @param task  任务
     * @param delay 延迟时间
     * @param unit  延迟时间单位
     * @return 调度任务
     */
    Task schedule(Runnable task, long delay, TimeUnit unit);

    /**
     * 调度任务
     */
    interface Task {
        /**
         * 撤销
         *
         * @return 是否成功
         */
        boolean cancel();
    }
}
