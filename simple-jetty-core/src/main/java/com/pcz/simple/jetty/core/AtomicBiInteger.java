package com.pcz.simple.jetty.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author picongzhi
 */
public class AtomicBiInteger extends AtomicLong {
    public AtomicBiInteger(int hi, int lo) {
    }

    public void set(int hi, int lo) {

    }

    public boolean compareAndSet(long encoded, int hi, int lo) {
        return false;
    }

    public static int getHi(long encoded) {
        return 0;
    }

    public static int getLo(long encoded) {
        return 0;
    }
}
