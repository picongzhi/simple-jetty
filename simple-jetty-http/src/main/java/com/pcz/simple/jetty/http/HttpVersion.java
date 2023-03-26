package com.pcz.simple.jetty.http;

import com.pcz.simple.jetty.core.util.StringUtils;

import java.nio.ByteBuffer;

/**
 * HTTP 版本
 *
 * @author picongzhi
 */
public enum HttpVersion {
    /**
     * HTTP/0.9
     */
    HTTP_0_9("HTTP/0.9", 9),
    /**
     * HTTP/1.0
     */
    HTTP_1_0("HTTP/1.0", 10),
    /**
     * HTTP/1.1
     */
    HTTP_1_1("HTTP/1.1", 11),
    /**
     * HTTP/2.0
     */
    HTTP_2("HTTP/2.0", 20),
    /**
     * HTTP/3.0
     */
    HTTP_3("HTTP/3.0", 30);

    /**
     * 版本字符串
     */
    private final String string;

    /**
     * 版本字节数组
     */
    private final byte[] bytes;

    /**
     * 版本缓存
     */
    private final ByteBuffer buffer;

    /**
     * 版本号
     */
    private final int version;

    HttpVersion(String string, int version) {
        this.string = string;
        this.bytes = StringUtils.getBytes(string);
        this.buffer = ByteBuffer.wrap(this.bytes);
        this.version = version;
    }

    /**
     * 转字节数组
     *
     * @return 字节数组
     */
    public byte[] toBytes() {
        return this.bytes;
    }

    /**
     * 转 {@link ByteBuffer}，只读
     *
     * @return {@link ByteBuffer}
     */
    public ByteBuffer toBuffer() {
        return this.buffer.asReadOnlyBuffer();
    }

    /**
     * 获取版本号
     *
     * @return 版本号
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * 判断输入的 {@code str} 是否是当前版本
     *
     * @param str 输入 {@code str}
     * @return 是否是当前版本
     */
    public boolean is(String str) {
        return this.string.equalsIgnoreCase(str);
    }

    /**
     * 转字符串
     *
     * @return 版本字符串
     */
    public String asString() {
        return this.string;
    }

    @Override
    public String toString() {
        return this.string;
    }
}
