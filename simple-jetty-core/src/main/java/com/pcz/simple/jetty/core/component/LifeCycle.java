package com.pcz.simple.jetty.core.component;

import java.util.EventListener;

/**
 * 生命周期
 *
 * @author picongzhi
 */
public interface LifeCycle {
    /**
     * 开始
     *
     * @throws Exception 开始异常
     */
    void start() throws Exception;

    /**
     * 停止
     *
     * @throws Exception 停止异常
     */
    void stop() throws Exception;

    /**
     * 判断是否在运行中
     *
     * @return 是否在运行中
     */
    boolean isRunning();

    /**
     * 判断是否已经开始
     *
     * @return 是否已经开始
     */
    boolean isStarted();

    /**
     * 判断是否正在开始
     *
     * @return 是否正在开始
     */
    boolean isStarting();

    /**
     * 判断是否正在停止
     *
     * @return 是否正在停止
     */
    boolean isStopping();

    /**
     * 判断是否已停止
     *
     * @return 是否已停止
     */
    boolean isStopped();

    /**
     * 判断是否已失败
     *
     * @return 是否已失败
     */
    boolean isFailed();

    /**
     * 添加事件监听器
     *
     * @param listener 事件监听器
     * @return 是否成功
     */
    boolean addEventListener(EventListener listener);

    /**
     * 移除事件监听器
     *
     * @param listener 事件监听器
     * @return 是否成功
     */
    boolean removeEventListener(EventListener listener);

    /**
     * 开始
     *
     * @param object 目标对象
     */
    static void start(Object object) {
        if (object instanceof LifeCycle) {
            try {
                ((LifeCycle) object).start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 停止
     *
     * @param object 目标对象
     */
    static void stop(Object object) {
        if (object instanceof LifeCycle) {
            try {
                ((LifeCycle) object).stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 生命周期事件监听器
     */
    interface Listener extends EventListener {
        /**
         * 开始中
         *
         * @param event 事件
         */
        default void lifeCycleStarting(LifeCycle event) {
        }

        /**
         * 已开始
         *
         * @param event 事件
         */
        default void lifeCycleStarted(LifeCycle event) {
        }

        /**
         * 异常
         *
         * @param event 事件
         * @param cause 异常原因
         */
        default void lifeCycleFailure(LifeCycle event, Throwable cause) {
        }

        /**
         * 停止中
         *
         * @param event 事件
         */
        default void lifeCycleStopping(LifeCycle event) {
        }

        /**
         * 已停止
         *
         * @param event 事件
         */
        default void lifeCycleStopped(LifeCycle event) {
        }
    }
}
