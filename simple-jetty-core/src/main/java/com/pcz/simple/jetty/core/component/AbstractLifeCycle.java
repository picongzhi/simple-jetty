package com.pcz.simple.jetty.core.component;

import com.pcz.simple.jetty.core.Uptime;
import com.pcz.simple.jetty.core.thread.AutoLock;
import com.pcz.simple.jetty.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 抽象的生命周期
 *
 * @author picongzhi
 */
public abstract class AbstractLifeCycle implements LifeCycle {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLifeCycle.class);

    /**
     * 已经停止
     */
    public static final String STOPPED = State.STOPPED.toString();

    /**
     * 正在开始
     */
    public static final String STARTING = State.STARTING.toString();

    /**
     * 已经开始
     */
    public static final String STARTED = State.STARTED.toString();

    /**
     * 正在停止
     */
    public static final String STOPPING = State.STOPPING.toString();

    /**
     * 已经失败
     */
    public static final String FAILED = State.FAILED.toString();

    /**
     * 锁
     */
    private final AutoLock lock = new AutoLock();

    /**
     * 事件监听器
     */
    private final List<EventListener> eventListeners = new CopyOnWriteArrayList<>();

    /**
     * 生命周期状态
     * 默认是 {@link State#STOPPED}
     */
    private volatile State state = State.STOPPED;

    @Override
    public void start() throws Exception {
        try (AutoLock autoLock = this.lock.lock()) {
            try {
                switch (this.state) {
                    case STARTED:
                        return;
                    case STARTING:
                    case STOPPING:
                        throw new IllegalStateException(getState());
                    default:
                        try {
                            // 将状态置为 STATING
                            setStarting();

                            // 执行开始逻辑
                            doStart();

                            // 将状态置为 STATED
                            setStarted();
                        } catch (StopException e) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Unable to stop", e);
                            }

                            // 将状态置为 STOPPING
                            setStopping();

                            // 执行停止逻辑
                            doStop();

                            // 将状态置为 STOPPED
                            setStopped();
                        }
                }
            } catch (Throwable t) {
                setFailed(t);
                throw t;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        try (AutoLock autoLock = this.lock.lock()) {
            try {
                switch (this.state) {
                    case STOPPED:
                        return;
                    case STARTING:
                    case STOPPING:
                        throw new IllegalStateException(getState());
                    default:
                        // 将状态置为 STOPPING
                        setStopping();

                        // 执行停止逻辑
                        doStop();

                        // 将状态置为 STOPPED
                        setStopped();
                }
            } catch (Throwable t) {
                setFailed(t);
                throw t;
            }
        }
    }

    @Override
    public boolean isRunning() {
        final State state = this.state;
        switch (state) {
            case STARTED:
            case STARTING:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isStarted() {
        return this.state == State.STARTED;
    }

    @Override
    public boolean isStarting() {
        return this.state == State.STARTING;
    }

    @Override
    public boolean isStopping() {
        return this.state == State.STOPPING;
    }

    @Override
    public boolean isStopped() {
        return this.state == State.STARTED;
    }

    @Override
    public boolean isFailed() {
        return this.state == State.FAILED;
    }

    @Override
    public boolean addEventListener(EventListener listener) {
        if (this.eventListeners.contains(listener)) {
            return false;
        }

        this.eventListeners.add(listener);
        return true;
    }

    @Override
    public boolean removeEventListener(EventListener listener) {
        return this.eventListeners.remove(listener);
    }

    /**
     * 获取所有事件监听器
     *
     * @return 所有事件监听器
     */
    public List<EventListener> getEventListeners() {
        return this.eventListeners;
    }

    /**
     * 设置事件监听器
     *
     * @param eventListeners 事件监听器
     */
    public void setEventListeners(Collection<EventListener> eventListeners) {
        // 移除不在请求参数中的事件监听器
        for (EventListener listener : this.eventListeners) {
            if (!eventListeners.contains(listener)) {
                removeEventListener(listener);
            }
        }

        // 添加请求参数中的事件监听器
        for (EventListener listener : eventListeners) {
            if (!this.eventListeners.contains(listener)) {
                addEventListener(listener);
            }
        }
    }

    /**
     * 获取声明周期状态
     *
     * @return 生命周期状态
     */
    public String getState() {
        return this.state.toString();
    }

    /**
     * 执行开始逻辑
     *
     * @throws StopException 抛出 StopException，生命周期置为停止
     * @throws Exception     抛出异常，生命周期置为异常
     */
    protected void doStart() throws Exception {
    }

    /**
     * 执行停止逻辑
     *
     * @throws Exception 抛出异常，生命周期置为异常
     */
    protected void doStop() throws Exception {
    }

    /**
     * 开始中
     *
     * @see State#STARTING
     */
    private void setStarting() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("STARING {}", this);
        }

        this.state = State.STARTING;

        for (EventListener listener : this.eventListeners) {
            if (listener instanceof Listener) {
                ((Listener) listener).lifeCycleStarting(this);
            }
        }
    }

    /**
     * 已开始
     *
     * @see State#STARTED
     */
    private void setStarted() {
        if (this.state == State.STARTING) {
            this.state = State.STARTED;

            if (LOG.isDebugEnabled()) {
                LOG.debug("STARTED @{}ms {}", Uptime.getUptime(), this);
            }

            for (EventListener listener : this.eventListeners) {
                if (listener instanceof Listener) {
                    ((Listener) listener).lifeCycleStarted(this);
                }
            }
        }
    }

    /**
     * 停止中
     *
     * @see State#STOPPING
     */
    private void setStopping() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("STOPPING {}", this);
        }

        this.state = State.STOPPING;

        for (EventListener listener : this.eventListeners) {
            if (listener instanceof Listener) {
                ((Listener) listener).lifeCycleStopping(this);
            }
        }
    }

    /**
     * 已停止
     *
     * @see State#STOPPED
     */
    private void setStopped() {
        this.state = State.STOPPED;

        if (LOG.isDebugEnabled()) {
            LOG.debug("STOPPED {}", this);
        }

        for (EventListener listener : this.eventListeners) {
            if (listener instanceof Listener) {
                ((Listener) listener).lifeCycleStopped(this);
            }
        }
    }

    /**
     * 异常
     *
     * @param t 异常
     * @see State#FAILED
     */
    private void setFailed(Throwable t) {
        this.state = State.FAILED;

        if (LOG.isDebugEnabled()) {
            LOG.warn("FAILED {}: {}", this, t, t);
        }

        for (EventListener listener : this.eventListeners) {
            if (listener instanceof Listener) {
                ((Listener) listener).lifeCycleFailure(this, t);
            }
        }
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (StringUtils.isBlank(name)
                && getClass().getSuperclass() != null) {
            name = getClass().getSuperclass().getSimpleName();
        }

        return String.format("%s@%x{%s}", name, hashCode(), getState());
    }

    /**
     * 获取指定生命周期对象的状态
     *
     * @param lifeCycle 生命周期对象
     * @return 状态
     */
    public static String getState(LifeCycle lifeCycle) {
        if (lifeCycle instanceof AbstractLifeCycle) {
            return ((AbstractLifeCycle) lifeCycle).state.toString();
        }

        if (lifeCycle.isStarting()) {
            return State.STARTING.toString();
        }

        if (lifeCycle.isStarted()) {
            return State.STARTED.toString();
        }

        if (lifeCycle.isStopping()) {
            return State.STOPPING.toString();
        }

        if (lifeCycle.isStopped()) {
            return State.STOPPED.toString();
        }

        return State.FAILED.toString();
    }

    /**
     * 生命周期状态
     */
    enum State {
        /**
         * 已经停止
         */
        STOPPED,
        /**
         * 正在开始
         */
        STARTING,
        /**
         * 已经开始
         */
        STARTED,
        /**
         * 正在停止
         */
        STOPPING,
        /**
         * 已经失败
         */
        FAILED
    }

    /**
     * 停止异常
     */
    public class StopException extends RuntimeException {
    }
}
