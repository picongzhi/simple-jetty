package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.Jetty;

/**
 * Http 配置
 *
 * @author picongzhi
 */
public class HttpConfiguration {
    /**
     * 服务器版本
     */
    public static final String SERVER_VERSION = "Jetty(" + Jetty.VERSION + ")";

    public boolean isUseInputDirectByteBuffers() {
        return false;
    }

    public boolean isUseOutputDirectByteBuffers() {
        return false;
    }

    /**
     * 连接工厂
     */
    public interface ConnectionFactory {
        /**
         * 获取 {@link HttpConfiguration}
         *
         * @return {@link HttpConfiguration}
         */
        HttpConfiguration getHttpConfiguration();
    }
}
