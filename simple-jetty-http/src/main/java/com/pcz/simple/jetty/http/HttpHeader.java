package com.pcz.simple.jetty.http;

/**
 * HTTP header
 *
 * @author picongzhi
 */
public enum HttpHeader {
    /**
     * Content-Length
     */
    CONTENT_LENGTH("Content-Length");

    /**
     * header 字符串
     */
    private final String string;

    HttpHeader(String string) {
        this.string = string;
    }

    /**
     * 获取 header 字符串
     *
     * @return header 字符串
     */
    public String asString() {
        return this.string;
    }
}
