package com.pcz.simple.jetty.server;

import com.pcz.simple.jetty.core.IO;
import com.pcz.simple.jetty.core.component.Destroyable;
import com.pcz.simple.jetty.core.component.LifeCycle;
import com.pcz.simple.jetty.core.thread.AutoLock;
import com.pcz.simple.jetty.core.thread.ShutdownThread;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

/**
 * Shutdown monitor
 *
 * @author picongzhi
 */
public class ShutdownMonitor {
    /**
     * 锁
     */
    private final AutoLock.WithCondition lock = new AutoLock.WithCondition();

    /**
     * 生命周期对象
     */
    private final Set<LifeCycle> lifeCycles = new LinkedHashSet<>();

    /**
     * 是否 debug
     */
    private boolean debug;

    /**
     * 主机
     */
    private final String host;

    /**
     * 端口
     */
    private int port;

    /**
     * key
     */
    private String key;

    /**
     * 是否停止 VM
     */
    private boolean exitVm = true;

    /**
     * 是否存活
     */
    private boolean alive;

    private ShutdownMonitor() {
        this.debug = System.getProperty("DEBUG") != null;
        this.host = System.getProperty("STOP.HOST", "127.0.0.1");
        this.port = Integer.getInteger("STOP.PORT", -1);
        this.key = System.getProperty("STOP.KEY", null);
        this.exitVm = Boolean.parseBoolean(System.getProperty("STOP.EXIT", "true"));
    }

    /**
     * 添加生命周期对象
     *
     * @param lifeCycles 生命周期对象
     */
    private void addLifeCycles(LifeCycle... lifeCycles) {
        try (AutoLock autoLock = this.lock.lock()) {
            this.lifeCycles.addAll(Arrays.asList(lifeCycles));
        }
    }

    /**
     * 移除生命周期对象
     *
     * @param lifeCycle 生命周期对象
     */
    private void removeLifeCycle(LifeCycle lifeCycle) {
        try (AutoLock autoLock = this.lock.lock()) {
            this.lifeCycles.remove(lifeCycle);
        }
    }

