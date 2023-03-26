package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.io.Connection;
import com.pcz.simple.jetty.core.io.EndPoint;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 连接工厂
 *
 * @author picongzhi
 */
public interface ConnectionFactory {
    /**
     * 获取主协议
     *
     * @return 主协议
     */
    String getProtocol();

    /**
     * 获取所有协议
     *
     * @return 所有协议
     */
    List<String> getProtocols();

    /**
     * 创建新连接
     *
     * @param connector 连接器
     * @param endPoint  端点
     * @return 连接
     */
    Connection newConnection(Connector connector, EndPoint endPoint);

    /**
     * 可检测的连接工厂
     */
    interface Detecting extends ConnectionFactory {
        /**
         * 检测
         *
         * @param buffer 缓存
         * @return 检测结果
         */
        Detection detect(ByteBuffer buffer);

        /**
         * 检测
         */
        enum Detection {
            /**
             * 可识别
             */
            RECOGNIZED,
            /**
             * 不识别
             */
            NOT_RECOGNIZED,
            /**
             * 需要更多字节
             */
            NEED_MORE_BYTES
        }
    }

    /**
     * 可配置连接器的连接工厂
     */
    interface Configuring extends ConnectionFactory {
        /**
         * 配置连接器
         *
         * @param connector 连接器
         */
        void configure(Connector connector);
    }
}
