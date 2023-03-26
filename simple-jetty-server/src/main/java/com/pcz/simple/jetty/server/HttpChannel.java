package com.pcz.simple.jetty.server;

import java.util.EventListener;

/**
 * HTTP 通道
 *
 * @author picongzhi
 */
public class HttpChannel implements Runnable {
    /**
     * 空的监听器
     */
    public static Listener NOOP_LISTENER = new Listener() {
    };

    @Override
    public void run() {

    }

    /**
     * {@link HttpChannel} 的监听器
     */
    public interface Listener extends EventListener {

    }
}
