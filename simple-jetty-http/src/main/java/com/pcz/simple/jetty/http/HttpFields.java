package com.pcz.simple.jetty.http;

import java.util.Iterator;

/**
 * HTTP 字段集合
 *
 * @author picongzhi
 */
public interface HttpFields extends Iterable<HttpField> {
    /**
     * 转 {@link Immutable}
     *
     * @return {@link Immutable}
     */
    Immutable asImmutable();

    default long getLongField(HttpHeader header) throws NumberFormatException {
        HttpField field = getField(header);
        return field == null
                ? -1L
                : field.getLongValue();
    }

    /**
     * 获取指定 {@link HttpHeader} 类型的 {@link HttpField}
     *
     * @param header {@link HttpHeader}
     * @return {@link HttpField}
     */
    default HttpField getField(HttpHeader header) {
        for (HttpField field : this) {
            if (field.getHeader() == header) {
                return field;
            }
        }

        return null;
    }

    /**
     * 不可变的 {@link HttpFields}
     */
    class Immutable implements HttpFields {
        @Override
        public Immutable asImmutable() {
            return this;
        }

        @Override
        public Iterator<HttpField> iterator() {
            return null;
        }
    }
}
