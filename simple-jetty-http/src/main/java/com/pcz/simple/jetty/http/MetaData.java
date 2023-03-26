package com.pcz.simple.jetty.http;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * 元数据
 *
 * @author picongzhi
 */
public class MetaData implements Iterable<HttpField> {
    /**
     * HTTP 版本
     */
    private final HttpVersion version;

    /**
     * HTTP 字段
     */
    private final HttpFields fields;

    /**
     * 内容长度
     */
    private final long contentLength;

    /**
     * TODO: 完善注释
     */
    private final Supplier<HttpField> trailerSupplier;

    public MetaData(HttpVersion version, HttpFields fields) {
        this(version, fields, -1);
    }

    public MetaData(HttpVersion version, HttpFields fields, long contentLength) {
        this(version, fields, contentLength, null);
    }

    public MetaData(HttpVersion version, HttpFields fields, long contentLength, Supplier<HttpField> trailerSupplier) {
        this.version = version;
        this.fields = fields == null ? null : fields.asImmutable();
        this.contentLength = contentLength >= 0
                ? contentLength
                : this.fields == null
                ? -1
                : this.fields.getLongField(HttpHeader.CONTENT_LENGTH);
        this.trailerSupplier = trailerSupplier;
    }

    @Override
    public Iterator<HttpField> iterator() {
        return null;
    }
}
