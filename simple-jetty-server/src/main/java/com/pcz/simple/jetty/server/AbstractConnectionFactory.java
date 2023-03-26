package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.component.ContainerLifeCycle;

import java.util.List;

/**
 * 抽象的连接工厂
 *
 * @author picongzhi
 */
public abstract class AbstractConnectionFactory extends ContainerLifeCycle implements ConnectionFactory {
    /**
     * 主协议
     */
    private final String protocol;

    /**
     * 所有协议
     */
    private final List<String> protocols;

    /**
     * 输入缓存大小
     */
    private int inputBufferSize = 8192;

    protected AbstractConnectionFactory(String protocol) {
        this.protocol = protocol;
        this.protocols = List.of(protocol);
    }

    protected AbstractConnectionFactory(String... protocols) {
        this.protocol = protocols[0];
        this.protocols = List.of(protocols);
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public List<String> getProtocols() {
        return this.protocols;
    }

    public int getInputBufferSize() {
        return this.inputBufferSize;
    }

    public void setInputBufferSize(int inputBufferSize) {
        this.inputBufferSize = inputBufferSize;
    }

    public static ConnectionFactory[] getFactories(SslConnectionFactory.Server sslConnectionFactory,
                                                   ConnectionFactory... connectionFactories) {
        return null;
    }
}
