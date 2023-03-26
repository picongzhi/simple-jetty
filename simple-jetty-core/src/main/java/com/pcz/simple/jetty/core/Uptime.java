package com.pcz.simple.jetty.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 正常运行时间工具类
 *
 * @author picongzhi
 */
public class Uptime {
    /**
     * 没有实现的值
     */
    public static final int NOIMPL = -1;

    /**
     * 实例
     */
    private static final Uptime INSTANCE = new Uptime();

    /**
     * 实现
     */
    private Impl impl;

    private Uptime() {
        try {
            this.impl = new DefaultIml();
        } catch (UnsupportedOperationException e) {
            System.err.printf("Defaulting Uptime to NOIMPL due to (%s) %s%n",
                    e.getClass().getName(), e.getMessage());
            this.impl = null;
        }
    }

    public Impl getImpl() {
        return impl;
    }

    public void setImpl(Impl impl) {
        this.impl = impl;
    }

    /**
     * 获取 {@link Uptime} 实例
     *
     * @return {@link Uptime} 实例
     */
    public static Uptime getInstance() {
        return INSTANCE;
    }

    /**
     * 获取正常运行时间
     *
     * @return 正常运行时间
     */
    public static long getUptime() {
        Uptime uptime = getInstance();
        if (uptime == null || uptime.impl == null) {
            return NOIMPL;
        }

        return uptime.impl.getUptime();
    }

    /**
     * 实现接口
     */
    private interface Impl {
        /**
         * 获取正常运行时间
         *
         * @return 正常运行时间
         */
        long getUptime();
    }

    /**
     * 默认实现
     */
    public static class DefaultIml implements Impl {
        /**
         * mxBean
         */
        private Object mxBean;

        /**
         * upTime 方法
         */
        private Method uptimeMethod;

        public DefaultIml() {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            try {
                final Class<?> mgmtFactory = Class.forName(
                        "java.lang.management.ManagementFactory", true, classLoader);
                final Class<?> runtimeClass = Class.forName(
                        "java.lang.management.RuntimeMXBean", true, classLoader);

                final Class<?>[] noParams = new Class<?>[0];
                final Method mxBeanMethod = mgmtFactory.getMethod("getRuntimeMXBean", noParams);
                if (mxBeanMethod == null) {
                    throw new UnsupportedOperationException("method getRuntimeMXBean() not found");
                }

                this.mxBean = mxBeanMethod.invoke(mgmtFactory);
                if (mxBean == null) {
                    throw new UnsupportedOperationException("getRuntimeMXBean() method returned null");
                }

                this.uptimeMethod = runtimeClass.getMethod("getUptime", noParams);
                if (uptimeMethod == null) {
                    throw new UnsupportedOperationException("method getUptime() not found");
                }
            } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new UnsupportedOperationException("Implementation not available in this environment");
            }
        }

        @Override
        public long getUptime() {
            try {
                return (long) this.uptimeMethod.invoke(this.mxBean);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                return NOIMPL;
            }
        }
    }
}
