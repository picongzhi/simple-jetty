package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.io.Connection;
import com.pcz.simple.jetty.core.io.EndPoint;
import com.pcz.simple.jetty.http.HttpVersion;

/**
 * HTTP 连接工厂
 *
 * @author picongzhi
 */
public class HttpConnectionFactory extends AbstractConnectionFactory implements HttpConfiguration.ConnectionFactory {
    /**
     * HTTP 配置
     */
    private final HttpConfiguration httpConfiguration;

    /**
     * 是否记录 HTTP 编译违规行为
     */
    private boolean recordHttpCompilationViolations;

    /**
     * 输入是否使用直接缓存
     */
    private boolean useInputDirectByteBuffers;

    /**
     * 输出是否使用直接缓存
     */
    private boolean useOutputDirectByteBuffers;

    public HttpConnectionFactory() {
        this(new HttpConfiguration());
    }

    public HttpConnectionFactory(HttpConfiguration httpConfiguration) {
        super(HttpVersion.HTTP_1_1.asString());

        this.httpConfiguration = httpConfiguration;
        addBean(this.httpConfiguration);

        setUseInputDirectByteBuffers(this.httpConfiguration.isUseInputDirectByteBuffers());
        setUseOutputDirectByteBuffers(this.httpConfiguration.isUseOutputDirectByteBuffers());
    }

    @Override
    public HttpConfiguration getHttpConfiguration() {
        return this.httpConfiguration;
    }

    public boolean isRecordHttpCompilationViolations() {
        return this.recordHttpCompilationViolations;
    }

    public void setRecordHttpCompilationViolations(boolean recordHttpCompilationViolations) {
        this.recordHttpCompilationViolations = recordHttpCompilationViolations;
    }

    public boolean isUseInputDirectByteBuffers() {
        return useInputDirectByteBuffers;
    }

    public void setUseInputDirectByteBuffers(boolean useInputDirectByteBuffers) {
        this.useInputDirectByteBuffers = useInputDirectByteBuffers;
    }

    public boolean isUseOutputDirectByteBuffers() {
        return useOutputDirectByteBuffers;
    }

    public void setUseOutputDirectByteBuffers(boolean useOutputDirectByteBuffers) {
        this.useOutputDirectByteBuffers = useOutputDirectByteBuffers;
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        return null;
    }
}
