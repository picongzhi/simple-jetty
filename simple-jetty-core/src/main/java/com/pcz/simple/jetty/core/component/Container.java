package com.pcz.simple.jetty.core.component;

import java.util.*;

/**
 * 容器
 *
 * @author picongzhi
 */
public interface Container {
    /**
     * 添加 bean
     *
     * @param object bean 对象
     * @return 是否成功
     */
    boolean addBean(Object object);

    /**
     * 添加 bean
     *
     * @param object  bean 对象
     * @param managed 是否管理 bean 的生命周期
     * @return 是否成功
     */
    boolean addBean(Object object, boolean managed);

    /**
     * 获取所有 bean
     *
     * @return 所有 bean
     */
    Collection<Object> getBeans();

    /**
     * 获取指定类型的 bean
     *
     * @param cls bean 类型
     * @param <T> bean 类型
     * @return 指定类型的 bean
     */
    <T> Collection<T> getBeans(Class<T> cls);

    /**
     * 获取 bean
     *
     * @param cls bean Class 实例
     * @param <T> bean 类型
     * @return bean
     */
    <T> T getBean(Class<T> cls);

    /**
     * 移除 bean
     *
     * @param object bean 对象
     * @return 是否成功
     */
    boolean removeBean(Object object);

    /**
     * 添加事件监听器
     *
     * @param listener 事件监听器
     * @return 是否成功
     * @see Container.Listener
     * @see LifeCycle.Listener
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
     * 管理 bean
     *
     * @param bean bean
     */
    void manage(Object bean);

    /**
     * 移除对 bean 的管理
     *
     * @param bean bean
     */
    void unmanage(Object bean);

    /**
     * 判断指定的 bean 是否被容器管理
     *
     * @param bean bean
     * @return 是否被容器管理
     */
    boolean isManaged(Object bean);

    /**
     * 获取容器包含的指定类型的 bean
     *
     * @param cls bean 类型
     * @param <T> bean 类型
     * @return 指定类型的 bean
     */
    <T> Collection<T> getContainedBeans(Class<T> cls);

    /**
     * 获取所有事件监听器
     *
     * @return 所有事件监听器
     */
    default List<EventListener> getEventListeners() {
        return Collections.unmodifiableList(new ArrayList<>(getBeans(EventListener.class)));
    }

    /**
     * 添加 bean
     *
     * @param parent 父容器
     * @param child  bean
     * @return 是否成功
     */
    static boolean addBean(Object parent, Object child) {
        if (parent instanceof Container) {
            return ((Container) parent).addBean(child);
        }

        return false;
    }

    /**
     * 添加 bean
     *
     * @param parent  父容器
     * @param child   bean
     * @param managed 是否管理 bean 的生命周期
     * @return 是否成功
     */
    static boolean addBean(Object parent, Object child, boolean managed) {
        if (parent instanceof Container) {
            return ((Container) parent).addBean(child, managed);
        }

        return false;
    }

    /**
     * 移除 bean
     *
     * @param parent 父容器
     * @param child  bean
     * @return 是否成功
     */
    static boolean removeBean(Object parent, Object child) {
        if (parent instanceof Container) {
            return ((Container) parent).removeBean(child);
        }

        return false;
    }

    /**
     * 容器事件监听器
     */
    interface Listener extends EventListener {
        /**
         * bean 被添加
         *
         * @param parent 父容器
         * @param child  bean 对象
         */
        void beanAdded(Container parent, Object child);

        /**
         * bean 被移除
         *
         * @param parent 父容器
         * @param child  bean 对象
         */
        void beanRemoved(Container parent, Object child);
    }

    /**
     * 有继承关系的监听器
     */
    interface InheritedListener extends Listener {
    }
}
