package com.pcz.simple.jetty.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包装多个异常的异常
 *
 * @author picongzhi
 */
public class MultiException extends Exception {
    /**
     * 默认的异常信息
     */
    private final static String DEFAULT_MESSAGE = "Multiple exceptions";

    /**
     * 嵌套的异常
     */
    private List<Throwable> nested;

    public MultiException() {
        super(DEFAULT_MESSAGE, null, false, false);
        this.nested = new ArrayList<>();
    }

    private MultiException(List<Throwable> nested) {
        super(DEFAULT_MESSAGE);
        this.nested = new ArrayList<>(nested);

        if (nested.size() > 0) {
            initCause(nested.get(0));
        }

        for (Throwable t : nested) {
            if (t != this && t != getCause()) {
                addSuppressed(t);
            }
        }
    }

    /**
     * 添加异常
     *
     * @param t 异常
     */
    public void add(Throwable t) {
        if (t instanceof MultiException) {
            MultiException multiException = (MultiException) t;
            nested.addAll(multiException.nested);
        } else {
            nested.add(t);
        }
    }

    /**
     * 获取嵌套的异常数
     *
     * @return 嵌套的异常数
     */
    public int size() {
        return nested == null ? 0 : nested.size();
    }

    /**
     * 获取嵌套的异常
     *
     * @return 嵌套的异常
     */
    public List<Throwable> getThrowables() {
        if (nested == null) {
            return Collections.emptyList();
        }

        return nested;
    }

    /**
     * 根据索引获取嵌套的异常
     *
     * @param i 索引
     * @return 嵌套的异常
     */
    public Throwable getThrowable(int i) {
        return nested.get(i);
    }

    /**
     * 如果有异常，则抛出
     *
     * @throws Exception 异常
     */
    public void ifExceptionThrow() throws Exception {
        if (nested == null) {
            return;
        }

        switch (nested.size()) {
            case 0:
                break;
            case 1:
                Throwable t = nested.get(0);
                if (t instanceof Error) {
                    throw (Error) t;
                }

                if (t instanceof Exception) {
                    throw (Exception) t;
                }

                throw new MultiException(nested);
            default:
                throw new MultiException(nested);
        }
    }

    /**
     * 如果有异常，抛出运行时异常
     *
     * @throws Error 错误
     */
    public void ifExceptionThrowRuntime() throws Error {
        if (nested == null) {
            return;
        }

        switch (nested.size()) {
            case 0:
                break;
            case 1:
                Throwable t = nested.get(0);
                if (t instanceof Error) {
                    throw (Error) t;
                }

                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }

                throw new RuntimeException(t);
            default:
                throw new RuntimeException(new MultiException(nested));
        }
    }

    /**
     * 如果有异常，抛出 {@link MultiException}
     *
     * @throws MultiException {@link MultiException}
     */
    public void ifExceptionThrowMulti() throws MultiException {
        if (nested == null) {
            return;
        }

        if (nested.size() > 0) {
            throw new MultiException(nested);
        }
    }

    /**
     * 如果有异常，抛出并带有可能的 suppress
     *
     * @throws Exception 异常
     */
    public void ifExceptionThrowSuppressed() throws Exception {
        if (nested == null || nested.size() == 0) {
            return;
        }

        Throwable e = nested.get(0);
        if (!Error.class.isInstance(e)
                && !Exception.class.isInstance(e)) {
            e = new MultiException(Collections.emptyList());
        }

        for (Throwable t : nested) {
            if (t != e) {
                e.addSuppressed(t);
            }
        }

        if (Error.class.isInstance(e)) {
            throw (Error) e;
        }

        throw (Exception) e;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(MultiException.class.getSimpleName());
        if (nested == null || nested.size() <= 0) {
            stringBuilder.append("[]");
        } else {
            stringBuilder.append(nested);
        }

        return stringBuilder.toString();
    }
}
