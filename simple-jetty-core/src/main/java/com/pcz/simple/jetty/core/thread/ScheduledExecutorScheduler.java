package com.pcz.simple.jetty.core.thread;

import com.pcz.simple.jetty.core.component.AbstractLifeCycle;

import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link java.util.concurrent.ScheduledThreadPoolExecutor} 的调度器
 *
 * @author picongzhi
 */
public class ScheduledExecutorScheduler extends AbstractLifeCycle implements Scheduler {
    public ScheduledExecutorScheduler(String name, boolean daemon) {
        
    }

    @Override
    public Task schedule(Runnable task, long delay, TimeUnit unit) {
        return null;
    }
}