    /**
     * 判断是否存在指定的生命周期对象
     *
     * @param lifeCycle 生命周期对象
     * @return 是否存在
     */
    private boolean containsLifeCycle(LifeCycle lifeCycle) {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.lifeCycles.contains(lifeCycle);
        }
    }

    /**
     * debug
     *
     * @param format 格式化字符串
     * @param args   参数
     */
    private void debug(String format, Object... args) {
        if (this.debug) {
            System.err.printf("[ShutdownMonitor] " + format + "%n", args);
        }
    }

    /**
     * debug
     *
     * @param t 异常
     */
    private void debug(Throwable t) {
        if (this.debug) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * 获取 key
     *
     * @return key
     */
    public String getKey() {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.key;
        }
    }

    /**
     * 获取 port
     *
     * @return port
     */
    public int getPort() {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.port;
        }
    }

    /**
     * 获取 exitVm
     *
     * @return exitVm
     */
    public boolean isExitVm() {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.exitVm;
        }
    }

    /**
     * 设置 debug
     *
     * @param debug debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 设置 exitVm
     *
     * @param exitVm exitVm
     */
    public void setExitVm(boolean exitVm) {
        try (AutoLock autoLock = this.lock.lock()) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.exitVm = exitVm;
        }
    }

    /**
     * 设置 key
     *
     * @param key key
     */
    public void setKey(String key) {
        try (AutoLock autoLock = this.lock.lock()) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.key = key;
        }
    }

    /**
     * 设置 port
     *
     * @param port port
     */
    public void setPort(int port) {
        try (AutoLock autoLock = this.lock.lock()) {
            if (this.alive) {
                throw new IllegalStateException("ShutdownMonitor already started");
            }
            this.port = port;
        }
    }

    /**
     * 开始
     *
     * @throws Exception 异常
     */
    public void start() throws Exception {
        try (AutoLock autoLock = this.lock.lock()) {
            if (this.alive) {
                this.debug("Already started");
                return;
            }

            ServerSocket serverSocket = listen();
            if (serverSocket != null) {
                this.alive = true;

                Thread thread = new Thread(new ShutdownMonitorRunnable(serverSocket));
                thread.setDaemon(true);
                thread.setName("ShutdownMonitor");
                thread.start();
            }
        }
    }

    /**
     * 等待
     *
     * @throws InterruptedException 中断异常
     */
    public void await() throws InterruptedException {
        try (AutoLock.WithCondition autoLock = this.lock.lock()) {
            while (this.alive) {
                autoLock.await();
            }
        }
    }

    /**
     * 判断是否存活
     *
     * @return 是否存活
     */
    protected boolean isAlive() {
        try (AutoLock autoLock = this.lock.lock()) {
            return this.alive;
        }
    }

    /**
     * 停止
     */
    private void stop() {
        try (AutoLock.WithCondition autoLock = this.lock.lock()) {
            this.alive = false;
            autoLock.signalAll();
        }
    }

    /**
     * 监听
     *
     * @return {@link ServerSocket}
     */
    private ServerSocket listen() {
        int port = this.getPort();
        if (port < 0) {
            this.debug("Not enabled (port < 0): %d", port);
            return null;
        }

        String key = getKey();
        try {
            ServerSocket serverSocket = new ServerSocket();
            try {
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(InetAddress.getByName(this.host), port));
            } catch (Throwable t) {
                IO.close(serverSocket);
                throw t;
            }

            if (port == 0) {
                port = serverSocket.getLocalPort();
                System.out.printf("STOP.PORT=%d%n", port);
                this.setPort(port);
            }

            if (key == null) {
                key = Long.toString(
                        (long) (Long.MAX_VALUE * Math.random() + this.hashCode() + System.currentTimeMillis()),
                        36);
                System.out.printf("STOP.KEY=%s%n", key);
                this.setKey(key);
            }

            return serverSocket;
        } catch (Throwable t) {
            this.debug(t);
            System.err.println("Error binding ShutdownMonitor to port " + port + ": " + t);
            return null;
        } finally {
            this.debug("STOP.PORT=%d", port);
            this.debug("STOP.KEY=%s", key);
            this.debug("STOP.EXIT=%b", this.exitVm);
        }
    }

    @Override
    public String toString() {
        return String.format("%s[port=%d,alive=%b]",
                this.getClass().getName(), this.getPort(), this.isAlive());
    }

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static ShutdownMonitor getInstance() {
        return Holder.instance;
    }

    /**
     * 注册生命周期对象
     *
     * @param lifeCycles 生命周期对象
     */
    public static void register(LifeCycle... lifeCycles) {
        getInstance().addLifeCycles(lifeCycles);
    }

    /**
     * 注销生命周期对象
     *
     * @param lifeCycle 生命周期对象
     */
    public static void deregister(LifeCycle lifeCycle) {
        getInstance().removeLifeCycle(lifeCycle);
    }

    /**
     * 判断指定的生命周期对象是否被注册
     *
     * @param lifeCycle 生命周期对象
     * @return 是否被注册
     */
    public static boolean isRegistered(LifeCycle lifeCycle) {
        return getInstance().containsLifeCycle(lifeCycle);
    }

    /**
     * 重置
     */
    protected static void reset() {
        Holder.instance = new ShutdownMonitor();
    }

    /**
     * ShutdownMonitor 任务
     */
    private class ShutdownMonitorRunnable implements Runnable {
        /**
         * 服务端 Socket
         */
        private final ServerSocket serverSocket;

        public ShutdownMonitorRunnable(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            debug("Started");

            try {
                String key = getKey();

                while (true) {
                    try (Socket socket = serverSocket.accept()) {
                        LineNumberReader reader = new LineNumberReader(new InputStreamReader(socket.getInputStream()));

                        String receivedKey = reader.readLine();
                        if (!key.equals(receivedKey)) {
                            debug("Ignoring command with incorrect key: %s", receivedKey);
                            continue;
                        }

                        String command = reader.readLine();
                        debug("command=%s", command);

                        boolean exitVm = isExitVm();
                        OutputStream outputStream = socket.getOutputStream();

                        if ("stop".equalsIgnoreCase(command)) {
                            debug("Performing stop command");

                            // 停止生命周期对象
                            stopLifeCycles(ShutdownThread::isRegistered, exitVm);

                            // 回复客户端
                            debug("Informing client that we are stopped");
                            informClient(outputStream, "Stopped\r\n");

                            if (!exitVm) {
                                break;
                            }

                            // 关闭虚拟机
                            debug("Killing JVM");
                            System.exit(0);
                        } else if ("forcestop".equalsIgnoreCase(command)) {
                            debug("Performing forcestop command");

                            // 停止生命周期对象
                            stopLifeCycles((lifeCycle) -> true, exitVm);

                            // 回复客户端
                            debug("Informing client that we are stopped");
                            informClient(outputStream, "Stopped\r\n");

                            if (!exitVm) {
                                break;
                            }

                            // 关闭虚拟机
                            debug("Killing JVM");
                            System.exit(0);
                        } else if ("stopexit".equalsIgnoreCase(command)) {
                            debug("Performing stop and exit commands");

                            // 停止生命周期对象
                            stopLifeCycles(ShutdownThread::isRegistered, true);

                            // 回复客户端
                            debug("Informing client that we are stopped");
                            informClient(outputStream, "Stopped\r\n");

                            // 关闭虚拟机
                            debug("Killing JVM");
                            System.exit(0);
                        } else if ("exit".equalsIgnoreCase(command)) {
                            debug("KILLING JVM");
                            System.exit(0);
                        } else if ("status".equalsIgnoreCase(command)) {
                            // 通知客户端状态
                            informClient(outputStream, "OK\r\n");
                        } else if ("pid".equalsIgnoreCase(command)) {
                            // 通知客户端线程id
                            informClient(outputStream, Long.toString(ProcessHandle.current().pid()));
                        }
                    }
                }
            } catch (Throwable t) {
                debug(t);
            } finally {
                IO.close(serverSocket);
                stop();
                debug("Stopped");
            }
        }

        /**
         * 停止生命周期对象
         *
         * @param predicate 是否执行 {@link LifeCycle#stop()}
         * @param destroy   是否执行 {@link Destroyable#destroy()}
         */
        private void stopLifeCycles(Predicate<LifeCycle> predicate, boolean destroy) {
            List<LifeCycle> lifeCycles;
            try (AutoLock autoLock = lock.lock()) {
                lifeCycles = new ArrayList<>(ShutdownMonitor.this.lifeCycles);
            }

            for (LifeCycle lifeCycle : lifeCycles) {
                try {
                    if (lifeCycle.isStarted() && predicate.test(lifeCycle)) {
                        lifeCycle.stop();
                    }

                    if ((lifeCycle instanceof Destroyable) && destroy) {
                        ((Destroyable) lifeCycle).destroy();
                    }
                } catch (Throwable t) {
                    debug(t);
                }
            }
        }

        /**
         * 回复客户端
         *
         * @param outputStream 输出流
         * @param message      通知消息
         * @throws IOException IO 异常
         */
        private void informClient(OutputStream outputStream, String message) throws IOException {
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    /**
     * ShutdownMonitor holder
     */
    private static class Holder {
        /**
         * 实例
         */
        static ShutdownMonitor instance = new ShutdownMonitor();
    }
}
