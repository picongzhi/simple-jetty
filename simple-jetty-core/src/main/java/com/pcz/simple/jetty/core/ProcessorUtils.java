package com.pcz.simple.jetty.core;

/**
 * 处理器工厂
 *
 * @author picongzhi
 */
public class ProcessorUtils {
    /**
     * 环境变量名
     */
    public static final String AVAILABLE_PROCESSORS = "JETTY_AVAILABLE_PROCESSORS";

    /**
     * 可用的线程数
     */
    private static int availableProcessors = init();

    /**
     * 初始化
     * 先从环境变量中获取，获取不到降级取 {@link Runtime#getRuntime()#availableProcessors()}
     *
     * @return 可用的线程数
     */
    static int init() {
        String processors = System.getProperty(AVAILABLE_PROCESSORS, System.getenv(AVAILABLE_PROCESSORS));
        if (processors != null) {
            try {
                return Integer.parseInt(processors);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取可用的线程数
     *
     * @return 可用的线程数
     */
    public static int availableProcessors() {
        return availableProcessors;
    }
}
