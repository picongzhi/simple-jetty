package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.io.Connection;
import com.pcz.simple.jetty.core.io.EndPoint;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * SSL 连接工厂
 *
 * @author picongzhi
 */
public class SslConnectionFactory
        extends AbstractConnectionFactory
        implements ConnectionFactory.Detecting, ConnectionFactory.Configuring {
    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public List<String> getProtocols() {
        return null;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        return null;
    }

    @Override
    public Detection detect(ByteBuffer buffer) {
        return null;
    }

    @Override
    public void configure(Connector connector) {

    }

    public String getNextProtocol() {
        return null;
    }

    public static class Server extends SslConnectionFactory {
        
    }
}
