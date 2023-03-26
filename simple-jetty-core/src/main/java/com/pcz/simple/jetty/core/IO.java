package com.pcz.simple.jetty.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * IO 工具类
 *
 * @author picongzhi
 */
public class IO {
    /**
     * 关闭 IO
     *
     * @param closeable 可关闭的对象
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // TODO: 记录日志
        }
    }
}
