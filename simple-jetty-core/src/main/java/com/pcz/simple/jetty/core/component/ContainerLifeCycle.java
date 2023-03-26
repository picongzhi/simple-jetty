package com.pcz.simple.jetty.core.component;

import com.pcz.simple.jetty.core.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 容器生命周期
 *
 * @author picongzhi
 */
public class ContainerLifeCycle extends AbstractLifeCycle implements Container, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerLifeCycle.class);

    /**
     * 组件 Bean
     */
    private final List<Bean> beans = new CopyOnWriteArrayList<>();

    /**
     * 容器监听器
     */
    private final List<Container.Listener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 是否已开始
     */
    private boolean doStarted;

    /**
     * 是否已销毁
     */
    private boolean destroyed;

    @Override
    protected void doStart() throws Exception {
        if (this.destroyed) {
            throw new IllegalStateException("Destroyed container cannot be restarted");
        }

        // 标记已开始
        this.doStarted = true;

        // 开始所有 MANAGED 和 AUTO beans
        try {
            for (Bean bean : this.beans) {
                if (!isStarting()) {
                    break;
                }

                if (bean.bean instanceof LifeCycle) {
                    LifeCycle lifeCycle = (LifeCycle) bean.bean;
                    switch (bean.managed) {
                        case MANAGED:
                            if (lifeCycle.isStopped() || lifeCycle.isFailed()) {
                                // 开始
                                start(lifeCycle);
                            }

                            break;
                        case AUTO:
                            if (lifeCycle.isStopped()) {
                                // 管理 bean
                                manage(bean);

                                // 开始
                                start(lifeCycle);
                            } else {
                                // 移除对 bean 的管理
                                unmanage(bean);
                            }

                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (Throwable t) {
            // 捕获异常，停止所有 MANAGED beans
            List<Bean> reverse = new ArrayList<>(this.beans);
            Collections.reverse(reverse);

            for (Bean bean : reverse) {
                if (bean.bean instanceof LifeCycle
                        && bean.managed == Managed.MANAGED) {
                    LifeCycle lifeCycle = (LifeCycle) bean.bean;
                    if (lifeCycle.isRunning()) {
                        try {
                            stop(lifeCycle);
                        } catch (Throwable t2) {
                            if (t2 != t) {
                                t.addSuppressed(t2);
                            }
                        }
                    }
                }
            }

            throw t;
        }
    }

    @Override
    protected void doStop() throws Exception {
        // 标记已开始为 false
        this.doStarted = false;

        super.doStop();

        List<Bean> reverse = new ArrayList<>(this.beans);
        Collections.reverse(reverse);

        MultiException multiException = new MultiException();
        for (Bean bean : reverse) {
            if (!isStopping()) {
                break;
            }

            if (bean.managed == Managed.MANAGED
                    && bean.bean instanceof LifeCycle) {
                LifeCycle lifeCycle = (LifeCycle) bean.bean;
                try {
                    stop(lifeCycle);
                } catch (Throwable t) {
                    multiException.add(t);
                }
            }
        }

        multiException.ifExceptionThrow();
    }

    /**
     * 开始
     *
     * @param lifeCycle 生命周期对象
     * @throws Exception 异常
     */
    protected void start(LifeCycle lifeCycle) throws Exception {
        lifeCycle.start();
    }

    /**
     * 停止
     *
     * @param lifeCycle 生命周期对象
     * @throws Exception 异常
     */
    protected void stop(LifeCycle lifeCycle) throws Exception {
        lifeCycle.stop();
    }

    @Override
    public void destroy() {
        this.destroyed = true;

        List<Bean> reverse = new ArrayList<>(this.beans);
        Collections.reverse(reverse);

        for (Bean bean : reverse) {
            if (bean.bean instanceof Destroyable
                    && (bean.managed == Managed.MANAGED || bean.managed == Managed.POJO)) {
                Destroyable destroyable = (Destroyable) bean.bean;
                try {
                    destroyable.destroy();
                } catch (Throwable t) {
                    LOG.warn("Unable to destroy", t);
                }
            }
        }

        this.beans.clear();
    }

    @Override
    public boolean isManaged(Object bean) {
        for (Bean b : this.beans) {
            if (b.bean == bean) {
                return b.isManaged();
            }
        }

        return false;
    }

    @Override
    public boolean addBean(Object object) {
        if (object instanceof LifeCycle) {
            LifeCycle lifeCycle = (LifeCycle) object;
            return addBean(object,
                    lifeCycle.isRunning() ? Managed.UNMANAGED : Managed.AUTO);
        }

        return addBean(object, Managed.POJO);
    }

    @Override
    public boolean addBean(Object object, boolean managed) {
        if (object instanceof LifeCycle) {
            return addBean(object, managed ? Managed.MANAGED : Managed.UNMANAGED);
        }

        return addBean(object, managed ? Managed.POJO : Managed.UNMANAGED);
    }

    /**
     * 判断是否包含指定的 bean
     *
     * @param bean bean
     * @return 是否包含
     */
    public boolean contains(Object bean) {
        for (Bean b : this.beans) {
            if (b.bean == bean) {
                return true;
            }
        }

        return false;
    }

    /**
     * 添加 bean
     *
     * @param object  bean
     * @param managed 管理类型
     * @return 是否成功
     */
    private boolean addBean(Object object, Managed managed) {
        if (object == null || contains(object)) {
            return false;
        }

        Bean newBean = new Bean(object);
        // 添加 bean
        this.beans.add(newBean);

        // 通知容器监听器，添加了新的 bean
        for (Container.Listener listener : this.listeners) {
            listener.beanAdded(this, object);
        }

        // 添加监听器
        if (object instanceof EventListener) {
            addEventListener((EventListener) object);
        }

        try {
            switch (managed) {
                case UNMANAGED:
                    unmanage(newBean);
                    break;
                case MANAGED:
                    manage(newBean);
                    if (isStarting() && this.doStarted) {
                        LifeCycle lifeCycle = (LifeCycle) object;
                        if (!lifeCycle.isRunning()) {
                            start(lifeCycle);
                        }
                    }
                    break;
                case AUTO:
                    if (object instanceof LifeCycle) {
                        LifeCycle lifeCycle = (LifeCycle) object;
                        if (isStarting()) {
                            if (lifeCycle.isRunning()) {
                                unmanage(newBean);
                            } else if (this.doStarted) {
                                manage(newBean);
                                start(lifeCycle);
                            } else {
                                newBean.managed = Managed.AUTO;
                            }
                        } else if (isStarted()) {
                            unmanage(newBean);
                        } else {
                            newBean.managed = Managed.AUTO;
                        }
                    }
                    break;
                case POJO:
                    newBean.managed = Managed.POJO;
                    break;
                default:
                    throw new IllegalStateException(managed.toString());
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} added {}", this, newBean);
        }

        return true;
    }

    @Override
    public boolean removeBean(Object object) {
        Bean bean = getBean(object);
        return bean != null && remove(bean);
    }

    /**
     * 根据对象获取 bean
     *
     * @param object 对象
     * @return bean
     */
    private Bean getBean(Object object) {
        for (Bean bean : this.beans) {
            if (bean.bean == object) {
                return bean;
            }
        }

        return null;
    }

    /**
     * 移除 bean
     *
     * @param bean bean
     * @return 是否成功
     */
    private boolean remove(Bean bean) {
        if (!this.beans.remove(bean)) {
            return false;
        }

        final boolean wasManaged = bean.isManaged();
        unmanage(bean);

        // 通知监听器
        for (Container.Listener listener : this.listeners) {
            listener.beanRemoved(this, bean.bean);
        }

        // 移除监听器
        if (bean.bean instanceof EventListener && getEventListeners().contains(bean.bean)) {
            removeEventListener((EventListener) bean.bean);
        }

        // 停止生命周期
        if (wasManaged && bean.bean instanceof LifeCycle) {
            try {
                stop((LifeCycle) bean.bean);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    /**
     * 更新 bean
     *
     * @param oldBean 老的 bean
     * @param newBean 新的 bean
     */
    public void updateBean(Object oldBean, final Object newBean) {
        if (newBean != oldBean) {
            if (oldBean != null) {
                removeBean(oldBean);
            }

            if (newBean != null) {
                addBean(newBean);
            }
        }
    }

    /**
     * 更新 bean
     *
     * @param oldBean 老的 bean
     * @param newBean 新的 bean
     * @param managed 是否管理
     */
    public void updateBean(Object oldBean, final Object newBean, boolean managed) {
        if (newBean != oldBean) {
            if (oldBean != null) {
                removeBean(oldBean);
            }

            if (newBean != null) {
                addBean(newBean, managed);
            }
        }
    }

    /**
     * 批量更新 bean
     *
     * @param oldBeans 老的 bean
     * @param newBeans 新的 bean
     */
    public void updateBeans(Object[] oldBeans, final Object[] newBeans) {
        updateBeans(
                oldBeans == null ? Collections.emptyList() : Arrays.asList(oldBeans),
                newBeans == null ? Collections.emptyList() : Arrays.asList(newBeans));
    }

    /**
     * 批量更新 bean
     *
     * @param oldBeans 老的 bean
     * @param newBeans 新的 bean
     */
    public void updateBeans(final Collection<?> oldBeans, final Collection<Object> newBeans) {
        Objects.requireNonNull(oldBeans);
        Objects.requireNonNull(newBeans);

        for (Object object : oldBeans) {
            if (!newBeans.contains(object)) {
                removeBean(object);
            }
        }

        for (Object object : newBeans) {
            if (!oldBeans.contains(object)) {
                addBean(object);
            }
        }
    }

    @Override
    public <T> T getBean(Class<T> cls) {
        for (Bean bean : this.beans) {
            if (cls.isInstance(bean.bean)) {
                return cls.cast(bean.bean);
            }
        }

        return null;
    }

    @Override
    public Collection<Object> getBeans() {
        return getBeans(Object.class);
    }

    @Override
    public <T> Collection<T> getBeans(Class<T> cls) {
        List<T> list = null;
        for (Bean bean : this.beans) {
            if (cls.isInstance(bean.bean)) {
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(cls.cast(bean.bean));
            }
        }

        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public <T> Collection<T> getContainedBeans(Class<T> cls) {
        Set<T> beans = new HashSet<>();
        getContainedBeans(cls, beans);

        return beans;
    }

    /**
     * 获取指定类型的 bean
     *
     * @param cls   bean 类型
     * @param beans bean 集合
     * @param <T>   bean 类型
     */
    protected <T> void getContainedBeans(Class<T> cls, Collection<T> beans) {
        // 获取当前容器包含的 bean
        beans.addAll(getBeans(cls));

        // 遍历其他容器
        for (Container container : getBeans(Container.class)) {
            Bean bean = getBean(container);
            if (bean != null && bean.isManageable()) {
                if (container instanceof ContainerLifeCycle) {
                    ((ContainerLifeCycle) container).getContainedBeans(cls, beans);
                } else {
                    beans.addAll(container.getContainedBeans(cls));
                }
            }
        }
    }

    @Override
    public void manage(Object bean) {
        for (Bean b : this.beans) {
            if (b.bean == bean) {
                manage(b);
                return;
            }
        }

        throw new IllegalArgumentException("Unknown bean " + bean);
    }

    /**
     * 管理指定的 bean
     *
     * @param bean bean
     */
    private void manage(Bean bean) {
        if (bean.managed == Managed.MANAGED) {
            return;
        }

        // 标记为 MANAGED
        bean.managed = Managed.MANAGED;

        if (!(bean.bean instanceof Container)) {
            return;
        }

        for (Container.Listener listener : this.listeners) {
            if (!(listener instanceof InheritedListener)) {
                continue;
            }

            if (bean.bean instanceof ContainerLifeCycle) {
                Container.addBean(bean.bean, listener, false);
            } else {
                Container.addBean(bean.bean, listener);
            }
        }
    }

    @Override
    public void unmanage(Object bean) {
        for (Bean b : this.beans) {
            if (b.bean == bean) {
                unmanage(b);
                return;
            }
        }

        throw new IllegalArgumentException("Unknown bean " + bean);
    }

    /**
     * 移除对执行 bean 的管理
     *
     * @param bean bean
     */
    protected void unmanage(Bean bean) {
        if (bean.managed == Managed.UNMANAGED) {
            return;
        }

        if (bean.managed == Managed.MANAGED
                && bean.bean instanceof Container) {
            for (Container.Listener listener : this.listeners) {
                if (!(listener instanceof InheritedListener)) {
                    continue;
                }

                Container.removeBean(bean.bean, listener);
            }
        }
    }

    @Override
    public boolean addEventListener(EventListener listener) {
        if (!super.addEventListener(listener)) {
            return false;
        }

        if (!contains(listener)) {
            // 新增 bean
            addBean(listener);
        }

        if (listener instanceof Container.Listener) {
            Container.Listener containerListener = (Container.Listener) listener;
            this.listeners.add(containerListener);

            for (Bean bean : this.beans) {
                // 通知所有 bean，新增了监听器
                containerListener.beanAdded(this, bean.bean);

                if (listener instanceof InheritedListener
                        && bean.isManaged()
                        && bean.bean instanceof Container) {
                    if (bean.bean instanceof ContainerLifeCycle) {
                        Container.addBean(bean.bean, listener, false);
                    } else {
                        Container.addBean(bean.bean, listener);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean removeEventListener(EventListener listener) {
        if (!super.removeEventListener(listener)) {
            return false;
        }

        removeBean(listener);

        if (listener instanceof Container.Listener
                && this.listeners.remove(listener)) {
            Container.Listener containerListener = (Container.Listener) listener;
            for (Bean bean : this.beans) {
                // 通知所有 bean，移除了监听器
                containerListener.beanRemoved(this, bean.bean);

                if (listener instanceof InheritedListener
                        && bean.isManaged()) {
                    Container.removeBean(bean.bean, listener);
                }
            }
        }

        return true;
    }


    /**
     * Bean 管理类型
     */
    enum Managed {
        /**
         * POJO
         */
        POJO,
        /**
         * MANAGED
         */
        MANAGED,
        /**
         * UNMANAGED
         */
        UNMANAGED,
        /**
         * AUTO
         */
        AUTO
    }

    /**
     * 组件 Bean
     */
    private static class Bean {
        /**
         * 内部 bean
         */
        private final Object bean;

        /**
         * 管理类型
         */
        private volatile Managed managed = Managed.POJO;

        private Bean(Object bean) {
            if (bean == null) {
                throw new NullPointerException();
            }

            this.bean = bean;
        }

        /**
         * 判断管理类型是否是 {@link Managed#MANAGED}
         *
         * @return 管理类型是否是 {@link Managed#MANAGED}
         */
        public boolean isManaged() {
            return this.managed == Managed.MANAGED;
        }

        /**
         * 判断是否可管理
         *
         * @return 是否可管理
         */
        public boolean isManageable() {
            switch (this.managed) {
                case MANAGED:
                    return true;
                case AUTO:
                    return this.bean instanceof LifeCycle && ((LifeCycle) this.bean).isStopped();
                default:
                    return false;
            }
        }

        @Override
        public String toString() {
            return String.format("{%s,%s}",
                    this.bean, this.managed);
        }
    }
}
