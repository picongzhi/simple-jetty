package com.pcz.simple.jetty.core;

/**
 * Jetty 工具类
 *
 * @author picongzhi
 */
public class Jetty {
    /**
     * 版本
     */
    public static final String VERSION;

    /**
     * 是否稳定版本
     */
    public static final boolean STABLE;

    /**
     * git hash
     */
    public static final String GIT_HASH;

    /**
     * 构建时间戳
     */
    public static final String BUILD_TIMESTAMP;

    static {
        Package pkg = Jetty.class.getPackage();
        if (pkg != null
                && "Eclipse Jetty Project".equals(pkg.getImplementationVendor())
                && pkg.getImplementationVersion() != null) {
            VERSION = pkg.getImplementationVersion();
        } else {
            VERSION = System.getProperty("jetty.version", "1.RC1");
        }

        STABLE = !VERSION.matches("^.*\\.(RC|M)[0-9]+$");

        // TODO: 获取构建配置
        String gitHash = "unknown";
        GIT_HASH = gitHash;

        String buildTimestamp = "unknown";
        BUILD_TIMESTAMP = buildTimestamp;
    }

    private Jetty() {
    }
}
