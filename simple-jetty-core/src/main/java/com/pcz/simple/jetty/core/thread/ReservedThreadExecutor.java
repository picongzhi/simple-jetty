package com.pcz.simple.jetty.core.thread;

import com.pcz.simple.jetty.core.component.AbstractLifeCycle;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 保留的线程执行器
 *
 * @author picongzhi
 */
public class ReservedThreadExecutor extends AbstractLifeCycle implements TryExecutor {
    public ReservedThreadExecutor(Executor executor, int capacity) {

    }

    @Override
    public boolean tryExecute(Runnable task) {
        return false;
    }

    public void setIdleTimeout(long timeout, TimeUnit idleTimeout) {
        
    }
}
