package com.pcz.simple.jetty.http;

import java.util.Objects;

/**
 * HTTP 字段
 *
 * @author picongzhi
 */
public class HttpField {
    /**
     * HTTP header
     */
    private final HttpHeader header;

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段值
     */
    private final String value;

    public HttpField(HttpHeader header, String name, String value) {
        this.header = header;

        if (this.header != null || name == null) {
            this.name = this.header.asString();
        } else {
            this.name = Objects.requireNonNull(name, "name");
        }

        this.value = value;
    }

    /**
     * 获取 {@code long} 类型的值
     *
     * @return {@code long} 类型的值
     */
    public long getLongValue() {
        return Long.parseLong(this.value);
    }

    /**
     * 获取 {@link HttpHeader}
     *
     * @return {@link HttpHeader}
     */
    public HttpHeader getHeader() {
        return this.header;
    }
}
